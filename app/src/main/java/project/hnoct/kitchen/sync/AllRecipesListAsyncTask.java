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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by hnoct on 2/15/2017.
 */

public class AllRecipesListAsyncTask extends AsyncTask<Void, Void, Void> {
    /** Constants **/
    final String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
    private final String LOG_TAG = AllRecipesListAsyncTask.class.getSimpleName();

    /** Member Variables **/
    Context mContext;       // Interface to global context
    ContentResolver mContentResolver;

    public AllRecipesListAsyncTask(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    protected Void doInBackground(Void... params) {
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

                // Get the recipe Id by converting the link to a URI and selecting the 2nd segment
                Uri recipeUri = Uri.parse(recipeUrl);
                long recipeId = Long.parseLong(recipeUri.getPathSegments().get(1));

                // Retrieve the recipe name, thumbnail URL, and description
                Element recipeElement = recipe.getElementsByClass("grid-col__rec-image").first();
                if (recipeElement == null) {
                    // Advertisements do not contain this element, so they can be skipped if found
                    continue;
                }

                // Replace some elements of the recipe title to get the recipe name
                String recipeTitle = recipeElement.attr("title");
                String recipeName = recipeTitle.replace(" Recipe", "").replace(" and Video", "");

                String recipeThumbnailUrl = recipeElement.attr("data-original-src");

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

                // Create ContentValues from values
                ContentValues recipeValues = new ContentValues();
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeEntry.COLUMN_THUMBNAIL_URL, recipeThumbnailUrl);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, RecipeContract.getCurrentTime());

                recipeCVList.add(recipeValues);
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

    private void insertAndUpdateValues(List<ContentValues> recipeCVList) {
        /** Variables **/
        int recipesInserted = 0;
        int recipesUpdated = 0;
        List<ContentValues> workingList = new ArrayList<>(recipeCVList);    // To prevent ConcurrentModificationError

        /** Constants **/
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        for (ContentValues recipeValue : workingList) {
            long recipeId = recipeValue.getAsLong(RecipeEntry.COLUMN_RECIPE_ID);
            // Check if recipe exists in database

            Cursor cursor = mContext.getContentResolver().query(
                    recipeUri,
                    null,
                    RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                    new String[] {Long.toString(recipeId)},
                    null
            );

            // Update appropriate recipes
            if (cursor.moveToFirst()) {
                // If cursor returns a row, then update the values if they have changed
                // Get the review and rating values from the ContentValues to be updated
                double dbRating = cursor.getDouble(cursor.getColumnIndex(RecipeEntry.COLUMN_RATING));
                long dbReviews = cursor.getLong(cursor.getColumnIndex(RecipeEntry.COLUMN_REVIEWS));

                if (recipeValue.getAsDouble(RecipeEntry.COLUMN_RATING) != dbRating ||
                        recipeValue.getAsLong(RecipeEntry.COLUMN_REVIEWS) != dbReviews) {
                    // Values do match database values. Update
                    mContentResolver.update(
                            recipeUri,
                            recipeValue,
                            RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                            new String[]{Long.toString(recipeId)}
                    );
                    recipesUpdated++;
                }

                // Close the cursor
                cursor.close();

                // Remove the recipeValues from the list
                recipeCVList.remove(recipeValue);
            }
        }


        // Create an ContentValues[] from the remaining values in the list
        ContentValues[] recipeValues = new ContentValues[recipeCVList.size()];

        // Add values of list to the array
        recipeCVList.toArray(recipeValues);

        // Bulk insert all remaining recipes
        recipesInserted = mContentResolver.bulkInsert(recipeUri, recipeValues);

        Log.v(LOG_TAG, recipesInserted + " recipes added and " + recipesUpdated + " recipes updated!");
    }
}
