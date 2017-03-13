package project.hnoct.kitchen.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/20/2017.
 */

public class AllRecipesAsyncTask extends AsyncTask<String, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = AllRecipesAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                       // Interface for global context
    private RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading
    private boolean mNewRecipe;                      // For setting whether the data should be inserted or updated

    public AllRecipesAsyncTask(Context context, RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(String... params) {
        /** Variables **/
        String recipeUrl = params[0];
        long recipeId = Utilities.getRecipeIdFromAllRecipesUrl(recipeUrl);
        int ingredientOrder = 0;        // Used to ensure ingredient order is kept the same when added to db

        // Check whether recipe exists in table or if it is a new recipe entry to be added
        Cursor cursor = mContext.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                null,
                RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?",
                new String[] {Long.toString(recipeId), AllRecipesListAsyncTask.ALL_RECIPES_ATTRIBUTION},
                null
        );

        // Set the parameter indicating if this is a new recipe
        mNewRecipe = !cursor.moveToFirst();

        List<Long> ingredientIdList = new ArrayList<>();        // Hack for duplicate ingredients. See below.
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new LinkedList<>();

        try {
            // Connect to the recipe URL and get the document
            Document recipeDoc = Jsoup.connect(recipeUrl).get();

            // Instantiate ContentValues to hold nutrient information and direction information
            ContentValues recipeValues = new ContentValues();

            // New recipes require all values to be populated in database
            if (mNewRecipe) {
                // Retrieve the recipe name
                String recipeName = recipeDoc.select("h1.recipe-summary__h1").first().text();

                Log.d(LOG_TAG, "Recipe name: " + recipeName);

                // Retrieve recipe author
                String recipeAuthor = recipeDoc.select("span.submitter__name").first().text();

//                Log.d(LOG_TAG, "Recipe author: " + recipeAuthor);

                // Retrieve recipe image
                String recipeImageUrl = recipeDoc.select("img.rec-photo").attr("src");

//                Log.d(LOG_TAG, "Recipe image URL: " + recipeImageUrl);

                // Retrieve the thumbnail image
                String recipeThumbnailUrl = recipeDoc.select("section.hero-photo--downsized")
                        .select("ar-save-item.favorite")
                        .attr("data-imageurl");

//                Log.d(LOG_TAG, "Recipe thumbnail URL: " + recipeThumbnailUrl);

                // Retrieve the recipe description
                String recipeDescription = recipeDoc.select("div.submitter__description").text();
                recipeDescription = recipeDescription.substring(1, recipeDescription.length() - 1);
//                Log.d(LOG_TAG, "Recipe description: " + recipeDescription);

                // Retrieve recipe rating
                Element recipeRatingReviewElement = recipeDoc.select("section.recipe-summary")
                        .select("span[itemprop=aggregateRating]")
                        .first();

                double recipeRating = Double.parseDouble(
                        recipeRatingReviewElement.select("meta[itemprop=ratingValue]")
                                .attr("content")
                );

//                Log.d(LOG_TAG, "Recipe rating: " + recipeRating);

                // Retrieve recipe reviews
                long recipeReviews = Long.parseLong(
                        recipeRatingReviewElement.select("meta[itemprop=reviewCount]")
                                .attr("content")
                );

                // Add recipe details to ContentValues
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, Utilities.generateAllRecipesUrlFromRecipeId(recipeId));
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, recipeImageUrl);
                recipeValues.put(RecipeEntry.COLUMN_THUMBNAIL_URL, recipeThumbnailUrl);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeEntry.COLUMN_RATING, recipeRating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, recipeReviews);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, AllRecipesListAsyncTask.ALL_RECIPES_ATTRIBUTION);
