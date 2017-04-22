package project.hnoct.kitchen.sync;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;
import android.util.StringBuilderPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
 * Created by hnoct on 4/20/2017.
 */

public class FoodDotComAsyncTask extends AsyncTask<Object, Void, Void> {
    /** Constants **/
    private static final String LOG_TAG = AllRecipesAsyncTask.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                       // Interface for global context
    private AllRecipesAsyncTask.RecipeSyncCallback mSyncCallback;       // For letting the UI thread know finished loading

    public FoodDotComAsyncTask(Context context, AllRecipesAsyncTask.RecipeSyncCallback syncCallback) {
        mContext = context;
        mSyncCallback = syncCallback;
    }

    @Override
    protected Void doInBackground(Object... params) {
        /** Variables **/
        String recipeUrl = (String) params[0];
        long recipeSourceId = Utilities.getRecipeSourceIdFromUrl(mContext, recipeUrl);
        int ingredientOrder = 0;        // Used to ensure ingredient order is kept the same when added to db

        // Check whether recipe exists in table or if it is a new recipe entry to be added
        Cursor cursor = mContext.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                null,
                RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?",
                new String[] {Long.toString(recipeSourceId), mContext.getString(R.string.attribution_food)},
                null
        );

        // Set the parameter indicating if this is a new recipe
        boolean mNewRecipe = !(cursor != null && cursor.moveToFirst());

        long recipeId = -1;
        if (cursor != null) {
            recipeId = cursor.getLong(RecipeEntry.IDX_RECIPE_ID);
            cursor.close();
        }

        // Initialize the Lists that will hold the ContentValues to be inserted into the
        // ingredients table and ingredient link table
        List<ContentValues> ingredientCVList = new ArrayList<>();
        List<ContentValues> linkCVList = new LinkedList<>();

        // Map for ensuring ingredients aren't added multiple times into the database and that
        // the ingredientId matches the one being inserted in the link ingredient table
        @SuppressLint("UseSparseArrays") Map<Long, String> ingredientIdNameMap = new HashMap<>();

