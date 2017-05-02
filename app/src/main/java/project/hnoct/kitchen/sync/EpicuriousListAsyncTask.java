package project.hnoct.kitchen.sync;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.BooleanAttribute;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.AccessControlContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/1/2017.
 */

public class EpicuriousListAsyncTask extends AsyncTask<Void, Void, Void> {
    // Constants

    // Member Variables
    Context mContext;
    long mTimeInMillis;

    public EpicuriousListAsyncTask(Context context, long timeInMillis) {
        mContext = context;
        mTimeInMillis = timeInMillis;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        // Constants
        String EPICURIOUS_BASE_URL = "http://www.epicurious.com";
        String EPICURIOUS_SEARCH_PATH = "search";
        String EPICURIOUS_BLANK_PATH = "";
        String EPICURIOUS_CONTENT_QUERY_PARAM = "content";
        String EPICURIOUS_CONTENT_VALUE = "recipe";

        // Variables
        List<ContentValues> recipeCVList = new ArrayList<>();
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new ArrayList<>();

        // Build the URI for the base epicurious recipes site
        Uri epicuriousUri = Uri.parse(EPICURIOUS_BASE_URL)
                .buildUpon()
                .appendPath(EPICURIOUS_SEARCH_PATH)
                .appendPath(EPICURIOUS_BLANK_PATH)
                .appendQueryParameter(EPICURIOUS_CONTENT_QUERY_PARAM, EPICURIOUS_CONTENT_VALUE)
                .build();

        // Convert the URI to String so the Document can be downloaded
        String epicuriousUrl = epicuriousUri.toString();

        try {
            // Retrive the HTML document for the epicurious recipes website
            Document document = Jsoup.connect(epicuriousUrl).get();

            // Split the HTML document by line
            String lines[] = document.toString().split("\n");

            // Parse the lines and retrieve the stored JSON object
            JSONObject json = null;
            for (String line : lines) {
                if (line.contains("window._state")) {
                    // Decode the URL encoding to String
                    line = java.net.URLDecoder.decode(line, "UTF-8");
                    line = line.replace("window._state = \"", "");
                    line = line.substring(0, line.length() - 2);
                    line = line.trim();

                    // Convert the String to a JSON stripped of URL encoding
                    json = new JSONObject(line);
                }
            }

            if (json == null) {
                // If no JSONObject was returned, do nothing
                return null;
            }

            // Constants for navigating JSONObject
            String EPI_CONTEXT_OBJ = "context";
            String EPI_DISPATCHER_OBJ = "dispatcher";
            String EPI_STORES_OBJ = "stores";
            String EPI_SEARCHSTORE_OBJ = "SearchStore";
            String EPI_RESULTS_ARRAY = "resultGroups";
            String EPI_ITEMS_ARRAY = "items";

            String EPI_RECIPE_ID = "id";
            String EPI_RECIPE_DESCRIPTION = "dek";
            String EPI_RECIPE_NAME = "hed";
            String EPI_RECIPE_AUTHOR_ARRAY = "author";
            String EPI_RECIPE_AUTHOR_NAME = "name";
            String EPI_RECIPE_URL = "url";
            String EPI_RECIPE_PHOTO_OBJ = "photoData";
            String EPI_RECIPE_IMG_FILENAME = "filename";
            String EPI_RECIPE_RATING = "aggregateRating";
            String EPI_RECIPE_REVIEWS = "reviewsCount";
            String EPI_RECIPE_INGREDIENT_ARRAY = "ingredients";
            String EPI_RECIPE_DIRECTIONS_ARRAY = "prepSteps";

            // Retrieve the Array holding all the recipe information
            JSONObject jsonContext = json.getJSONObject(EPI_CONTEXT_OBJ);
            JSONObject jsonDispatcher = jsonContext.getJSONObject(EPI_DISPATCHER_OBJ);
            JSONObject jsonStores = jsonDispatcher.getJSONObject(EPI_STORES_OBJ);
            JSONObject jsonSearch = jsonStores.getJSONObject(EPI_SEARCHSTORE_OBJ);
            JSONArray jsonResultsArray = jsonSearch.getJSONArray(EPI_RESULTS_ARRAY);
            JSONObject jsonNull = (jsonResultsArray.getJSONObject(1));
            JSONArray jsonItemArray = jsonNull.getJSONArray(EPI_ITEMS_ARRAY);

            // Initialize the IngredientHelper for keeping track of duplicate ingredients
            Utilities.IngredientHelper helper = new Utilities.IngredientHelper(mContext);

            // Estimate the recipeID of the to-be-inserted recipe
            long recipeId = Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);

            // Iterate through the recipes and store their information in ContentValues
            for (int i = 0; i < jsonItemArray.length(); i++) {
                // Initialize the Random Object that will be used to increment the time inserted
                // value
                Random random = new Random();

                // Retrieve the recipe from the Array
                JSONObject jsonRecipeObj = jsonItemArray.getJSONObject(i);

                // Retrieve the recipe information
                String recipeName = jsonRecipeObj.getString(EPI_RECIPE_NAME);
                String description = jsonRecipeObj.getString(EPI_RECIPE_DESCRIPTION);

                // Some recipes do not have listed authors, so a blank String is initialized to
                // allow insertion into the database
                String author = "";

                // Check to see if an author exists. If so use their information
                if (jsonRecipeObj.getJSONArray(EPI_RECIPE_AUTHOR_ARRAY).length() > 0) {
                    author = jsonRecipeObj.getJSONArray(EPI_RECIPE_AUTHOR_ARRAY)
                            .getJSONObject(0)
                            .getString(EPI_RECIPE_AUTHOR_NAME);
                }

                // The ahref value holds a relative link, not an absolute, so the base URL needs to
                // be prepended
                String recipeUrl = EPICURIOUS_BASE_URL + jsonRecipeObj.getString(EPI_RECIPE_URL);

                // Utilize the photoID as the recipeID, because the actual recipeID is not used
                // anywhere in the site and the photoID at least points to the image
                String recipeSourceId = jsonRecipeObj.getJSONObject(EPI_RECIPE_PHOTO_OBJ)
                        .getString(EPI_RECIPE_ID);

                String imageFilename = jsonRecipeObj.getJSONObject(EPI_RECIPE_PHOTO_OBJ)
                        .getString(EPI_RECIPE_IMG_FILENAME);

                // Check if the recipe already exists in the database
                if (Utilities.getRecipeIdFromSourceId(mContext,
                        recipeSourceId,
                        mContext.getString(R.string.attribution_epicurious)) > 0) {
                    // Skip if already in database
                    continue;
                }

                // Generate the link to a higher-quality image to be loaded
                String imageUrl = generateImgUrl(recipeSourceId, imageFilename);

                // Site's rating is out of 4, so it needs to be converted to out-of-5 score
                Double rating = 5 * jsonRecipeObj.getDouble(EPI_RECIPE_RATING) / 4;
                int reviews = jsonRecipeObj.getInt(EPI_RECIPE_REVIEWS);

                // Retrieve the directions and append each with a new line
                StringBuilder builder = new StringBuilder();

                JSONArray directionsArray = jsonRecipeObj.getJSONArray(EPI_RECIPE_DIRECTIONS_ARRAY);

                for (int j = 0; j < directionsArray.length(); j++) {
                    builder.append(directionsArray.getString(j));
                    builder.append("\n");
                }

                String directions = builder.toString().trim();

                // Generate the ContentValues for the recipe
                ContentValues recipeValues = new ContentValues();
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_epicurious));
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, description);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, imageUrl);
                recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, directions);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, mTimeInMillis);
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);

                // Increment the time-added using the Random Object from above
                int randomNum = random.nextInt((50-10) + 1) + 10;
                mTimeInMillis -= randomNum;

                // Recipes will be bulk added, so need to be added to a List first
                recipeCVList.add(recipeValues);

                // Retrieve the JSONArray holding the ingredient information
                JSONArray ingredientsArray = jsonRecipeObj.getJSONArray(EPI_RECIPE_INGREDIENT_ARRAY);

                for (int j = 0; j < ingredientsArray.length(); j++) {
                    // Retrieve the text with ingredient and quantity
                    String ingredientQuantity = ingredientsArray.getString(j);

                    // Split it into separated quantity and ingredient
                    Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);

                    String ingredient = ingredientQuantityPair.first;
                    String quantity = ingredientQuantityPair.second;

                    // Add ingredient to the IngredientHelper and retrieve the results
                    Pair<Boolean, Long> pair = helper.addIngredient(ingredient);
                    boolean skipIngredient = pair.first;
                    long ingredientId = pair.second;

                    if (!skipIngredient) {
                        // If ingredient does not exist in database, add it
                        ContentValues ingredientValues = new ContentValues();
                        ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                        ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);

                        ingredientCVList.add(ingredientValues);
                    }

                    // Add the link values to the database
                    ContentValues linkValues = new ContentValues();
                    linkValues.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
                    linkValues.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, j);
                    linkValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                    linkValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);

