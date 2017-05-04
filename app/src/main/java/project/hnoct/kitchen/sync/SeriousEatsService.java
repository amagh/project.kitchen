package project.hnoct.kitchen.sync;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/3/2017.
 */

public class SeriousEatsService extends RecipeSyncService {
    // Constants

    // Member Variables
    private long mTimeInMillis;
    private Intent mBroadcastIntent;

    public SeriousEatsService() {
        super("SeriousEatsService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Initialize the BroadcastIntent
        mBroadcastIntent = new Intent(getString(R.string.intent_filter_sync_finished));

        // Set the default flag as success -- any errors encountered will change the flag;
        mBroadcastIntent.setFlags(SYNC_SUCCESS);

        // Get the seed time
        mTimeInMillis = intent.getLongExtra(getString(R.string.extra_time), 0);

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
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME, title);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_DATE_ADDED, mTimeInMillis);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_IMG_URL, imgUrl);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValue.put(RecipeContract.RecipeEntry.COLUMN_SOURCE, getString(R.string.attribution_seriouseats));

                // Add the ContentValues to the List of recipe values to be bulk inserted
                recipeCVList.add(recipeValue);

                // Increment the timeAdded
                int randomNum = random.nextInt((50-10) + 1) + 10;
                mTimeInMillis -= randomNum;
            }

            // Bulk insert recipe information
            Utilities.insertAndUpdateRecipeValues(this, recipeCVList);
        } catch (IOException e) {
            // If there is an error connecting to the site, add the server down flag
            mBroadcastIntent.setFlags(SYNC_SERVER_DOWN);
            e.printStackTrace();
        }

        if (mBroadcastIntent.getFlags() == SYNC_SUCCESS) {
            // If there are no errors in importing recipes, then update the last sync time
            Utilities.updateLastSynced(this);
        }

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.sendBroadcast(mBroadcastIntent);
    }
}
