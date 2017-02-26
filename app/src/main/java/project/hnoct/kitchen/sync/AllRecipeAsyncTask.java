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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/20/2017.
 */

public class AllRecipeAsyncTask extends AsyncTask<String, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = AllRecipeAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                       // interface for global context
    private RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading

    public AllRecipeAsyncTask(Context context, RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(String... params) {
        /** Variables **/
        String recipeUrl = params[0];
        long recipeId = Long.parseLong(params[1]);
        int ingredientOrder = 0;        // Used to ensure ingredient order is kept the same when added to db

        List<Long> ingredientIdList = new ArrayList<>();        // Hack for duplicate ingredients. See below.
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new LinkedList<>();

        try {
            // Connect to the recipe URL and get the document
            Document recipeDoc = Jsoup.connect(recipeUrl).get();

            // Instantiate ContentValues to hold nutrient information and direction information
            ContentValues recipeValues = new ContentValues();

            // Retrieve the serving count for the recipe
            Element servingElement = recipeDoc.select("span.servings-count").first();
            int servings = Integer.parseInt(servingElement.text());

            // Select all Elements containing nutrition information
            Elements nutritionElements = recipeDoc.select("div.recipe-nutrition__form");
            for (Element nutrientElement : nutritionElements) {
                // Retrieve the nutrient type and value
                Element nutrientTypeElement = nutrientElement.select("ul.nutrientLine")
                        .select("li.nutrientLine__item--amount").first();
                String nutrientType = nutrientTypeElement.attr("itemprop");
                long nutrientValue = Long.parseLong(
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

                // Convert fractions to Unicode equivalents if they exist
                String quantity = Utilities.convertToUnicodeFraction(mContext, ingredientQuantityPair.second);

                // Retrieve the ingredientId from the Element
                long ingredientId = Long.parseLong(ingredientElement.attr("data-id"));
                if (ingredientId == 0) {
                    // Ingredients without ingredientIds are section headings and should be relegated
                    // to an unused spot in the ingredient table
                    ingredientId = 1000000;
                }

                // Generate a new ingredientId if needed
                String databaseIngredientName = Utilities.getIngredientNameFromId(mContext, ingredientId);
                while (databaseIngredientName != null && !ingredient.equals(databaseIngredientName)) {
                    ingredientId = Utilities.generateNewId(mContext, ingredientId, Utilities.INGREDIENT_TYPE);
                    databaseIngredientName = Utilities.getIngredientNameFromId(mContext, ingredientId);
                }

                /**
                 * ** Hack for duplicate ingredients in a single recipe **
                 * Increment the ingredientId until there are no more instances of it in the
                 * ingredientIdList used to hold all ingredientIds in this recipe
                 */
                while (ingredientIdList.contains(ingredientId)) {
                    // If ingredientId exists in List, then generate new ID that does not already exist in database
                    ingredientId = Utilities.generateNewId(mContext, ++ingredientId, Utilities.INGREDIENT_TYPE);
                    Log.d(LOG_TAG, "Ingredient ID (" + ingredientId + ") already exists!");
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

            // Update the database with new recipe directions
            mContext.getContentResolver().update(
                    RecipeEntry.CONTENT_URI,
                    recipeValues,
                    RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                    new String[] {Long.toString(recipeId)}
            );

            // Link values should not have any overlap so it should be safe to just add values
            ContentValues[] linkValues = new ContentValues[linkCVList.size()];
            linkCVList.toArray(linkValues);

            mContext.getContentResolver().bulkInsert(
                    LinkEntry.CONTENT_URI,
                    linkValues
            );

            // Bulk insert ingredient values
            insertAndUpdateIngredientValues(ingredientCVList);

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

    private void insertAndUpdateIngredientValues(List<ContentValues> ingredientCVList) {
        // Duplicate the list as to avoid ConcurrentModificationError
        List<ContentValues> workingList = new LinkedList<>(ingredientCVList);

        // Bulk insert ingredient and link information
        for (ContentValues ingredientValue : workingList) {
            // Check if ingredient is already in the database, if so, skip it
            long ingredientId = ingredientValue.getAsLong(IngredientEntry.COLUMN_INGREDIENT_ID);

            Cursor cursor = mContext.getContentResolver().query(
                    IngredientEntry.CONTENT_URI,
                    null,
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?",
                    new String[] {Long.toString(ingredientId)},
                    null
            );

            if (cursor.moveToFirst()) {
                // Remove the ContentValues from the list to be bulk inserted
                ingredientCVList.remove(ingredientValue);
            }
            // Close the Cursor
            cursor.close();
        }
        ContentValues[] ingredientValues = new ContentValues[ingredientCVList.size()];
        ingredientCVList.toArray(ingredientValues);

        mContext.getContentResolver().bulkInsert(
                IngredientEntry.CONTENT_URI,
                ingredientValues
        );
    }

    public interface RecipeSyncCallback {
        public void onFinishLoad();
    }

}