//                    linkCVList.add(linkValues);
                }

                // Increment the generated ID
                recipeId++;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Bulk insert recipes and ingredients that don't exist in database
        Utilities.insertAndUpdateRecipeValues(mContext, recipeCVList);
        Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

        // Bulk insert link values
        ContentValues[] linkArray = new ContentValues[linkCVList.size()];
        linkCVList.toArray(linkArray);

        mContext.getContentResolver().bulkInsert(
                LinkIngredientEntry.CONTENT_URI,
                linkArray
        );

        return null;
    }

    /**
     * Generates a URL for epicurious pointing to the location of a medium resolution image
     * @param id ID of the recipe's photograph
     * @param filename File name of the recipe's photograph image
     * @return String URL linking to the recipe's image
     */
    private String generateImgUrl(String id, String filename) {
        String BASE_IMG_URL = "http://assets.epicurious.com";
        String PHOTOS_PATH = "photos";
        String ASPECT_RATIO_PATH = "6:4";
        String QUALITY_PATH = "w_620%2Ch_413";

        // epicurious uses a common pattern for their photographs
        String imageUrl = BASE_IMG_URL + "/" +
                PHOTOS_PATH + "/" +
                id + "/" +
                ASPECT_RATIO_PATH + "/" +
                QUALITY_PATH + "/" +
                filename;
//
//        Uri imageUri = Uri.parse(BASE_IMG_URL)
//                .buildUpon()
//                .appendPath(PHOTOS_PATH)
//                .appendPath(id)
//                .appendPath(URLEncoder.encode(ASPECT_RATIO_PATH, "UTF-8"))
//                .appendPath(QUALITY_PATH)
//                .appendPath(filename)
//                .build();

        return imageUrl;
    }
}
