package project.hnoct.kitchen.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/22/2017.
 */

public class SeriousEatsListAsyncTask extends AsyncTask<Void, Void, Void> {
    /** Constants **/
    private final String LOG_TAG = SeriousEatsListAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;       // Interface to global context
    private ContentResolver mContentResolver;
    private long mTimeInMillis;

    public SeriousEatsListAsyncTask(Context context, long timeInMillis) {
        // Initialize member variables
        mContext = context;
        mContentResolver = mContext.getContentResolver();
        mTimeInMillis = timeInMillis;
    }
    @Override
    protected Void doInBackground(Void... voids) {
        // Instantiate variable to hold time recipes were added. Will subtract one to the time to
        // each subsequent recipe added as to preserve order
        long timeAdded = Utilities.getCurrentTime();

        try {
            List<ContentValues> recipeCVList = new ArrayList<>();
            // Connect and downloading the HTML document
            String SERIOUS_EATS_BASE_URL = "http://www.seriouseats.com/recipes";

            // Retrieve the HTML document using Jsoup
            Document document = Jsoup.connect(SERIOUS_EATS_BASE_URL).get();

            // Get individual recipe information
            Elements recipeElements = document.select("section.block")
                    .select("div.block__wrapper")
                    .select("div.module");

            // Initialize a List to hold recipeIds that are to be inserted to prevent duplicate
            // recipes from being inserted multiple times
            List<String> importedRecipes = new ArrayList<>();

            // Used to increment the timeAdded a random amount of time between 10-50ms for each recipe
            // This will allow recipes from each source to be woven into each other instead of being
            // separated
            Random random = new Random();

            for (Element recipeElement : recipeElements)  {
                // Check that the element is for a recipe and not a "technique" or "collection"
                Element dataTypeElement = recipeElement.select("a.category-link").first();

                // If element does not contain data type, then skip it
                if (dataTypeElement == null) continue;

                String dataType = dataTypeElement.attr("data-click-id");

                // If not recipe, skip
                if (dataType == null || !dataType.equals("Recipes")) continue;

                // Get the recipe information
                String recipeSourceId = recipeElement.attr("data-postid");

                // Check to ensure recipe isn't already in the list of recipe to be added
                // If it is already on the List, then skip
                if (importedRecipes.contains(recipeSourceId)) continue;

                // Add the recipe to the List to prevent multiple insertions of same recipe
                importedRecipes.add(recipeSourceId);

                String recipeUrl = recipeElement.select("a[href").first().attr("href");
                String imgUrl = recipeElement.select("img.module__image").first().attr("data-src");
                String title = recipeElement.select("div.module__wrapper")
                        .select("div.metadata")
                        .select("a.module__link")
                        .select("h4.title")
                        .text();
                String author = recipeElement.select("div.module__wrapper")
                        .select("div.metadata")
                        .select("a.module__link")
                        .select("p.author")
                        .text();

                // Create ContentValues to hold recipe information to be inserted
                ContentValues recipeValue = new ContentValues();
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_NAME, title);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValue.put(RecipeEntry.COLUMN_DATE_ADDED, mTimeInMillis);
                recipeValue.put(RecipeEntry.COLUMN_IMG_URL, imgUrl);
                recipeValue.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValue.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_seriouseats));

                // Add the ContentValues to the List of recipe values to be bulk inserted
                recipeCVList.add(recipeValue);

                // Increment the timeAdded
                int randomNum = random.nextInt((50-10) + 1) + 10;
                mTimeInMillis -= randomNum;
            }

            // Bulk insert recipe information
            Utilities.insertAndUpdateRecipeValues(mContext, recipeCVList);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