//                Log.d(LOG_TAG, "Recipe reviews: " + recipeReviews);
            }

            // Retrieve the serving count for the recipe
            Element servingElement = recipeDoc.select("meta[id=metaRecipeServings]").first();
            int recipeServings = Integer.parseInt(servingElement.attr("content"));
            recipeValues.put(RecipeEntry.COLUMN_SERVINGS, recipeServings);

            // Select all Elements containing nutrition information
            Elements nutritionElements = recipeDoc.select("div.recipe-nutrition__form")
                    .select("ul.nutrientLine");
            for (Element nutrientElement : nutritionElements) {
                // Retrieve the nutrient type and value
                Element nutrientTypeElement = nutrientElement.select("li.nutrientLine__item--amount").first();
                String nutrientType = nutrientTypeElement.attr("itemprop");
                double nutrientValue = Double.parseDouble(
                        nutrientTypeElement.select("span").first().text()
                );

                // Add a ContentValues pair based on nutrient type
                switch (nutrientType) {
                    case "calories": {
                        recipeValues.put(RecipeEntry.COLUMN_CALORIES, nutrientValue);
                    }
                    case "fatContent": {
                        recipeValues.put(RecipeEntry.COLUMN_FAT, nutrientValue);
                    }
                    case "carbohydrateContent": {
                        recipeValues.put(RecipeEntry.COLUMN_CARBS, nutrientValue);
                    }
                    case "proteinContent": {
                        recipeValues.put(RecipeEntry.COLUMN_PROTEIN, nutrientValue);
                    }
                    case "cholesterolContent": {
                        recipeValues.put(RecipeEntry.COLUMN_CHOLESTEROL, nutrientValue);
                    }
                    case "sodiumContent": {
                        recipeValues.put(RecipeEntry.COLUMN_SODIUM, nutrientValue);
                    }
                }
            }

            // Select all Elements containing ingredient information
            Elements ingredientElements = recipeDoc.select("span.recipe-ingred_txt").select("[itemprop='ingredients'");
            for (Element ingredientElement : ingredientElements) {
                // Get the text from the element
                String ingredientAndQuantity = ingredientElement.text();

                // Convert the String containing both the ingredient and the quantity to a Pair
                /** @see Utilities#getIngredientQuantity(String) **/
                Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientAndQuantity);

                // Separate the Pair into two Strings
                String ingredient = ingredientQuantityPair.first;

                // Convert fractions in the quantity to Unicode equivalents if they exist
                String quantity = ingredientQuantityPair.second;

                // Check to see if ingredient already exists in database
                long ingredientId;
                if ((ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient)) == -1) {
                    // Ingredient not found in database. Retrieve the ingredientId from the Element
                    ingredientId = Long.parseLong(ingredientElement.attr("data-id"));
                }

                if (ingredientId <= 0) {
                    // Ingredients without ingredientIds are section headings and should be relegated
                    // to an unused spot in the ingredient table or new ingredients added by the user
                    ingredientId = 1000000;

                    // Generate a new ingredientId if needed
                    // Check to see if ingredientId already matches an ingredient
                    String databaseIngredientName = Utilities.getIngredientNameFromId(mContext, ingredientId);
                    while (databaseIngredientName != null && !ingredient.equals(databaseIngredientName)) {
                        // Keep incrementing to find a new ID if the ingredientName already exists
                        // and does not match the ingredient being queried
                        ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
                        databaseIngredientName = Utilities.getIngredientNameFromId(mContext, ingredientId);
                    }
                }

                /**
                 * ** Hack for duplicate ingredients in a single recipe **
                 * Increment the ingredientId until there are no more instances of it in the
                 * ingredientIdList used to hold all ingredientIds in this recipe
                 */
                while (ingredientIdList.contains(ingredientId)) {
                    // If ingredientId exists in List, then increment the ingredientId
                    ingredientId++;
                }
                ingredientIdList.add(ingredientId);

                // Create ContentValues from data
                ContentValues ingredientValue = new ContentValues();
                ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);

                ingredientCVList.add(ingredientValue);

                ContentValues linkValue = new ContentValues();
                linkValue.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                linkValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                linkValue.put(RecipeEntry.COLUMN_SOURCE, AllRecipesListAsyncTask.ALL_RECIPES_ATTRIBUTION);
                linkValue.put(LinkEntry.COLUMN_QUANTITY, quantity);
                linkValue.put(LinkEntry.COLUMN_INGREDIENT_ORDER, ingredientOrder);

                linkCVList.add(linkValue);

                // Increment the ingredient order
                ingredientOrder++;
            }

            // Instantiate StringBuilder that will be used to combine all directions into single
            // new line ('\n') separated String
            StringBuilder builder = new StringBuilder();

            // Get all Elements containing direction information
            Elements directionElements = recipeDoc.select("ol.recipe-directions__list")
                    .select("[itemprop='recipeInstructions']")
                    .select("li.step");

            for (Element directionElement : directionElements) {
                // Retrieve text from Element
                String direction = directionElement.text();

                // Use StringBuilder to build direction String and append new line ('\n')
                builder.append(direction).append("\n");
            }

            // Create String from StringBuilder and trim the final appended new line ('\n')
            String directions = builder.toString().trim();

            // Create ContentValues from directions
            recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, directions);

            if (mNewRecipe) {
                // If new recipe, insert the values into database
                mContext.getContentResolver().insert(
                        RecipeEntry.CONTENT_URI,
                        recipeValues
                );
            } else {
                // If recipe exists, update the database with new recipe directions
                mContext.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        recipeValues,
                        RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                        new String[] {Long.toString(recipeId)}
                );
            }


            // Link values should not have any overlap so it should be safe to just add values
            ContentValues[] linkValues = new ContentValues[linkCVList.size()];
            linkCVList.toArray(linkValues);

            mContext.getContentResolver().bulkInsert(
                    LinkEntry.CONTENT_URI,
                    linkValues
            );

            // Bulk insert ingredient values
            Utilities.insertIngredientValues(mContext, ingredientCVList);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        mSyncCallback.onFinishLoad();
    }

    public interface RecipeSyncCallback {
        public void onFinishLoad();
    }

}