        try {
            // Retrieve the HTML document and convert to String
            Document document = Jsoup.connect(recipeUrl).get();
            String html = document.toString();

            // Retrieve the rating from the document
            double rating = Double.parseDouble(document.select("div.fd-container-full")
                    .select("div.fd-container")
                    .select("header.recipe")
                    .select("section.recipe-subhead")
                    .select("div.recipe-info")
                    .select("div.fd-rating")
                    .select("span.sr-only")
                    .first()
                    .text()
            );

            // Retrieve the directions from the document
            Elements directionElements = document.select("div[data-module=recipeDirections")
                    .select("ol")
                    .select("li");

            // Initialize a StringBuilder to modify the directions so that they can be easily
            // split later when reading from the database
            StringBuilder builder = new StringBuilder();
            for (Element directionElement : directionElements) {
                // Retrieve the direction and replace HTML code with Unicode for readability
                String direction = directionElement.text()
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&reg;", "\u00ae")
                        .replaceAll("&deg;", "\u00b0");

                // Check to make sure the direction is an actual step
                if (!direction.equals("Submit a Correction")) {
                    // Append the StringBuilder with the direction and a new line (\n) so that it
                    // can be easily split later
                    builder.append(direction).append("\n");
                }
            }

            // Convert the directions to String and trim off the final \n
            String recipeDirections = builder.toString().trim();

            // Split the HTML String document into lines for easier navigation
            String lines[] = html.split("\n");

            // Initialize the JSON Object that will hold the recipe information
            JSONObject json = null;

            // Iterate through the lines to find the JSON Object
            for (String line : lines) {
                if (line.contains("<script type=\"application/ld+json\">")) {
                    // Modify the line to remove the non-JSON parts of the String
                    line = line.replace("<script type=\"application/ld+json\">", "");
                    line = line.replace("</script>", "");

                    // Convert to JSON Object
                    json = new JSONObject(line);
                }
            }

            if (json == null) {
                // If no JSON Object is found, do not proceed
                return null;
            }

            // Variables for navigating the JSON Object
            String FOOD_RECIPE_NAME = "name";
            String FOOD_RECIPE_DESC = "description";
            String FOOD_RECIPE_AUTHOR = "author";
            String FOOD_RECIPE_IMG_URL = "image";
            String FOOD_RECIPE_REVIEWS_OBJ = "aggregateRating";
            String FOOD_RECIPE_REVIEWS = "reviewCount";

            String FOOD_RECIPE_NUTRITION_OBJ = "nutrition";
            String FOOD_RECIPE_CALORIES = "calories";
            String FOOD_RECIPE_FAT = "fatContent";
            String FOOD_RECIPE_CHOLESTEROL = "cholesterolContent";
            String FOOD_RECIPE_SODIUM = "sodiumContent";
            String FOOD_RECIPE_CARBS = "carbohydrateContent";
            String FOOD_RECIPE_PROTEIN = "proteinContent";
            String FOOD_RECIPE_SERVINGS = "recipeYield";

            // Retrieve the recipe information from the JSON Object
            String recipeName = json.getString(FOOD_RECIPE_NAME);
            String recipeDescription = json.getString(FOOD_RECIPE_DESC);
            String recipeAuthor = json.getString(FOOD_RECIPE_AUTHOR);
            String recipeImgUrl = json.getString(FOOD_RECIPE_IMG_URL)
                    // Replace Unicode with String characters
                    .replaceAll("\u0026", "&")
                    .replaceAll("\u003d", "=");

            JSONObject reviewsJsonObject = json.getJSONObject(FOOD_RECIPE_REVIEWS_OBJ);
            long recipeReviews = reviewsJsonObject.getLong(FOOD_RECIPE_REVIEWS);

            JSONObject nutritionJsonObject = json.getJSONObject(FOOD_RECIPE_NUTRITION_OBJ);
            double recipeCalories = nutritionJsonObject.getDouble(FOOD_RECIPE_CALORIES);
            double recipeFat = nutritionJsonObject.getDouble(FOOD_RECIPE_FAT);
            double recipeCholesterol = nutritionJsonObject.getDouble(FOOD_RECIPE_CHOLESTEROL);
            double recipeSodium = nutritionJsonObject.getDouble(FOOD_RECIPE_SODIUM);
            double recipeCarbs = nutritionJsonObject.getDouble(FOOD_RECIPE_CARBS);
            double recipeProtein = nutritionJsonObject.getDouble(FOOD_RECIPE_PROTEIN);

            int recipeServings = Integer.parseInt(json.getString(FOOD_RECIPE_SERVINGS).split("-")[0].split(" ")[0]);

            // Create a ContentValues to hold the recipe information to be inserted into database
            ContentValues recipeValues = new ContentValues();
            if (mNewRecipe) {
                // If new recipe, add all recipe information not already in database
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
                recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, recipeDescription);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
                recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
                recipeValues.put(RecipeEntry.COLUMN_IMG_URL, recipeImgUrl);
                recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
                recipeValues.put(RecipeEntry.COLUMN_REVIEWS, recipeReviews);
                recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, Utilities.getCurrentTime());
                recipeValues.put(RecipeEntry.COLUMN_FAVORITE, 0);
                recipeValues.put(RecipeEntry.COLUMN_SOURCE, mContext.getString(R.string.attribution_food));
            }

            recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, recipeDirections);
            recipeValues.put(RecipeEntry.COLUMN_CALORIES, recipeCalories);
            recipeValues.put(RecipeEntry.COLUMN_FAT, recipeFat);
            recipeValues.put(RecipeEntry.COLUMN_CHOLESTEROL, recipeCholesterol);
            recipeValues.put(RecipeEntry.COLUMN_SODIUM, recipeSodium);
            recipeValues.put(RecipeEntry.COLUMN_CARBS, recipeCarbs);
            recipeValues.put(RecipeEntry.COLUMN_PROTEIN, recipeProtein);
            recipeValues.put(RecipeEntry.COLUMN_SERVINGS, recipeServings);

            if (recipeId != -1) {
                // If recipeID was retrieved from the the database, then the information should be
                // used to update the database
                mContext.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        recipeValues,
                        RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                        new String[] {Long.toString(recipeId)}
                );
            } else {
                // If no recipeID was retrieved, then recipe is new and needs to be inserted
                mContext.getContentResolver().insert(
                        RecipeEntry.CONTENT_URI,
                        recipeValues
                );

                // Query the database to get the recipeID for the newly inserted recipe
                cursor = mContext.getContentResolver().query(
                        RecipeEntry.CONTENT_URI,
                        RecipeEntry.RECIPE_PROJECTION,
                        RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?",
                        new String[] {Long.toString(recipeSourceId), mContext.getString(R.string.attribution_food)},
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    recipeId = cursor.getLong(RecipeEntry.IDX_RECIPE_ID);
                    cursor.close();
                } else {
                    // If unable to query database for the recipe, then there was an error inserting.
                    // Stop importing.
                    return null;
                }
            }

            // Get the ingredient Elements from the document
            Elements ingredientElements = document.select("section.container")
                    .select("div.row")
                    .select("div.ingredients")
                    .select("ul.ingredient-list")
                    .select("li[data-ingredient]");

            // Iterate through and retrieve the ingredient information
            for (Element ingredientElement : ingredientElements) {
                // Replace the weird HTML markup superscript and subscript characters with regular ones
                String ingredientQuantity = ingredientElement.text()
                        .replaceAll("1", "1")
                        .replaceAll("3", "3")
                        .replaceAll("⁄", "/")
                        .replaceAll("2", "2")
                        .replaceAll("4", "4");

                // Ingredient ID is only found in the URL, so retrieve the URL
                String ingredientIdUrl = ingredientElement.select("a[href]").attr("href");

                // Utilize Regex to retrieve the ingredient ID
                Pattern pattern = Pattern.compile("[0-9]+$");
                Matcher match = pattern.matcher(ingredientIdUrl);

                long foodIngredientId = -1;
                if (match.find()) {
                    foodIngredientId = Long.parseLong(match.group());
                }

                // Split the ingredientQuantity String to separate Strings
                Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);
                String ingredient = ingredientQuantityPair.first;
                String quantity = ingredientQuantityPair.second.replace(" -", "-");

                // Check to see if ingredient already exists in database
                long ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient);

                if (ingredientId == -1) {
                    // If it does not, find the ID that will be automatically generated for this
                    // ingredient
                    ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
                }

                // Check to see if the ingredient ID has already been used by a previous ingredient
                // for this recipe
                while (ingredientIdNameMap.containsKey(ingredientId) &&
                        !ingredient.equals(ingredientIdNameMap.get(ingredientId))) {
                    // If so, increment the ingredientID until an unused one is found
                    ingredientId++;
                }

                // Final check to see if ingredient already exists in ingredientIdNameMap
                boolean skipAddIngredient = false;
                String ingredientMapName = ingredientIdNameMap.get(ingredientId);

                if (ingredient.equals(ingredientMapName)) {
                    // If it exists, there is no need to add a duplicate to the ingredient table
                    skipAddIngredient = true;
                }

                // Add the ingredient ID to ingredientIdNameMap to keep track of which IDs have
                // already been used
                ingredientIdNameMap.put(ingredientId, ingredient);

                // Check to see if the ingredient doesn't exist in database
                if (!skipAddIngredient) {
                    // If it does not exist, create a ContentValues so it can be inserted into
                    // database
                    ContentValues ingredientValue = new ContentValues();
                    ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                    ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);
                    ingredientValue.put(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID, foodIngredientId);

                    ingredientCVList.add(ingredientValue);
                }

                // Create ContentValues for inserting ingredient and quantity into link ingredient
                // table
                ContentValues linkValue = new ContentValues();
                linkValue.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, ingredientOrder);
                linkValue.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
                linkValue.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                linkValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);

                linkCVList.add(linkValue);

                // Increment ingredient order
                ingredientOrder++;
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        // Insert ingredient values into database
        Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

        // Convert the List of ContentValues for link ingredient table to an Array
        ContentValues[] linkCVArray = new ContentValues[linkCVList.size()];
        linkCVList.toArray(linkCVArray);

        // Insert the link ingredient values
        mContext.getContentResolver().bulkInsert(
                LinkIngredientEntry.CONTENT_URI,
                linkCVArray
        );

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
