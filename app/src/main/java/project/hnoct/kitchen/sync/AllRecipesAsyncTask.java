package project.hnoct.kitchen.sync;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/20/2017.
 */

class AllRecipesAsyncTask extends AsyncTask<Object, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = AllRecipesAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                       // Interface for global context
    private RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading

    public AllRecipesAsyncTask(Context context, RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(Object... params) {
        /** Variables **/
        String recipeUrl = (String) params[0];
        String recipeSourceId = Utilities.getRecipeSourceIdFromUrl(mContext, recipeUrl);
        int ingredientOrder = 0;        // Used to ensure ingredient order is kept the same when added to db
        String source =  mContext.getString(R.string.attribution_allrecipes);

        // Check whether recipe exists in table or if it is a new recipe entry to be added
        long recipeId = Utilities.getRecipeIdFromSourceId(
                mContext,
                recipeSourceId,
                mContext.getString(R.string.attribution_allrecipes)
        );

        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new LinkedList<>();

        @SuppressLint("UseSparseArrays") Map<Long, String> ingredientIdNameMap = new HashMap<>();

        try {
            // Connect to the recipe URL and get the document
            Document recipeDoc = Jsoup.connect(recipeUrl).get();

            // Instantiate ContentValues to hold nutrient information and direction information
            ContentValues recipeValues = new ContentValues();

            // New recipes require all values to be populated in database
            if (recipeId == -1) {
                // Retrieve the recipe name
                String recipeName = recipeDoc.select("h1.recipe-summary__h1").first().text();

                // Retrieve recipe author
                String recipeAuthor = recipeDoc.select("span.submitter__name").first().text();

                // Retrieve recipe image
                String recipeImageUrl = recipeDoc.select("img.rec-photo").attr("src");

                // Retrieve the recipe description
                String recipeDescription = recipeDoc.select("div.submitter__description").text();
                recipeDescription = recipeDescription.substring(1, recipeDescription.length() - 1);

                // Retrieve recipe rating
                Element recipeRatingReviewElement = recipeDoc.select("section.recipe-summary")
                        .select("span[itemprop=aggregateRating]")
                        .first();

                double recipeRating = Double.parseDouble(
                        recipeRatingReviewElement.select("meta[itemprop=ratingValue]")
                                .attr("content")
                );

                // Retrieve recipe reviews
                long recipeReviews = Long.parseLong(
                        recipeRatingReviewElement.select("meta[itemprop=reviewCount]")
                                .attr("content")
                );

                // Add recipe details to ContentValues
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, Utilities.generateAllRecipesUrlFromRecipeId(recipeSourceId));
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, recipeImageUrl);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeEntry.COLUMN_RATING, recipeRating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, recipeReviews);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_allrecipes));
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
                        break;
                    }
                    case "fatContent": {
                        recipeValues.put(RecipeEntry.COLUMN_FAT, nutrientValue);
                        break;
                    }
                    case "carbohydrateContent": {
                        recipeValues.put(RecipeEntry.COLUMN_CARBS, nutrientValue);
                        break;
                    }
                    case "proteinContent": {
                        recipeValues.put(RecipeEntry.COLUMN_PROTEIN, nutrientValue);
                        break;
                    }
                    case "cholesterolContent": {
                        recipeValues.put(RecipeEntry.COLUMN_CHOLESTEROL, nutrientValue);
                        break;
                    }
                    case "sodiumContent": {
                        recipeValues.put(RecipeEntry.COLUMN_SODIUM, nutrientValue);
                        break;
                    }
                }
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

            if (recipeId == -1) {
                // If new recipe, insert the values into database
                mContext.getContentResolver().insert(
                        RecipeEntry.CONTENT_URI,
                        recipeValues
                );

                recipeId = Utilities.getRecipeIdFromSourceId(
                        mContext,
                        recipeSourceId,
                        mContext.getString(R.string.attribution_allrecipes)
                );

                if (recipeId == -1) {
                    // If unable to retrieve recipe ID, then there was an error inserting the recipe
                    // Do nothing.
                    return null;
                }
            } else {
                // If recipe exists, update the database with new recipe directions
                mContext.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        recipeValues,
                        RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?",
                        new String[] {recipeSourceId, mContext.getString(R.string.attribution_allrecipes)}
                );
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
                String quantity = ingredientQuantityPair.second;

                long allrecipesIngredientId = Long.parseLong(ingredientElement.attr("data-id"));

                // Check to see if ingredient already exists in database
                long ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient);
                boolean skipAddIngredient = false;

                if (ingredientId == -1) {
                    // If it does not, find the ID that will be automatically generated for this
                    // ingredient
                    ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
                } else {
                    skipAddIngredient = true;
                }

                // Check to see if the ingredient ID has already been used by a previous ingredient
                // for this recipe
                while (ingredientIdNameMap.containsKey(ingredientId) &&
                        !ingredient.equals(ingredientIdNameMap.get(ingredientId))) {
                    // If so, increment the ingredientID until an unused one is found
                    ingredientId++;
                }

                // Final check to see if ingredient already exists in database or ingredientIdNameMap
                String ingredientMapName = ingredientIdNameMap.get(ingredientId);

                if (ingredient.equals(ingredientMapName)) {
                    // If it exists, there is no need to add a duplicate to the ingredient table
                    skipAddIngredient = true;
                }
                ingredientIdNameMap.put(ingredientId, ingredient);

                // Create ContentValues from data
                if (!skipAddIngredient) {
                    ContentValues ingredientValue = new ContentValues();
                    ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);                     // Only used for checking, will not be inserted into database as it is auto-incremented
                    ingredientValue.put(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID,
                            allrecipesIngredientId);
                    ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);
                    ingredientCVList.add(ingredientValue);
                }

                ContentValues linkValue = new ContentValues();
                linkValue.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                linkValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                linkValue.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
                linkValue.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, ingredientOrder);

                linkCVList.add(linkValue);

                // Increment the ingredient order
                ingredientOrder++;
            }

            // Link values should not have any overlap so it should be safe to just add values
            Uri linkUri = LinkIngredientEntry.buildIngredientUriFromRecipe(recipeId);
            Cursor cursor = mContext.getContentResolver().query(
                    linkUri,
                    null,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

            ContentValues[] linkValues = new ContentValues[linkCVList.size()];
            linkCVList.toArray(linkValues);

            mContext.getContentResolver().bulkInsert(
                    LinkIngredientEntry.CONTENT_URI,
                    linkValues
            );

            // Bulk insert ingredient values
            Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

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
