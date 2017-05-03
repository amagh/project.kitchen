package project.hnoct.kitchen.sync;

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
import java.util.List;
import java.util.UUID;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/1/2017.
 */

public class EpicuriousAsyncTask extends AsyncTask<Object, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = EpicuriousAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                       // Interface for global context
    private AllRecipesAsyncTask.RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading

    public EpicuriousAsyncTask(Context context, AllRecipesAsyncTask.RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(Object... params) {
        // Variables
        String recipeUrl = (String) params[0];

        // Lists to add of information to add to database
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new ArrayList<>();

        try {
            // Retrieve the HTML document from the website
            Document document = Jsoup.connect(recipeUrl).get();

            // Retrieve the recipe information
            String recipeName = document.select("div.main-content")
                    .select("h1[itemprop=name]")
                    .text();

            String author = document.select("div.main-content")
                    .select("div.byline-source")
                    .select("cite.contributors")
                    .select("a[itemprop=author]")
                    .text();

            // Rating is stored out of 4 so needs to be converted to out of 5
            Double rating = 5 * Double.parseDouble(document.select("div.main-content")
                    .select("div.recipe-sidebar")
                    .select("div.review-rating")
                    .select("span.rating")
                    .text()
                    .replaceAll("\\/.*", "")
            ) / 4;

            int reviews = Integer.parseInt(document.select("div.main-content")
                    .select("div.recipe-sidebar")
                    .select("div.review-rating")
                    .select("span.reviews-count")
                    .text()
            );

            Element imageElement = document.select("div.recipe-image")
                    .select("picture.photo-wrap")
                    .select("source")
                    .first();

            String imageUrl = "http://assets.epicurious.com/photos/5674617eb47c050a284a4e11/6:4/w_322,h_314,c_limit/EP_12162015_placeholders_bright.jpg";
            if (imageElement != null) {
                imageUrl = "http://" + document.select("div.recipe-image")
                        .select("picture.photo-wrap")
                        .select("source")
                        .first()
                        .attr("srcset")
                        .substring(2);
            }

            // Utilize the photoID from the image URL as the recipe ID
            String recipeSourceId = Uri.parse(imageUrl).getPathSegments().get(1);
            if (recipeSourceId.equals("5674617eb47c050a284a4e11")) {
                recipeSourceId = UUID.randomUUID().toString().replace("-", "").substring(0, 24);
            }

            String description = document.select("div.content-container")
                    .select("div.main")
                    .select("div.dek")
                    .select("p")
                    .text();

            // Initialize the StringBuilder to append a new line between each direction so it can be
            // easily split later
            StringBuilder builder = new StringBuilder();

            Elements directionElements = document.select("div.recipe-and-additional-content")
                    .select("div.instructions")
                    .select("ol.preparation-groups")
                    .select("li.preparation-step");

            for (Element directionElement : directionElements) {
                String direction = directionElement.text();
                builder.append(direction)
                        .append("\n");
            }

            // Trim the last new line from the StringBuilder
            String directions = builder.toString().trim();

            String servings = document.select("div.footer")
                    .select("span.per-serving")
                    .text()
                    .replaceAll("\\D+", "");

            // Store recipe values in the ContentValues
            ContentValues recipeValues = new ContentValues();

            if (Utilities.getRecipeIdFromSourceId(mContext,
                    recipeSourceId,
                    mContext.getString(R.string.attribution_epicurious)) == -1) {

                recipeValues.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, description);
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, imageUrl);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_epicurious));
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, author);
                recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, reviews);
                recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, directions);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);
            }


            recipeValues.put(RecipeEntry.COLUMN_SERVINGS, servings);

            // Retrieve the ingredient Elements
            Elements ingredientElements = document.select("div.recipe-and-additional-content")
                    .select("div.recipe-content")
                    .select("div.ingredients-info")
                    .select("ol.ingredient-groups")
                    .select("ul.ingredients")
                    .select("li.ingredient");

            // Initialize the IngredientHelper to keep track of ingredient IDs
            Utilities.IngredientHelper helper = new Utilities.IngredientHelper(mContext);

            Elements nutritionElements = document.select("div.recipe-and-additional-content")
                    .select("div[id=additional-info-panels]")
                    .select("div.edaman-nutrition")
                    .select("div.nutrition")
                    .select("ul")
                    .select("li");

            for (Element nutritionElement : nutritionElements) {
                String nutrientType = nutritionElement.select("span.nutri-label").text();

                double nutrientValue = 0;
                if (!nutrientType.isEmpty()) {
                    nutrientValue = Double.parseDouble(nutritionElement.select("span.nutri-data").text().replaceAll(" [mg]+\\(*.*\\)*", ""));
                }

                switch (nutrientType) {
                    case "Calories": {
                        recipeValues.put(RecipeEntry.COLUMN_CALORIES, nutrientValue);
                        break;
                    }
                    case "Carbohydrates": {
                        recipeValues.put(RecipeEntry.COLUMN_CARBS, nutrientValue);
                        break;
                    }
                    case "Fat": {
                        recipeValues.put(RecipeEntry.COLUMN_FAT, nutrientValue);
                        break;
                    }
                    case "Protein": {
                        recipeValues.put(RecipeEntry.COLUMN_PROTEIN, nutrientValue);
                        break;
                    }
                    case "Sodium": {
                        recipeValues.put(RecipeEntry.COLUMN_SODIUM, nutrientValue);
                        break;
                    }
                    case "Cholesterol": {
                        recipeValues.put(RecipeEntry.COLUMN_CHOLESTEROL, nutrientValue);
                        break;
                    }
                }
            }

            boolean newRecipe = false;
            // Estimate the recipeID of the recipe to be added
            long recipeId = Utilities.getRecipeIdFromSourceId(mContext, recipeSourceId, mContext.getString(R.string.attribution_epicurious));

            if (recipeId == -1) {
                recipeId = Utilities.getRecipeIdFromUrl(mContext, recipeUrl);

                if (recipeId == -1) {
                    recipeId = Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);
                    newRecipe = true;
                }

            }

            // Initialize the variable to hold the ingredient order
            int ingredientOrder = 0;

            // Iterate through the ingredient Elements
            for (Element ingredientElement : ingredientElements) {
                // Retrieve the ingredient and quantity information
                String ingredientQuantity = ingredientElement.text();

                // Split the String into separate quantity and ingredient
                Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);
                String ingredient = ingredientQuantityPair.first;
                String quantity = ingredientQuantityPair.second;

                // Add the ingredient to the IngredientHelper
                Pair<Boolean, Long> pair = helper.addIngredient(ingredient);
                boolean skipIngredient = pair.first;
                long ingredientId = pair.second;

                // Check if ingredient needs to be added to database
                if (!skipIngredient) {
                    ContentValues ingredientValues = new ContentValues();
                    ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                    ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);

                    ingredientCVList.add(ingredientValues);
                }

                // Create ContentValues from the link values
                ContentValues linkValues = new ContentValues();
                linkValues.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, ingredientOrder);
                linkValues.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
                linkValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                linkValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);

                linkCVList.add(linkValues);

                // Iterate ingredient order
                ingredientOrder++;
            }

            if (newRecipe) {
                // Insert the recipe values
                mContext.getContentResolver().insert(
                        RecipeEntry.CONTENT_URI,
                        recipeValues
                );
            } else {
                String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(recipeId)};

                mContext.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        recipeValues,
                        selection,
                        selectionArgs
                );
            }


            // Bulk insert the ingredient values
            Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

