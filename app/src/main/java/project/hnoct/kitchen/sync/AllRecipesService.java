package project.hnoct.kitchen.sync;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

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
import project.hnoct.kitchen.ui.ActivityRecipeList;

/**
 * Created by hnoct on 5/2/2017.
 */

public class AllRecipesService extends IntentService {
    /** Constants **/
    private final String LOG_TAG = AllRecipesService.class.getSimpleName();

    /** Member Variables **/
    private Context mContext = this;
    private long mTimeInMillis;

    public AllRecipesService() {
        super("AllRecipesService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mTimeInMillis = intent.getLongExtra(getString(R.string.extra_time), 0);

        // Instantiate variable to hold time recipes were added. Will subtract one to the time to
        // each subsequent recipe added as to preserve order
        long timeAdded = Utilities.getCurrentTime();

        try {
            List<ContentValues> recipeCVList = new ArrayList<>();
            // Connect and downloading the HTML document
            String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
            Document document = Jsoup.connect(ALL_RECIPES_BASE_URL).get();

            // Select the elements from the document to add to the database as part of the recipe
            Elements recipes = document.select("article.grid-col--fixed-tiles:not(.marketing-card):not(.hub-card)");

            Random random = new Random();

            for (Element recipe : recipes) {
                // Retrieve the href for the recipe
                Element recipeLinkElement = recipe.getElementsByTag("a").first();
                if (recipeLinkElement == null) {
                    // Some advertisements do not contain an href and should be skipped
                    continue;
                }
                String recipeUrl = recipeLinkElement.attr("href");

                if (!recipeUrl.contains("recipe")) {
                    // Skip any links that don't direct to a recipe
                    continue;
                }

                // Prepend Allrecipes base URL to the recipe URL to get the full length URL
                recipeUrl = ALL_RECIPES_BASE_URL + recipeUrl;

                // Get the recipe Id by converting the link to a URI and selecting the 2nd segment
                String recipeSourceId = Utilities.getRecipeSourceIdFromUrl(mContext, recipeUrl);

                // Retrieve the recipe name, thumbnail URL, and description
                Element recipeElement = recipe.getElementsByClass("grid-col__rec-image").first();
                if (recipeElement == null) {
                    // Advertisements do not contain this element, so they can be skipped if found
                    continue;
                }

                // Replace some elements of the recipe title to get the recipe name
                String recipeName = recipe.select("h3.grid-col__h3").text();

                // Retrieve the thumbnail URL
                String recipeThumbnailUrl = recipeElement.attr("data-original-src");

                // Convert the thumbnail URL to the imageURL
                /** @see Utilities#getAllRecipesImageUrlFromThumbnailUrl(String) **/
                String recipeImageUrl = Utilities.getAllRecipesImageUrlFromThumbnailUrl(recipeThumbnailUrl);

                // Recipe description contains name of recipe, so it is removed
                String recipeDescription = recipe.select("div.rec-card__description").text();

                // Retrieve the rating
                Element ratingElement = recipe.getElementsByClass("rating-stars").first();
                if (ratingElement == null) {
                    // Second check for advertisements as they do not contain ratings
                    continue;
                }
                double rating = Double.parseDouble(ratingElement.attr("data-ratingstars"));

                // Retrieve the number of reviews
                Element reviewElement = recipe.getElementsByTag("format-large-number").first();
                long reviews = Long.parseLong(reviewElement.attr("number"));

                // Retrieve the author of the recipe
                Element authorElement = recipe.select("ul.cook-details").first().select("h4").first();
                String recipeAuthor = authorElement.text().replace("Recipe by ", "").trim();

                // Create ContentValues from values
                ContentValues recipeValues = new ContentValues();
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_IMG_URL, recipeImageUrl);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_DATE_ADDED, mTimeInMillis);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValues.put(RecipeContract.RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_allrecipes));

                recipeCVList.add(recipeValues);

                int randomNum = random.nextInt((50-10) + 1) + 10;
                mTimeInMillis -= randomNum;
            }

            Utilities.insertAndUpdateRecipeValues(mContext, recipeCVList);
        } catch(IOException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
            Log.d(LOG_TAG, "Error parsing document", e);
            e.printStackTrace();
        }
    }
}
