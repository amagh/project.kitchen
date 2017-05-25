package project.kitchen.search;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/28/2017.
 */

public class AllRecipesSearchAsyncTask extends AsyncTask<Object, Void, List<Map<String, Object>>> {
    // Constants
    private static final String LOG_TAG = AllRecipesSearchAsyncTask.class.getSimpleName();

    // Member Variables
    Context mContext;
    SearchListener mListener;

    public AllRecipesSearchAsyncTask(Context context, SearchListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected List<Map<String, Object>> doInBackground(Object... params) {
        // Constants
        String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
        String ALL_RECIPES_SEARCH_PATH = "search";
        String ALL_RECIPES_RESULTS_PATH = "results";
        String ALL_RECIPES_EMPTY_PATH = "";
        String ALL_RECIPES_QUERY_PARAM = "wt";
        String ALL_RECIPES_SORT_PARAM = "sort";
        String ALL_RECIPES_SORT_VALUE = "re";

        // Retrieve the search term passed as an argument
        String searchTerm = (String) params[0];

        // Initialize the List to hold all the recipe information
        List<Map<String, Object>> recipeList = new ArrayList<>();

        try {
            // Build the URI for searching AllRecipes.com for the user's search term
            Uri allRecipesSearchUri = Uri.parse(ALL_RECIPES_BASE_URL)
                    .buildUpon()
                    .appendPath(ALL_RECIPES_SEARCH_PATH)
                    .appendPath(ALL_RECIPES_RESULTS_PATH)
                    .appendPath(ALL_RECIPES_EMPTY_PATH)
                    .appendQueryParameter(ALL_RECIPES_QUERY_PARAM, searchTerm)
                    .appendQueryParameter(ALL_RECIPES_SORT_PARAM, ALL_RECIPES_SORT_VALUE)
                    .build();

            // Convert the URI to a URL
            String searchUrl = allRecipesSearchUri.toString().replace("%20", " ");

            // Connect to the site and create a Jsoup Document from it
            Document document = Jsoup.connect(searchUrl).get();

            // Parse the document for recipe information
            Elements recipeElements = document.select("article"); //.grid-col--fixed-tiles"); //:not(.marketing-card):not(.hub-card)");

            // Iterate through the recipe Elements and retrieve the recipe information
            for (Element recipeElement : recipeElements) {

                // Retrieve the data type of the Element and check that it is a recipe
                Element dataTypeElement = recipeElement.select("ar-save-item.favorite").first();
                if (dataTypeElement == null) {
                    // If data type element does not exist, then it is not a recipe, skip.
                    continue;
                }

                String dataType = recipeElement.select("ar-save-item.favorite")
                        .first()
                        .attr("data-type");

                if (dataType == null || !dataType.equals("'Recipe'")) {
                    // If the Element is not a recipe, skip it
                    continue;
                }

                // Retrieve the recipe information
                String recipeSourceId = recipeElement.select("ar-save-item.favorite")
                        .attr("data-id");

                String recipeUrl = ALL_RECIPES_BASE_URL + recipeElement.select("a[href]")
                        .attr("href");

                String imgUrl = recipeElement.select("img.grid-col__rec-image")
                        .attr("data-original-src");

                // Modify the image URL to point to a medium-resolution version
                imgUrl = imgUrl.replace("250x250", "560x315");

                String recipeName = recipeElement.select("h3.grid-col__h3")
                        .text();

                double rating = Double.parseDouble(recipeElement.select("div.rating-stars")
                        .attr("data-ratingstars")
                );

                int reviews = Integer.parseInt(recipeElement.select("format-large-number")
                        .attr("number")
                );

                String description = recipeElement.select("a[href]")
                        .select("div.rec-card__description")
                        .text();

                String author = recipeElement.select("div.profile")
                        .select("h4")
                        .text()
                        .replace("Recipe by ", "");

                // Save all the recipe information into a HashMap
                Map<String, Object> recipeMap = new HashMap<>();

                recipeMap.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeMap.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeMap.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeMap.put(RecipeEntry.COLUMN_IMG_URL, imgUrl);
                recipeMap.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeMap.put(RecipeEntry.COLUMN_SHORT_DESC, description);
                recipeMap.put(RecipeEntry.COLUMN_RATING, rating);
                recipeMap.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeMap.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeMap.put(RecipeEntry.COLUMN_FAVORITE, false);
                recipeMap.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_allrecipes));

//                for (String key : recipeMap.keySet()) {
//                    Log.d(LOG_TAG, key + ": " + recipeMap.get(key));
//                }

                recipeList.add(recipeMap);
            }

        } catch (IOException e) {
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
}
