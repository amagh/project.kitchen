package project.hnoct.kitchen.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/20/2017.
 */

public class FoodDotComListAsyncTask extends AsyncTask<Void, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = FoodDotComListAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;
    private long mTimeInMillis;

    public FoodDotComListAsyncTask(Context context, long timeInMillis) {
        mContext = context;
        mTimeInMillis = timeInMillis;
    }

//    public FoodDotComListAsyncTask(Context context, long timeInMillis, String searchTerm) {
//        mContext = context;
//        mTimeInMillis = timeInMillis;
//
//        if (!searchTerm.trim().isEmpty()) {
//            String FOOD_BASE_SEARCH_URL = "http://www.food.com";
//            String FOOD_SEARCH_PATH = "search";
//            searchTerm = searchTerm.trim().replace(" ", "+");
//
//            Uri searchUri = Uri.parse(FOOD_BASE_SEARCH_URL)
//                    .buildUpon()
//                    .appendPath(FOOD_SEARCH_PATH)
//                    .appendPath(searchTerm)
//                    .build();
//
//            mSearchUrl = searchUri.toString();
//            Log.d(LOG_TAG, mSearchUrl);
//        }
//    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Instantiate variable to hold time recipes were added. Will subtract one to the time to
        // each subsequent recipe added as to preserve order
        long timeAdded = Utilities.getCurrentTime();

        // Initialize the List to hold all the ContentValues that need to be inserted into database
        List<ContentValues> recipeCVList = new ArrayList<>();
        try {
            for (int j = 1; j < 6; j++) {
                // URL for recipes
                String FOOD_BASE_URL = "http://www.food.com/recipe";

                if (j > 1) {
                    FOOD_BASE_URL = FOOD_BASE_URL + "?pn=" + j;
                }

                // Retrieve the html document from the website
                Document document = document = Jsoup.connect(FOOD_BASE_URL).get();

                String html = document.toString();

                // Split the html into separate lines in order to retrieve the JSON object located
                // within
                String[] lines = html.split("\n");

                // Find the JSON object in the String and convert it to a JSON Object
                JSONObject json = null;
                for (String line : lines) {
                    if (line.contains("var searchResults")) {
                        line = line.substring(20, line.length() - 1);
                        json = new JSONObject(line);
                    }
                }

                // Check to ensure a JSON Object was found
                if (json == null) {
                    // Do nothing if no JSON Object was found
                    return null;
                }

                // Constants for retrieving values from the JSON Object
                String FOOD_RECIPE_RESPONSE_ARRAY = "response";
                String FOOD_RECIPE_ARRAY = "results";
                String FOOD_RECORD_TYPE = "record_type";
                String FOOD_RECIPE_NAME = "main_title";
                String FOOD_RECIPE_DESC = "main_description";
                String FOOD_RECIPE_ID = "recipe_id";
                String FOOD_RECIPE_AUTHOR = "main_username";
                String FOOD_RECIPE_IMG_URL = "recipe_photo_url";
                String FOOD_RECIPE_URL = "record_url";
                String FOOD_RECIPE_RATING = "main_rating";
                String FOOD_RECIPE_REVIEWS = "main_num_ratings";

                // Retrieve the JSON Array holding all the recipe values
                JSONObject jsonResponseArray = json.getJSONObject(FOOD_RECIPE_RESPONSE_ARRAY);
                JSONArray jsonRecipeArray = jsonResponseArray.getJSONArray(FOOD_RECIPE_ARRAY);

                Random random = new Random();

                // Iterate through the objects backwards so that the first object appears at the top of
                // the list
                for (int i = 0; i < jsonRecipeArray.length(); i++) {
                    // Retrieve each recipe JSON Object
                    JSONObject jsonRecipe = jsonRecipeArray.getJSONObject(i);

                    String recordType = jsonRecipe.getString(FOOD_RECORD_TYPE);
                    if (!recordType.contains("ecipe")) {
                        continue;
                    }

                    // Retrieve the recipe information
                    String recipeName = jsonRecipe.getString(FOOD_RECIPE_NAME);
                    String recipeDescription = jsonRecipe.getString(FOOD_RECIPE_DESC);
                    long recipeId = jsonRecipe.getLong(FOOD_RECIPE_ID);
                    String recipeAuthor = jsonRecipe.getString(FOOD_RECIPE_AUTHOR);
                    String recipeImgUrl = jsonRecipe.getString(FOOD_RECIPE_IMG_URL)
                            .replaceAll("\u0026", "&")
                            .replaceAll("\u003d", "=");

                    // Check which domain is hosting the recipe's image and modify the URL so that a
                    // smaller, resized image is returned
                    if (recipeImgUrl.contains("http://img.sndimg.com")) {
                        recipeImgUrl = recipeImgUrl.replaceAll("upload/*.*/v1", "upload/h_420,w_560,c_fit/v1");
                    } else if (recipeImgUrl.contains("http://pictures.food.com")){
                        recipeImgUrl = recipeImgUrl.replaceAll("jpg&.*|JPG&.*", "jpg");
                        recipeImgUrl = recipeImgUrl + "&width=560&height=420&fit=crop&flags=progressive&quality=95";
                    }

                    String recipeUrl = jsonRecipe.getString(FOOD_RECIPE_URL);
                    double recipeRating = jsonRecipe.getDouble(FOOD_RECIPE_RATING);
                    long recipeReviews = jsonRecipe.getLong(FOOD_RECIPE_REVIEWS);

                    // Create a ContentValues to insert into the database with the information
                    ContentValues values = new ContentValues();
                    values.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                    values.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                    values.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeId);
                    values.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                    values.put(RecipeEntry.COLUMN_IMG_URL, recipeImgUrl);
                    values.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                    values.put(RecipeEntry.COLUMN_RATING, recipeRating);
                    values.put(RecipeEntry.COLUMN_REVIEWS, recipeReviews);
                    values.put(RecipeEntry.COLUMN_FAVORITE, 0);
                    values.put(RecipeEntry.COLUMN_DATE_ADDED, mTimeInMillis);
                    values.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_food));

                    // Add the ContentValues to the List of ContentValues to be inserted/updated
                    recipeCVList.add(values);

                    // Increase the time-added so the next object is located below
                    int randomNum = random.nextInt((50-10) + 1) + 10;
                    mTimeInMillis -= randomNum;
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (recipeCVList.size() > 0) {
                // Insert/update entries into the database
                Utilities.insertAndUpdateRecipeValues(mContext, recipeCVList);
            }
        }
        return null;
    }
}
