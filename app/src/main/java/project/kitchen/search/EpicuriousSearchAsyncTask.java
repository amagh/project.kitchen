package project.kitchen.search;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/2/2017.
 */

public class EpicuriousSearchAsyncTask extends AsyncTask<Object, Void, List<Map<String, Object>>> {
    // Constants
    private final static String LOG_TAG = EpicuriousSearchAsyncTask.class.getSimpleName();

    // Member Variables
    Context mContext;
    SearchListener mListener;

    public EpicuriousSearchAsyncTask(Context context, SearchListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected List<Map<String, Object>> doInBackground(Object... params) {
        // Retrieve the search term from user-input
        String searchTerm = (String) params[0];

        // Constants for building the URL for searching epicurious for the search term
        String EPICURIOUS_BASE_URL = "http://www.epicurious.com";
        String EPICURIOUS_SEARCH_PATH = "search";
        String EPICURIOUS_CONTENT_QUERY_PARAM = "content";
        String EPICURIOUS_CONTENT_QUERY_VALUE = "recipe";

        // Build the URI
        Uri searchUri = Uri.parse(EPICURIOUS_BASE_URL)
                .buildUpon()
                .appendPath(EPICURIOUS_SEARCH_PATH)
                .appendPath(searchTerm)
                .appendQueryParameter(EPICURIOUS_CONTENT_QUERY_PARAM, EPICURIOUS_CONTENT_QUERY_VALUE)
                .build();

        // Initialize the List that will contain all the recipes
        List<Map<String, Object>> recipeList = new ArrayList<>();

        try {
            // Connect to the URI for the search term
            Document document = Jsoup.connect(searchUri.toString().replace("%20", " ")).get();

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

            // Retrieve the Array holding all the recipe information
            JSONObject jsonContext = json.getJSONObject(EPI_CONTEXT_OBJ);
            JSONObject jsonDispatcher = jsonContext.getJSONObject(EPI_DISPATCHER_OBJ);
            JSONObject jsonStores = jsonDispatcher.getJSONObject(EPI_STORES_OBJ);
            JSONObject jsonSearch = jsonStores.getJSONObject(EPI_SEARCHSTORE_OBJ);
            JSONArray jsonResultsArray = jsonSearch.getJSONArray(EPI_RESULTS_ARRAY);
            JSONObject jsonNull = (jsonResultsArray.getJSONObject(1));
            JSONArray jsonItemArray = jsonNull.getJSONArray(EPI_ITEMS_ARRAY);

            // Iterate through the recipes and store their information
            for (int i = 0; i < jsonItemArray.length(); i++) {
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

                Map<String, Object> map = new HashMap<>();
                map.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                map.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                map.put(RecipeEntry.COLUMN_SHORT_DESC, description);
                map.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                map.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                map.put(RecipeEntry.COLUMN_IMG_URL, imageUrl);
                map.put(RecipeEntry.COLUMN_RATING, rating);
                map.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                map.put(RecipeEntry.COLUMN_FAVORITE, false);
                map.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_epicurious));

                recipeList.add(map);
            }
        } catch (IOException |JSONException e) {
            e.printStackTrace();
        }
        return recipeList;
    }

    @Override
    protected void onPostExecute(List<Map<String, Object>> list) {
        if (mListener != null) {
            mListener.onSearchFinished(list);
        }
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

        return imageUrl;
    }
}
