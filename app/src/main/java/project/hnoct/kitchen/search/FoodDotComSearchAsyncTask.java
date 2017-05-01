package project.hnoct.kitchen.search;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/28/2017.
 */

public class FoodDotComSearchAsyncTask extends AsyncTask<Object, Void, List<Map<String, Object>>> {
    // Constants
    private final static String LOG_TAG = FoodDotComSearchAsyncTask.class.getSimpleName();

    // Member Variables
    Context mContext;
    SyncListener mListener;

    public FoodDotComSearchAsyncTask(Context context, SyncListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected List<Map<String, Object>> doInBackground(Object... params) {
        // Parse the passed HTML document into a JSoup Document
        Document document = Jsoup.parse((String) params[0]);

        // Select the recipe elements
        Elements recipeElements = document.select("section.fd-menu")
                .select("div.container-fluid")
                .select("div.search-results")
                .select("div.fd-recipe:not(.str-adunit)");

        // Initialize the List that will contain all the recipes
        List<Map<String, Object>> recipeList = new ArrayList<>();

        // Parse through each element and collect the recipe information
        for (Element recipeElement : recipeElements) {
            // Retrieve the recipe information
            long recipeSourceId = Long.parseLong(recipeElement.select("div.fd-recipe")
                    .attr("data-id")
                    .replace("recipe-", "")
            );

            String recipeUrl = recipeElement.select("div.fd-img-wrap")
                    .select("a[href]")
                    .attr("href");

            String imgUrl = recipeElement.select("div.fd-img-wrap")
                    .select("a[href]")
                    .select("img")
                    .attr("src");

            // Check which domain is hosting the recipe's image and modify the URL so that a
            // better quality image is returned
            if (imgUrl.contains("http://img.sndimg.com")) {
                imgUrl = imgUrl.replaceAll("upload/*.*/v1", "upload/h_420,w_560,c_fit/v1");
            } else if (imgUrl.contains("http://pictures.food.com")){
                imgUrl = imgUrl.replaceAll("jpg&.*|JPG&.*", "jpg");
                imgUrl = imgUrl + "&width=560&height=420&fit=crop&flags=progressive&quality=95";
            }

            String recipeName = recipeElement.select("div.tile-content")
                    .select("h2.title")
                    .select("a[href]")
                    .text();

            String author = recipeElement.select("div.tile-content")
                    .select("div.author")
                    .select("span.name")
                    .select("a[href]")
                    .text();

            // Rating is stored as a percentage, so it needs to be converted to a decimal and then
            // multiplied by five to get its five-star-rating
            double rating = 5 * Double.parseDouble("." + recipeElement.select("div.tile-content")
                    .select("div.meta-data")
                    .select("div.fd-rating")
                    .select("div.five-star")
                    .select("span.fd-rating-percent")
                    .attr("style")
                    .replaceAll("[a-z]*:|\\.|%;", "")
            );

            int reviews = Integer.parseInt(recipeElement.select("div.tile-content")
                    .select("div.meta-data")
                    .select("div.fd-rating")
                    .select("span")
                    .text()
                    .replaceAll("\\(|\\)", "")
            );

            // Store the recipe information to a HashMap
            Map<String, Object> reviewMap = new HashMap<>();

            reviewMap.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
            reviewMap.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
            reviewMap.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
            reviewMap.put(RecipeEntry.COLUMN_IMG_URL, imgUrl);
            reviewMap.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
            reviewMap.put(RecipeEntry.COLUMN_RATING, rating);
            reviewMap.put(RecipeEntry.COLUMN_REVIEWS, reviews);
            reviewMap.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
            reviewMap.put(RecipeEntry.COLUMN_FAVORITE, 0);
            reviewMap.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_food));

//            for (String key : reviewMap.keySet()) {
//                Log.d(LOG_TAG, "Key: " + key + " | " + reviewMap.get(key));
//            }

            recipeList.add(reviewMap);
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Map<String, Object>> list) {
        if (mListener != null) {
            mListener.onFinishLoad(list);
        }
    }

    public interface SyncListener {
        void onFinishLoad(List<Map<String, Object>> recipeList);
    }
}
