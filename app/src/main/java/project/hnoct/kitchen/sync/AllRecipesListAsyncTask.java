package project.hnoct.kitchen.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hnoct on 2/15/2017.
 */

public class AllRecipesListAsyncTask extends AsyncTask<Void, Void, Void> {
    /** Constants **/
    private final String LOG_TAG = AllRecipesListAsyncTask.class.getSimpleName();
    private final String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
    private final String ALL_RECIPES_ATTRIBUTION = "Allrecipes.com";

    /** Member Variables **/
    Context mContext;       // Interface to global context
    ContentResolver mContentResolver;

    public AllRecipesListAsyncTask(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    protected Void doInBackground(Void... params) {
        // Instantiate variable to hold time recipes were added. Will subtract one to the time to
        // each subsequent recipe added as to preserve order
        long timeAdded = RecipeContract.getCurrentTime();

        try {
            List<ContentValues> recipeCVList = new ArrayList<>();
            // Connect and downloading the HTML document
            Document document = Jsoup.connect(ALL_RECIPES_BASE_URL).get();

            // Select the elements from the document to add to the database as part of the recipe
            Elements recipes = document.select("article.grid-col--fixed-tiles").select("article:not(.marketing-card)");

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
                long recipeId = Utilities.getRecipeIdFromAllRecipesUrl(recipeUrl);

                // Retrieve the recipe name, thumbnail URL, and description
                Element recipeElement = recipe.getElementsByClass("grid-col__rec-image").first();
                if (recipeElement == null) {
                    // Advertisements do not contain this element, so they can be skipped if found
                    continue;
                }

                // Replace some elements of the recipe title to get the recipe name
                String recipeTitle = recipeElement.attr("title");
                String recipeName = recipeTitle.replace(" Recipe", "").replace(" and Video", "");

                // Retrieve the thumbnail URL
                String recipeThumbnailUrl = recipeElement.attr("data-original-src");

                // Convert the thumbnail URL to the imageURL
                /** @see Utilities#getImageUrlFromThumbnailUrl(String) **/
                String recipeImageUrl = Utilities.getImageUrlFromThumbnailUrl(recipeThumbnailUrl);

                // Recipe description contains name of recipe, so it is removed
                String recipeDescription = recipeElement.attr("alt");
                recipeDescription = recipeDescription.substring(recipeTitle.length() + 3);

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
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeEntry.COLUMN_THUMBNAIL_URL, recipeThumbnailUrl);
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, recipeImageUrl);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, timeAdded);
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, ALL_RECIPES_ATTRIBUTION);

                recipeCVList.add(recipeValues);

                timeAdded--;
            }

            insertAndUpdateValues(recipeCVList);
        } catch(IOException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
            Log.d(LOG_TAG, "Error parsing document", e);
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Checks to make sure the recipe doesn't already exist in the database prior to bulk-insertion
     * @param recipeCVList List containing recipes to be bulk-inserted
     */
    private void insertAndUpdateValues(List<ContentValues> recipeCVList) {
        /** Variables **/
        int recipesInserted;
        int recipesUpdated = 0;
        List<ContentValues> workingList = new ArrayList<>(recipeCVList);    // To prevent ConcurrentModificationError

        /** Constants **/
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        for (ContentValues recipeValue : workingList) {
            long recipeId = recipeValue.getAsLong(RecipeEntry.COLUMN_RECIPE_ID);
            // Check if recipe exists in database

            Cursor cursor = mContext.getContentResolver().query(
                    recipeUri,
                    RecipeEntry.RECIPE_PROJECTION,
                    RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                    new String[] {Long.toString(recipeId)},
                    null
            );

            // Update appropriate recipes
            if (cursor.moveToFirst()) {
                if (!recipeValue.get(RecipeEntry.COLUMN_RECIPE_NAME).equals(cursor.getString(RecipeEntry.IDX_RECIPE_NAME))) {
                    // Recipe ID exists, but for with a different recipe name. generate new recipeID
                    long newRecipeId = Utilities.generateNewId(mContext, recipeId, Utilities.RECIPE_TYPE);

                    // Replace the recipeId in the ContentValues
                    recipeValue.put(RecipeEntry.COLUMN_RECIPE_ID, newRecipeId);

                    // Skip this entry
                    continue;
                }
                // If cursor returns a row, then update the values if they have changed
                // Get the review and rating values from the ContentValues to be updated
                double dbRating = cursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);
                long dbReviews = cursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);

                if (recipeValue.getAsDouble(RecipeEntry.COLUMN_RATING) != dbRating ||
                        recipeValue.getAsLong(RecipeEntry.COLUMN_REVIEWS) != dbReviews) {
                    // Values do match database values. Update database.
                    // Remove date from ContentValues so that the update doesn't push the recipe to
                    // the top
                    recipeValue.remove(RecipeEntry.COLUMN_DATE_ADDED);

                    mContentResolver.update(
                            recipeUri,
                            recipeValue,
                            RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                            new String[]{Long.toString(recipeId)}
                    );
                    recipesUpdated++;
                }

                // Remove the recipeValues from the list to be bulk-inserted
                recipeCVList.remove(recipeValue);
            }
            // Close the cursor
            cursor.close();
        }


        // Create a ContentValues[] from the remaining values in the list
        ContentValues[] recipeValues = new ContentValues[recipeCVList.size()];

        // Add values of list to the array
        recipeCVList.toArray(recipeValues);

        // Bulk insert all remaining recipes
        recipesInserted = mContentResolver.bulkInsert(recipeUri, recipeValues);

        Log.v(LOG_TAG, recipesInserted + " recipes added and " + recipesUpdated + " recipes updated!");
    }
}
