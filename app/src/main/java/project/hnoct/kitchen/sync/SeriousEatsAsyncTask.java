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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/22/2017.
 */

public class SeriousEatsAsyncTask extends AsyncTask<Object, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = SeriousEatsAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;
    private AllRecipesAsyncTask.RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading

    public SeriousEatsAsyncTask(Context context, AllRecipesAsyncTask.RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(Object... params) {
        String recipeUrl = (String) params[0];
        int ingredientOrder = 0;        // Used to ensure ingredient order is kept the same when added to db

        // Check whether recipe exists in table or if it is a new recipe entry to be added

        long recipeId = Utilities.getRecipeIdFromUrl(
                mContext,
                recipeUrl
        );

        // Initialize the Lists that will hold the ContentValues to be inserted into the
        // ingredients table and ingredient link table
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new LinkedList<>();

        // Map for ensuring ingredients aren't added multiple times into the database and that
        // the ingredientId matches the one being inserted in the link ingredient table
        @SuppressLint("UseSparseArrays") Map<Long, String> ingredientIdNameMap = new HashMap<>();

        try {
            Document document = Jsoup.connect(recipeUrl).get();
            ContentValues recipeValue = new ContentValues();

            // Retrieve the recipe information from the HTML document
            // Recipe Source ID
            String recipeSourceId = document.select("div.content-main")
                    .attr("data-id");

            if (recipeId == -1) {
                // If recipe does not already exist in database, then all information needs to be
                // retrieved from HTML document
                String title = document.select("div.content-main")
                        .select("section.entry-container")
                        .select("div.entry-header-inner")
                        .select("h1.recipe-title")
                        .text();

                String author = document.select("div.content-main")
                        .select("section.entry-container")
                        .select("div.entry-header-inner")
                        .select("div.author-byline")
                        .select("div.name-contact")
                        .select("a.name")
                        .text();

                String imgUrl = document.select("div.content-main")
                        .select("section.entry-container")
                        .select("figure.recipe-main-photo")
                        .select("div.se-pinit-image-container")
                        .select("img[src]")
                        .attr("src");

                // Add the recipe info to ContentValues to be inserted into database
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_NAME, title);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeValue.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValue.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeValue.put(RecipeEntry.COLUMN_IMG_URL, imgUrl);
                recipeValue.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValue.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_seriouseats));
            }

            // Description
            String description = document.select("div.content-main")
                    .select("section.entry-container")
                    .select("div.recipe-body")
                    .select("p:not(.caption)")
                    .first()
                    .text();

            // Initialize StringBuilder that will append each additional direction separated by a
            // new line (\n) so they can be easily split later
            StringBuilder builder = new StringBuilder();
            Elements directionElements = document.select("div.content-main")
                    .select("section.entry-container")
                    .select("div.recipe-body")
                    .select("div.recipe-wrapper")
                    .select("div.recipe-procedures")
                    .select("ol.recipe-procedures-list")
                    .select("li.recipe-procedure");

            for (Element directionElement : directionElements) {
                String direction = directionElement.select("div.recipe-procedure-text")
                        .select("p")
                        .text();

                builder.append(direction).append("\n");
            }

            // Get the direction from the StringBuilder and trim off the final appended new line
            String direction = builder.toString().trim();

            // Number of servings
            String servingString = document.select("div.content-main")
                    .select("section.entry-container")
                    .select("div.recipe-body")
                    .select("div.recipe-wrapper")
                    .select("span[itemprop=recipeYield]")
                    .text();

            // Utilize Regex to retrieve the first number in the text as the serving size
            Pattern pattern = Pattern.compile("\\d+");
            Matcher match = pattern.matcher(servingString);

            int servings = 1;
            if (match.find()) {
                servings = Integer.parseInt(match.group());
            }

            // Add information to ContentValues to be inserted
            recipeValue.put(RecipeEntry.COLUMN_SHORT_DESC, description);
            recipeValue.put(RecipeEntry.COLUMN_DIRECTIONS, direction);
            recipeValue.put(RecipeEntry.COLUMN_SERVINGS, servings);

            // Check if recipe already exists in database or not
            if (recipeId != -1) {
                // If it already exists in database, then update database values with new values
                String selection = RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?";
                String[] selectionArgs = new String[] {recipeSourceId, mContext.getString(R.string.attribution_seriouseats)};

                mContext.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        recipeValue,
                        selection,
                        selectionArgs
                );
            } else {
                // If it does not exist, insert the recipe into the database
                mContext.getContentResolver().insert(
                        RecipeEntry.CONTENT_URI,
                        recipeValue
                );

                // Retrieve the recipe ID of the entry just inserted
                recipeId = Utilities.getRecipeIdFromSourceId(
                        mContext,
                        recipeSourceId,
                        mContext.getString(R.string.attribution_seriouseats)
                );
            }

            // Retrieve ingredient information from HTML document
            Elements ingredients = document.select("div.content-main")
                    .select("section.entry-container")
                    .select("div.recipe-body")
                    .select("div.recipe-wrapper")
                    .select("div.recipe-ingredients")
                    .select("ul")
                    .select("li");

            for (Element ingredientElement : ingredients) {
                String ingredientQuantity = ingredientElement.text();

                // Utilize Regex to strip ingredient info of metric measurements
                ingredientQuantity = ingredientQuantity
                        .replaceAll(" *\\(\\d+\\.*\\d*[kgml]+\\)", "")
                        .replaceAll("\\w* *\\d\\.*\\d*[kgml]+ *; *", "")
                        .replaceAll("; \\d+\\.*\\d* *[kgml]+", "");

                // Get the separated ingredient name and quantity information
                Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);

                String ingredient = ingredientQuantityPair.first;
                String quantity = ingredientQuantityPair.second;

                // Check to see if ingredient already exists in database
                long ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient);
                boolean skipAddIngredient = false;

                if (ingredientId == -1) {
                    // If it does not, find the ID that will be automatically generated for this
                    // ingredient
                    ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
                } else {
                    // If ingredient ID can be retrieved from database, then it does not need to be
                    // added again
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Insert ingredient values into database
        Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

        // Convert the List of ContentValues for link ingredient table to an Array
        ContentValues[] linkCVArray = new ContentValues[linkCVList.size()];
        linkCVList.toArray(linkCVArray);

        // Insert the link ingredient values
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

        if (cursor != null) {
            cursor.close();
        }

        mContext.getContentResolver().bulkInsert(
                LinkIngredientEntry.CONTENT_URI,
                linkCVArray
        );

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mSyncCallback != null) {
            mSyncCallback.onFinishLoad();
        }
    }
}