//            // Convert the List of link values to an Array
//            ContentValues[] linkArray = new ContentValues[linkCVList.size()];
//            linkCVList.toArray(linkArray);
//
//            // Bulk insert the link values
//            mContext.getContentResolver().bulkInsert(
//                    LinkIngredientEntry.CONTENT_URI,
//                    linkArray
//            );

            checkAndInsertLinkValues(linkCVList);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void checkAndInsertLinkValues(List<ContentValues> linkCVList) {
        List<ContentValues> workingList = new ArrayList<>(linkCVList);

        for (ContentValues value : workingList) {
            long recipeId = value.getAsLong(RecipeEntry.COLUMN_RECIPE_ID);
            long ingredientId = value.getAsLong(IngredientEntry.COLUMN_INGREDIENT_ID);
            int ingredientOrder = value.getAsInteger(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER);

            Uri linkUri = LinkIngredientEntry.buildIngredientUriFromRecipe(recipeId);
            String selection = IngredientEntry.COLUMN_INGREDIENT_ID + " = ? AND " +
                    LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " = ?";
            String[] selectionArgs = new String[] {Long.toString(ingredientId), Integer.toString(ingredientOrder)};

            Cursor cursor = mContext.getContentResolver().query(
                    linkUri,
                    LinkIngredientEntry.LINK_PROJECTION,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                linkCVList.remove(value);
            }

            if (cursor != null) {
                cursor.close();
            }
        }

        ContentValues[] linkArray = new ContentValues[linkCVList.size()];
        linkCVList.toArray(linkArray);

        mContext.getContentResolver().bulkInsert(
                LinkIngredientEntry.CONTENT_URI,
                linkArray
        );
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if (mSyncCallback != null) {
            mSyncCallback.onFinishLoad();
        }
    }
}
