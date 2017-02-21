package project.hnoct.kitchen.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/20/2017.
 */

public class AllRecipeAsyncTask extends AsyncTask<String, Void, Void> {
    /** Constants **/

    /** Member Variables **/
    Context mContext;           // interface for global context

    public AllRecipeAsyncTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(String... params) {
        String recipeUrl = params[0];
        long recipeId = Long.parseLong(params[1]);

        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new ArrayList<>();

        try {
            // Connect to the recipe URL and get the document
            Document recipeDoc = Jsoup.connect(recipeUrl).get();

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

                // Retrieve the ingredientId from the Element
                Long ingredientId = Long.parseLong(ingredientElement.attr("data-id"));

                // Create ContentValues from data
                ContentValues ingredientValue = new ContentValues();
                ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);

                ContentValues linkValue = new ContentValues();
                linkValue.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                linkValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                linkValue.put(LinkEntry.COLUMN_QUANTITY, quantity);

//                System.out.println("Ingredient: " + ingredient + " (" +ingredientId + ") | Quantity: " + quantity);
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

                // Use StringBuilder to build direction String
                builder.append(direction + "\n");
            }

            // Create String from StringBuilder and trim the final appended new line ('\n')
            String directions = builder.toString().trim();

            // Create ContentValues from directions
            ContentValues recipeValue = new ContentValues();
            recipeValue.put(RecipeEntry.COLUMN_DIRECTIONS, directions);

            // Update the database with new recipe directions
            mContext.getContentResolver().update(
                    RecipeEntry.CONTENT_URI,
                    recipeValue,
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

            insertAndUpdateIngredientValues(ingredientCVList);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void insertAndUpdateIngredientValues(List<ContentValues> ingredientCVList) {
        // Bulk insert ingredient and link information
        for (ContentValues ingredientValue : ingredientCVList) {
            // Check if ingredient is already in the database, if so, skip it
            long ingredientId = ingredientValue.getAsLong(IngredientEntry.COLUMN_INGREDIENT_ID);

            Cursor cursor = mContext.getContentResolver().query(
                    RecipeEntry.CONTENT_URI,
                    null,
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?",
                    new String[] {Long.toString(ingredientId)},
                    null
            );

            if (cursor.moveToFirst()) {
                // Remove the ContentValues from the list to be bulk inserted
                ingredientCVList.remove(ingredientValue);

                // Close the Cursor
                cursor.close();
            }
        }
        ContentValues[] ingredientValues = new ContentValues[ingredientCVList.size()];
        ingredientCVList.toArray(ingredientValues);

        mContext.getContentResolver().bulkInsert(
                IngredientEntry.CONTENT_URI,
                ingredientValues
        );
    }

}
