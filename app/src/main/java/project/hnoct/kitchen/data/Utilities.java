package project.hnoct.kitchen.data;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v4.util.Pair;
import android.util.DisplayMetrics;
import android.util.Log;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterNutrition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hnoct on 2/20/2017.
 */

public class Utilities {
    /** Constants **/
    private static final String LOG_TAG = Utilities.class.getSimpleName();

    public static final int RECIPE_TYPE = 0;
    public static final int INGREDIENT_TYPE = 1;

    private static final int CUSTOM_RECIPE_URI = 0;
    public static final int ALLRECIPES_URI = 1;
    public static final int FOOD_URI = 2;
    public static final int SERIOUSEATS_URI = 3;
    public static final int EPICURIOUS_URI = 4;

    public static final String URI = "uri";
    public static final String PROJECTION = "projection";
    public static final String SELECTION = "selection";
    public static final String SELECTION_ARGS = "selectionArgs";
    public static final String SORT_ORDER = "sortOrder";

    /** Member Variables **/

    /**
     *
     * @param reviews
     * @return
     */
    public static String formatReviews(Context context, long reviews) {
        // Append 'review(s)' to the number of reviews
        if (reviews != 1) {
            return context.getString(R.string.format_reviews, reviews);
        } else {
            return context.getString(R.string.format_reviews_single, reviews);
        }
    }

    /**
     *
     * @param rating
     * @return
     */
    public static String formatRating(double rating) {
        // Truncate rating to a single decimal point
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(rating) + "\u2605";
    }

    /**
     * Helper method for splitting quantity and ingredient from a single String that contains both
     * @param ingredientAndQuantity String containing quantity and a single ingredient
     * @return Pair with ingredient as First and quantity as Second
     * TODO: Move measurements within parentheses to end
     */
    public static Pair<String,String> getIngredientQuantity(String ingredientAndQuantity) {
        // Retrieve array of measurements from RecipeContract
        String[] measurements = IngredientEntry.measurements;

        for (String measurement : measurements) {
            if (measurement.equals("g")) {
                Pattern pattern = Pattern.compile("\\d+g");
                Matcher match = pattern.matcher(ingredientAndQuantity);

                String quantity = null;
                if (match.find()) {
                    quantity = match.group(0);
                }

                if (quantity == null) {
                    continue;
                }

                String ingredient = ingredientAndQuantity.replaceAll(quantity, "").trim();

                return new Pair<>(ingredient, quantity);
            }

            // Check to see if any measurement is used in the input String
            if (ingredientAndQuantity.contains(measurement)) {
                // If found, split the String in two so that the quantity and ingredient are separated

                // Make sure the measurement isn't just part of an ingredient
                // e.g. 'can' in 'American cheese'
                if (ingredientAndQuantity.charAt(ingredientAndQuantity.indexOf(measurement) - 1) != ' ') {
                    continue;
                }

                // Get the last index of the measurement in the input String
                int lastCharIdx = ingredientAndQuantity.indexOf(measurement) + measurement.length();

                if (ingredientAndQuantity.charAt(lastCharIdx) == 's' ||
                        ingredientAndQuantity.charAt(lastCharIdx) == ')') {
                    // Check to see if next character is an 's' or end parenthesis ')'. If so, include it
                    lastCharIdx++;
                }

                if (ingredientAndQuantity.length() == lastCharIdx || ingredientAndQuantity.charAt(lastCharIdx) != ' ') {
                    // If the index of the last character of the quantity is the length of the
                    // String or part of another word then skip
                    continue;
                }

                // Create two strings as substrings of the inputString
                String ingredientQuantity = ingredientAndQuantity.substring(0, lastCharIdx).trim();
                String ingredient = ingredientAndQuantity.substring(lastCharIdx).trim();

                if (ingredientQuantity.length() > 20) {
                    // If the ingredient quantity is an abnormal length, it is usually because the
                    // measurement is a clarification within the ingredient and not the actual
                    // quantity
                    //
                    // e.g. 1 large bunch lacinato (Tuscan) kale, washed, tough stems
                    // removed and discarded, and roughly chopped (about 300g ; 10 ounces after
                    // de-stemming) - ounces will be caught, but is not the actual quantity of the
                    // ingredient
                    continue;
                }

                // Return the Strings as a Pair
                return new Pair<>(ingredient, ingredientQuantity);
            }
        }
        // If the input String does not contain any known measurements, then use Regex to
        // obtain the quantity of the ingredient
        Pattern pattern = Pattern.compile("([0-9]* *-* *[1-9]+\\/*[1-9]*) (.*)");
        Matcher matcher = pattern.matcher(ingredientAndQuantity);

        if (matcher.find()) {
            // If quantity is found, create new Strings from the groups matched
            String ingredientQuantity = matcher.group(1).trim();
            String ingredient = matcher.group(2).trim();
            return new Pair<>(ingredient, ingredientQuantity);
        } else {
            // If no quantity is found, then ingredient is informal measurement
            // e.g. dash of paprika, salt and pepper to taste, etc.
            // Return an empty String as the quantity
            return new Pair<>(ingredientAndQuantity, "");
        }
    }

    /**
     * Helper method for obtaining recipeId from the recipe URL
     * @param recipeUrl URL for the recipe
     * @return recipeId as long
     */
    public static String getRecipeIdFromAllRecipesUrl(String recipeUrl) {
        // Check to ensure that the URL is prepended with "http://"
        if (!recipeUrl.contains("http://") && !recipeUrl.substring(0, 7).equals("http://")) {
            // Correct the URL
            recipeUrl = "http://" + recipeUrl;
        }
        // Convert URL to URI
        Uri recipeLinkUri = Uri.parse(recipeUrl);

        // Return 2nd segment of the URI as the recipeId
        return recipeLinkUri.getPathSegments().get(1);
    }

    /**
     * Converts thumbnail URL to higher resolution image URL
     * @param thumbnailUrl URL of the thumbnail as String
     * @return URL of the image as String
     */
    public static String getAllRecipesImageUrlFromThumbnailUrl(String thumbnailUrl) {
        /** Constants **/
        final String ALL_RECIPES_IMAGE_URL_BASE = "http://images.media-allrecipes.com/userphotos/560x315/";

        // Convert URL to URI
        Uri thumbnailUri = Uri.parse(thumbnailUrl);

        // The last segment of the URI does not change based on photo size, so it is retrieved and
        // appended to the image URL base
        String photoId = thumbnailUri.getLastPathSegment();

        // Append the photoId to the image URL's base and return the result
        return Uri.parse(ALL_RECIPES_IMAGE_URL_BASE).buildUpon()
                .appendPath(photoId)
                .toString();
    }

    /**
     * Generates the Allrecipes URL for a recipe given its recipeId
     * @param recipeId Allrecipes recipeId
     * @return String URL for link to recipe on AllRecipes website
     */
    public static String generateAllRecipesUrlFromRecipeId(String recipeId) {
        final String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
        Uri recipeUri = Uri.parse(ALL_RECIPES_BASE_URL).buildUpon()
                .appendPath("recipe")
                .appendPath(recipeId)
                .build();

        return recipeUri.toString();
    }

    /**
     * Replaces the fraction in a String with its Unicode equivalent
     * @param context Interface to global context
     * @param quantity String containing a fraction
     * @return String containing unicode fraction
     */
    public static String convertToUnicodeFraction(Context context, String quantity) {
        // Find and replace all fractions with their Unicode equivalent
        if (quantity.contains("1/4")) {
            quantity = quantity.replaceAll(" {0,1}1/4", context.getString(R.string.one_quarter_fraction));
        }
        if (quantity.contains("1/2")) {
            quantity = quantity.replaceAll(" {0,1}1/2", context.getString(R.string.one_half_fraction));
        }
        if (quantity.contains("3/4")) {
            quantity = quantity.replaceAll(" {0,1}3/4", context.getString(R.string.three_quarter_fraction));
        }
        if (quantity.contains("1/8")) {
            quantity = quantity.replaceAll(" {0,1}1/8", context.getString(R.string.one_eighth_fraction));
        }
        if (quantity.contains("3/8")) {
            quantity = quantity.replaceAll(" {0,1}3/8", context.getString(R.string.three_eighth_fraction));
        }
        if (quantity.contains("5/8")) {
            quantity = quantity.replaceAll(" {0,1}5/8", context.getString(R.string.five_eighth_fraction));
        }
        if (quantity.contains("1/3")) {
            quantity = quantity.replaceAll(" {0,1}1/3", context.getString(R.string.one_third_fraction));
        }
        if (quantity.contains("2/3")) {
            quantity = quantity.replaceAll(" {0,1}2/3", context.getString(R.string.two_third_fraction));
        }
        if (quantity.contains("1/5")) {
            quantity = quantity.replaceAll(" {0,1}1/5", context.getString(R.string.one_fifth_fraction));
        }
        if (quantity.contains("2/5")) {
            quantity = quantity.replaceAll(" {0,1}2/5", context.getString(R.string.two_fifth_fraction));
        }
        if (quantity.contains("3/5")) {
            quantity = quantity.replaceAll(" {0,1}3/5", context.getString(R.string.three_fifth_fraction));
        }
        if (quantity.contains("4/5")) {
            quantity = quantity.replaceAll(" {0,1}4/5", context.getString(R.string.four_fifth_fraction));
        }

        // Return the modified String
        return quantity;
    }

    /**
     * @see #convertToUnicodeFraction(Context, String) but doesn't remove the preceeding space for
     * direction formatting
     * @param context Interface to global context
     * @param direction Directions for a recipe
     * @return Directions for a recipe with fractions replaced with their Unicode equivalent
     */
    public static String convertToUnicodeFractionForDirections(Context context, String direction) {
        // Find and replace all fractions with their Unicode equivalent
        if (direction.contains("1/4")) {
            direction = direction.replaceAll("1/4", context.getString(R.string.one_quarter_fraction));
        }
        if (direction.contains("1/2")) {
            direction = direction.replaceAll("1/2", context.getString(R.string.one_half_fraction));
        }
        if (direction.contains("3/4")) {
            direction = direction.replaceAll("3/4", context.getString(R.string.three_quarter_fraction));
        }
        if (direction.contains("1/8")) {
            direction = direction.replaceAll("1/8", context.getString(R.string.one_eighth_fraction));
        }
        if (direction.contains("3/8")) {
            direction = direction.replaceAll("3/8", context.getString(R.string.three_eighth_fraction));
        }
        if (direction.contains("5/8")) {
            direction = direction.replaceAll("5/8", context.getString(R.string.five_eighth_fraction));
        }
        if (direction.contains("1/3")) {
            direction = direction.replaceAll("1/3", context.getString(R.string.one_third_fraction));
        }
        if (direction.contains("2/3")) {
            direction = direction.replaceAll("2/3", context.getString(R.string.two_third_fraction));
        }
        if (direction.contains("1/5")) {
            direction = direction.replaceAll("1/5", context.getString(R.string.one_fifth_fraction));
        }
        if (direction.contains("2/5")) {
            direction = direction.replaceAll("2/5", context.getString(R.string.two_fifth_fraction));
        }
        if (direction.contains("3/5")) {
            direction = direction.replaceAll("3/5", context.getString(R.string.three_fifth_fraction));
        }
        if (direction.contains("4/5")) {
            direction = direction.replaceAll("4/5", context.getString(R.string.four_fifth_fraction));
        }

        return direction;
    }

    /**
     * Queries the databse for the favorite status of a recipe
     * @param context Interface to global Context
     * @param recipeId Recipe ID of the recipe to be queried
     * @return boolean value for whether the recipe is a favorite
     */
    public static boolean getRecipeFavorite(Context context, long recipeId) {
        // Setup selection and selection arguments for querying the database
        String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ?";
        String[] selectionArgs = new String[] {Long.toString(recipeId)};

        // Query the database to get current favorite status of the recipe
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                null
        );

        // Instantiate the boolean value to return
        boolean favorite = false;

        if (cursor != null && cursor.moveToFirst()) {
            // Retrieve the favorite status from the database
            favorite = (cursor.getInt(RecipeEntry.IDX_FAVORITE) == 1);
            cursor.close();
        }

        return favorite;
    }

    /**
     * Toggles the favorite status of the recipe
     * @param context Interface for global Context
     * @param recipeId Id of the recipe to be toggle favorite status
     * @return boolean status of whether the recipe is favorite after the click
     */
    public static boolean setRecipeFavorite(Context context, long recipeId) {
        /** Constants **/
        String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ?";
        String[] selectionArgs = new String[] {Long.toString(recipeId)};

        // Query the database to get current favorite status of the recipe
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // Favorite is set to true if 1, else false
            boolean favorite = (cursor.getInt(RecipeEntry.IDX_FAVORITE) == 1);

            // Close the cursor so the new status can be written to the database
            cursor.close();

            // Instantiate the ContentValues that will contain the new favorite value
            ContentValues favoriteValue = new ContentValues();
            if (favorite) {
                // If true, set the favorite value to false (0)
                favoriteValue.put(RecipeEntry.COLUMN_FAVORITE, 0);

                // Update the database with the new value
                context.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        favoriteValue,
                        selection,
                        selectionArgs
                );

            } else {
                // See above
                favoriteValue.put(RecipeEntry.COLUMN_FAVORITE, 1);
                context.getContentResolver().update(
                        RecipeEntry.CONTENT_URI,
                        favoriteValue,
                        selection,
                        selectionArgs
                );
            }

            // Return the new favorite value
            return !favorite;

        } else {
            return false;
        }
    }

    /**
     * Splits the directions up into individual Strings containing one step each
     * @param directions String containing all directions for a recipe
     * @return LinkedList containing each step of the recipe
     */
    public static List<String> getDirectionList(String directions) {
        // Split the directions into individual Strings containing one step each
        String[] directionArray = directions.split("\n");

        // Create LinkedList that will hold all the directions. Since order is imperative, the List
        // must be Linked
        List<String> directionList = new LinkedList<>();

        for (String direction : directionArray) {
            // For each direction in the array, add it to the list
            directionList.add(direction.trim());
        }

        return directionList;
    }

    /**
     * Extracts the height of the statusbar from system resources
     * @param context Interface for global context
     * @return height of the statusbar
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Abbreviates the measurements as to better fit small form-factor screens
     * @param quantity String containing the quantity and measurement of an ingredient
     * @return String containing the quantity and abbreviated measurement of an ingredient
     */
    public static String abbreviateMeasurements(String quantity) {
        if (quantity.contains("teaspoon")) {
            quantity = quantity.replaceAll("teaspoons{0,1}", "tsp");
        }
        if (quantity.contains("tablespoon")) {
            quantity = quantity.replaceAll("tablespoons{0,1}", "tbsp");
        }
        if (quantity.contains("pound")) {
            quantity = quantity.replaceAll("pound", "lb");
        }
        if (quantity.contains("fluid ounce")) {
            quantity = quantity.replaceAll("fluid ounces{0,1}", "fl oz");
        }
        if (quantity.contains("ounce")) {
            quantity = quantity.replaceAll("ounces{0,1}", "oz");
        }

        return quantity;
    }

    /**
     * Prepends the by-line to the name of the author
     * @param context Interface to global context
     * @param author Author of the recipe
     * @return Author of recipe prepended with by-line (e.g. Recipe by hnocturna)
     */
    public static String formatAuthor(Context context, String author) {
        return context.getString(R.string.format_author, author);
    }

    /**
     * Generates a new ID given that there are conflicting IDs that already exist in the database
     * @param context Interface to global Context
     * @param type Type of Id that needs to be generated (Recipe or Ingredient)
     * @return Unused Id for the given type
     */
    public static long generateNewId(Context context, int type) {
        long id = 1;
        switch (type) {
            case RECIPE_TYPE: {
                // Instantiate a new List that will hold all the recipeIds that already exist
                List<Long> recipeIdList = new ArrayList<>();

                // Query the database for recipeIds
                Cursor cursor = context.getContentResolver().query(
                        RecipeEntry.CONTENT_URI,
                        new String[] {RecipeEntry.COLUMN_RECIPE_ID},
                        null,
                        null,
                        RecipeEntry.COLUMN_RECIPE_ID + " DESC"
                );

                // Add all recipeIds to the List // TODO: Replace this explanation
                if (cursor != null && cursor.moveToFirst()) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    int recipesDeleted = prefs.getInt(context.getString(R.string.recipes_deleted_key), 0);
                    id = cursor.getLong(cursor.getColumnIndex(RecipeEntry.COLUMN_RECIPE_ID)) + recipesDeleted + 1;
                }

                // Close the cursor
                if (cursor!= null) cursor.close();

                // Return the new ID
                return id;
            }

            case INGREDIENT_TYPE: {
                // See above for explanation
                List<Long> ingredientIdList = new ArrayList<>();

                Cursor cursor = context.getContentResolver().query(
                        IngredientEntry.CONTENT_URI,
                        new String[] {IngredientEntry.COLUMN_INGREDIENT_ID},
                        null,
                        null,
                        IngredientEntry.COLUMN_INGREDIENT_ID + " DESC"
                );


                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(cursor.getColumnIndex(IngredientEntry.COLUMN_INGREDIENT_ID)) + 1;
                }

                if (cursor!= null) cursor.close();

                return id;
            }

            default: throw new UnsupportedOperationException("Unknown type: " + type);
        }
    }

    /**
     * Queries the database and returns the name of an ingredient given its ingredientId
     * @param context Interface for global context
     * @param ingredientId Id of the ingredient to query the database for a match
     * @return Name of the matching ingredient in String or null if none found
     */
    public static String getIngredientNameFromId(Context context, long ingredientId) {
        String ingredientName = null;
        Cursor cursor = context.getContentResolver().query(
                IngredientEntry.CONTENT_URI,
                null,
                IngredientEntry.COLUMN_INGREDIENT_ID + " = ?",
                new String[] {Long.toString(ingredientId)},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            ingredientName = cursor.getString(cursor.getColumnIndex(IngredientEntry.COLUMN_INGREDIENT_NAME));

        }

        if (cursor != null) cursor.close();

        return ingredientName;
    }

    /**
     * Queries the database and returns the ingredientId of a ingredient given its name
     * @param context Interface for global context
     * @param ingredientName Name of the ingredient to query the database for a match
     * @return long ingredientId or -1 if none found
     */
    public static long getIngredientIdFromName(Context context, String ingredientName) {
        long ingredientId = -1;
        Cursor cursor = context.getContentResolver().query(
                IngredientEntry.CONTENT_URI,
                null,
                IngredientEntry.COLUMN_INGREDIENT_NAME + " = ?",
                new String[] {ingredientName.trim()},
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            ingredientId = cursor.getLong(cursor.getColumnIndex(IngredientEntry.COLUMN_INGREDIENT_ID));
        }

        if (cursor != null) cursor.close();

        return ingredientId;
    }

    /**
     * Converts pixels to display independent pixels for sizing views programmatically
     * @param pixels float value number of pixels
     * @return float value for display independent pixels
     */
    public static float convertPixelsToDp(float pixels) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return pixels / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Converts display independent pixels to pixels for sizing views programmatically
     * @param dips float value for display independent pixels
     * @return float value for pixels
     */
    public static float convertDpToPixels(float dips) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        return dips * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    /**
     * Calculates the percentage of daily allotment for a specific nutrient type
     * @param nutrientType  int value for {@link RecipeEntry.NutrientType}
     * @param nutrientValue double value of a given nutrient in its measurement (g, mg, etc)
     * @return Percentage of total daily allotment
     */
    public static double getDailyValues(Context context, @RecipeEntry.NutrientType int nutrientType, double nutrientValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int dailyCalories = prefs.getInt(
                context.getString(R.string.pref_calorie_key),
                Integer.parseInt(context.getString(R.string.calories_default))
        );

        switch (nutrientType) {
            case RecipeEntry.NUTRIENT_CALORIE: {
                return nutrientValue / dailyCalories;
            }
            case RecipeEntry.NUTRIENT_FAT: {
                double fatPercentage = Double.parseDouble(context.getString(R.string.fat_percent_float_default));
                double calFromFat = dailyCalories * fatPercentage;
                double gramsOfFat = calFromFat / Double.parseDouble(context.getString(R.string.fat_cal_per_gram));
                return nutrientValue / gramsOfFat;
            }
            case RecipeEntry.NUTRIENT_CARB: {
                double carbPercentage = Double.parseDouble(context.getString(R.string.carb_percent_float_default));
                double calFromCarbs = dailyCalories * carbPercentage;
                double gramsOfCarbs = calFromCarbs / Double.parseDouble(context.getString(R.string.carb_cal_per_gram));
                return nutrientValue / gramsOfCarbs;
            }
            case RecipeEntry.NUTRIENT_PROTEIN: {
                double proteinPercentage = Double.parseDouble(context.getString(R.string.protein_percent_float_default));
                double calFromProtein = dailyCalories * proteinPercentage;
                double gramsOfProtein = calFromProtein / Double.parseDouble(context.getString(R.string.protein_cal_per_gram));
                return nutrientValue / gramsOfProtein;
            }
            case RecipeEntry.NUTRIENT_CHOLESTEROL: {
                double mgOfCholesterol = Double.parseDouble(context.getString(R.string.cholesterol_mg_default));
                return nutrientValue / mgOfCholesterol;
            }
            case RecipeEntry.NUTRIENT_SODIUM: {
                double mgOfSodium = Double.parseDouble(context.getString(R.string.sodium_mg_default));
                return nutrientValue / mgOfSodium;
            }
        }
        return 0;
    }

    /**
     * Formats the nutrient value to be displayed by the {@link AdapterNutrition}
     * @param context Interface to global context
     * @param nutrientType int value for {@link RecipeEntry.NutrientType}
     * @param nutrientValue double value for a given nutrient in its measurement
     * @return Formatted String for displaying the value
     */
    public static String formatNutrient(Context context, @RecipeEntry.NutrientType int nutrientType, double nutrientValue) {
        switch (nutrientType) {
            case RecipeEntry.NUTRIENT_CALORIE: {
                return context.getString(R.string.format_nutrient_calories, (int) nutrientValue);
            }
            case RecipeEntry.NUTRIENT_FAT: {
                return context.getString(R.string.format_nutrient_fat, nutrientValue);
            }
            case RecipeEntry.NUTRIENT_CARB: {
                return context.getString(R.string.format_nutrient_carbs, nutrientValue);
            }
            case RecipeEntry.NUTRIENT_PROTEIN: {
                return context.getString(R.string.format_nutrient_protein, nutrientValue);
            }
            case RecipeEntry.NUTRIENT_CHOLESTEROL: {
                return context.getString(R.string.format_nutrient_cholesterol, (int) nutrientValue);
            }
            case RecipeEntry.NUTRIENT_SODIUM: {
                return context.getString(R.string.format_nutrient_sodium, (int) nutrientValue);
            }
        }
        return null;
    }

    /**
     * Matches the {@link RecipeEntry.NutrientType} int to the String equivalent
     * @param context Interface for global context
     * @param nutrientType {@link RecipeEntry.NutrientType} int value
     * @return Plain String for NutrientType equivalent
     */
    public static String getNutrientType(Context context, @RecipeEntry.NutrientType int nutrientType) {
        switch (nutrientType) {
            case RecipeEntry.NUTRIENT_CALORIE: {
                return context.getString(R.string.nutrient_calories_title);
            }
            case RecipeEntry.NUTRIENT_FAT: {
                return context.getString(R.string.nutrient_fat_title);
            }
            case RecipeEntry.NUTRIENT_CARB: {
                return context.getString(R.string.nutrient_carbs_title);
            }
            case RecipeEntry.NUTRIENT_PROTEIN: {
                return context.getString(R.string.nutrient_protein_title);
            }
            case RecipeEntry.NUTRIENT_CHOLESTEROL: {
                return context.getString(R.string.nutrient_cholesterol_title);
            }
            case RecipeEntry.NUTRIENT_SODIUM: {
                return context.getString(R.string.nutrient_sodium_title);
            }
        }

        return null;
    }

    /**
     * Gets the user-set daily calorie count
     * @param context Interface for global context
     * @return Number of calories per day set by the user, otherwise returns standard 2000kCal
     */
    public static int getUserCalories(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.pref_calorie_key),
                        Integer.parseInt(context.getString(R.string.calories_default))
                );
    }

    /**
     * Resolves URL and retrieves recipeId from user input link to recipe
     * @param context Interface to global Context
     * @param recipeUrl String form of link to website containing recipe
     * @return recipeId
     */
    public static String getRecipeSourceIdFromUrl(Context context, String recipeUrl) {
        // Parse the URL into a Uri
        Uri recipeUri = Uri.parse(recipeUrl);

        // Check to make sure scheme has been included in given URL
        String recipeScheme = recipeUri.getScheme();

        if (recipeScheme == null) {
            // Add scheme if missing
            recipeUri = Uri.parse(context.getString(R.string.http_scheme) +
                            "://" +
                            recipeUri.toString());

        }

        // Match the URI and return the recipeId
        UriMatcher matcher = buildUriMatcher(context);
        switch (matcher.match(recipeUri)) {
            case CUSTOM_RECIPE_URI: {
                return getRecipeIdFromCustomUrl(recipeUri.toString());
            }

            case ALLRECIPES_URI: {
                return getRecipeIdFromAllRecipesUrl(recipeUri.toString());
            }

            case FOOD_URI: {
                return getRecipeIdFromFoodUrl(recipeUri.toString());
            }

            default: throw new UnsupportedOperationException("Unknown URL: " + recipeUrl);
        }
    }

    public static long getRecipeIdFromUrl(Context context, String recipeUrl) {
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_URL + " = ?",
                new String[] {recipeUrl},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            long recipeId = cursor.getLong(RecipeEntry.IDX_RECIPE_ID);
            cursor.close();
            return recipeId;
        } else {

            if (cursor != null) {
                cursor.close();
            }

            return -1;
        }


    }

    public static String getRecipeIdFromFoodUrl(String recipeUrl) {
        Pattern pattern = Pattern.compile("[0-9]+$");

        Matcher match = pattern.matcher(recipeUrl);

        if (match.find()) {
            String recipeId = match.group();
            return recipeId;
        }
        return null;
    }

    /**
     * Builds UriMatcher for identifying URLs input from user
     * @return UriMatcher
     */
    public static UriMatcher buildUriMatcher(Context context) {
        // Root URI
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Add URIs that need to be matched
        uriMatcher.addURI(context.getString(R.string.custom_authority) , "/*", CUSTOM_RECIPE_URI);
        uriMatcher.addURI(context.getString(R.string.allrecipes_authority), "/recipe/#/*", ALLRECIPES_URI);
        uriMatcher.addURI(context.getString(R.string.allrecipes_www_authority), "/recipe/#/*", ALLRECIPES_URI);
        uriMatcher.addURI(context.getString(R.string.allrecipes_authority), "/recipe/#", ALLRECIPES_URI);
        uriMatcher.addURI(context.getString(R.string.allrecipes_www_authority), "/recipe/#", ALLRECIPES_URI);
        uriMatcher.addURI(context.getString(R.string.food_authority), "/recipe/*", FOOD_URI);
        uriMatcher.addURI(context.getString(R.string.food_www_authority), "/recipe/*", FOOD_URI);
        uriMatcher.addURI(context.getString(R.string.seriouseats_authority), "/recipes/#/#/*", SERIOUSEATS_URI);
        uriMatcher.addURI(context.getString(R.string.seriouseats_www_authority), "/recipes/#/#/*", SERIOUSEATS_URI);
        uriMatcher.addURI(context.getString(R.string.epicurious_authority), "/recipes/food/views/*", EPICURIOUS_URI);
        uriMatcher.addURI(context.getString(R.string.epicurious_www_authority), "/recipes/food/views/*", EPICURIOUS_URI);

        // Return UriMatcher
        return uriMatcher;
    }

    /**
     * Retrieves the recipe's URL from the database given the recipeId
     * @param context Interface to global Context
     * @param recipeId
     * @return URL in String form for the recipe
     */
    public static String getRecipeUrlFromRecipeId(Context context, long recipeId) {
        // Query the database and filter to the recipeId
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(recipeId)},
                null
        );

        // Instantiate the String for the recipe URL
        String recipeUrl = null;

        // Check to make sure Cursor exists and it returned a row
        if (cursor != null && cursor.moveToFirst()) {
            // Get the recipe's URL from the database
            recipeUrl = cursor.getString(RecipeEntry.IDX_RECIPE_URL);
        }

        // Close the Cursor
        if (cursor != null) cursor.close();

        return recipeUrl;
    }

    /**
     * Retrieves the recipe's ID from the URL for user-made/edited recipes
     * @param recipeUrl String form of the recipe's URL
     * @return Long recipeId
     */
    private static String getRecipeIdFromCustomUrl(String recipeUrl) {
        Uri recipeUri = Uri.parse(recipeUrl);
        return recipeUri.getPathSegments().get(0);
    }

    /**
     * Saves a user-selected Bitmap image to file so it can be used for the recipe's image
     * @param context Interface to global Context
     * @param recipeId ID of the recipe to link the image to
     * @param bitmap Image supplied by the user
     * @return URI for the image location on file
     */
    public static Uri saveImageToFile(Context context, String recipeId, Bitmap bitmap) {
        // A copy of the photo will be saved in the app's private directory
        File directory = context.getDir(
                context.getString(R.string.food_image_dir),
                Context.MODE_PRIVATE
        );

        // If the directory doesn't already exist, create it
        if (!directory.exists()) {
            directory.mkdir();
        }

        // Generate the filepath to save the file
        File imagePath = new File(directory, recipeId + ".jpg");
        if (imagePath.exists()) {
            System.out.println(imagePath.delete());
        }

        // Initialize the FileOutputStream that will save the photo
        FileOutputStream fileOutputStream;

        // URI of the photo's location after it has been saved
        Uri imageUri = null;

        // Resize the image if it is too large
        if (bitmap.getWidth() > 4096 || bitmap.getHeight() > 4096) {
            // Check which dimension is larger
            double tooLarge = bitmap.getWidth() > bitmap.getHeight() ?
                    bitmap.getWidth() : bitmap.getHeight();

            // Scale it so the largest size is no larger than 2048px
            int scaleFactor = (int) Math.ceil(tooLarge / 2048);
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / scaleFactor, bitmap.getHeight() / scaleFactor, false);
        }

        // Save the new photo to file
        try {
            fileOutputStream = new FileOutputStream(imagePath);

            // Compress the image to save space
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.close();

            // Retrieve the URI of the saved image to pass to the Activity
            imageUri = Uri.fromFile(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUri;
    }

    /**
     * Delete the image from the directory
     * @param context Interface to global Context
     * @param imagePathUri URI path for the image file
     * @return Boolean value for whether the image was successfully deleted
     */
    public static boolean deleteImageFromFile(Context context, Uri imagePathUri) {
        // Get the File from the URI
        File imageFile = new File(imagePathUri.getPath());

        // Check whether the File exists
        if (imageFile.exists()) {
            // Delete the image file
            return imageFile.delete();
        } else {
            return false;
        }
    }

    /**
     * Method for standardizing the time that is added to the database
     * @return
     */
    public static long getCurrentTime() {
        GregorianCalendar gc = new GregorianCalendar();
        return gc.getTimeInMillis();
    }

    /**
     * Checks to make sure there are no duplicate ingredients in the database and then bulk-inserts
     * values into database
     * @param mContext Interface to global Context
     * @param ingredientCVList List of ingredient ContentValues to be inserted to database
     */
    public static void insertAndUpdateIngredientValues(Context mContext, List<ContentValues> ingredientCVList) {
        // Duplicate the list as to avoid ConcurrentModificationError
        List<ContentValues> workingList = new LinkedList<>(ingredientCVList);

        // Create an ArrayList for update operations
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        String selection = IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID + " = ? OR " +
                IngredientEntry.COLUMN_FOOD_INGREDIENT_ID + " = ?";

        // Iterate through the ContentValues to check whether they need to be added or updated
        for (ContentValues ingredientValue : workingList) {
            // Retrieves parameters to insert into database so they can be checked if they already
            // exist
            long ingredientId = -1;
            long allrecipesId = -1;
            long foodId = -1;

            if (ingredientValue.containsKey(IngredientEntry.COLUMN_INGREDIENT_ID)) {
                // If ContentValues contains ingredientId value, remove it because it cannot be
                // inserted. It is only used to ensure parity with the value inserted in the
                // ingredient link table
                ingredientId = ingredientValue.getAsLong(IngredientEntry.COLUMN_INGREDIENT_ID);
                ingredientValue.remove(IngredientEntry.COLUMN_INGREDIENT_ID);
            }

            if (ingredientValue.containsKey(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID)) {
                allrecipesId = ingredientValue.getAsLong(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID);
            }

            if (ingredientValue.containsKey(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID)) {
                foodId = ingredientValue.getAsLong(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID);
            }

            String ingredientName = ingredientValue.getAsString(IngredientEntry.COLUMN_INGREDIENT_NAME);

            // Query the database to check whether it already contains the ingredient
            long databaseIngredientId = getIngredientIdFromName(mContext, ingredientName);

            if (databaseIngredientId != -1 && ingredientId != databaseIngredientId) {
                // If database contains ingredient, check to ensure the value being inserted into
                // the link table matches the value in the ingredients table
                Log.d(LOG_TAG, "Mismatched ingredientId while attempting to insert ingredient!");
                Log.d(LOG_TAG, "Expected value for " + ingredientName + ": " + ingredientId +
                        " | Read value: " + databaseIngredientId);
                return;
            }

            if (databaseIngredientId != -1 && (allrecipesId != -1 || foodId != -1)) {
                // If database contains ingredient and it passes the check, remove it from the list
                // to be bulk-inserted into the database
                ingredientCVList.remove(ingredientValue);

                // Create parameters for updating the database
                String updateSelection = IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";
                String[] updateArgs = new String[] {Long.toString(ingredientId)};
                String column = null;
                long value = -1;

                if (allrecipesId != -1) {
                    column = IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID;
                    value = allrecipesId;
                }
                if (foodId != -1) {
                    column = IngredientEntry.COLUMN_FOOD_INGREDIENT_ID;
                    value = foodId;
                }

                // Create the update operation
                ContentProviderOperation operation = ContentProviderOperation
                        .newUpdate(IngredientEntry.CONTENT_URI)
                        .withSelection(updateSelection, updateArgs)
                        .withValue(column, value)
                        .build();

                // Add to the update list
                operations.add(operation);
            }

//            // Check if ingredient is already in the database, if so, skip it
//            long allrecipesId = -1;
//            long foodId = -1;
//
//            if (ingredientValue.containsKey(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID)) {
//                allrecipesId = ingredientValue.getAsLong(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID);
//            }
//            if (ingredientValue.containsKey(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID)) {
//                foodId = ingredientValue.getAsLong(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID);
//            }
//
//            // If not being added from an online source, then the ingredient is sure be new
//            if (allrecipesId == -1 && foodId == -1) continue;
//
//            // Selection Argument
//            String[] selectionArgs = new String[] {Long.toString(allrecipesId), Long.toString(foodId)};
//
//            Cursor cursor = mContext.getContentResolver().query(
//                    IngredientEntry.CONTENT_URI,
//                    null,
//                    selection,
//                    selectionArgs,
//                    null
//            );
//
//            if (cursor != null && cursor.moveToFirst()) {
//                // Remove the ContentValues from the list to be bulk inserted
//                ingredientCVList.remove(ingredientValue);
//            }
//            // Close the Cursor
//            if (cursor != null) cursor.close();
        }

        // Update existing values
        try {
            mContext.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, operations);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        // Create an Array of Content Values to be bulk inserted from what remains in the
        // ingredientCVList
        ContentValues[] ingredientValues = new ContentValues[ingredientCVList.size()];
        ingredientCVList.toArray(ingredientValues);

        // Bulk insert values into database
        mContext.getContentResolver().bulkInsert(
                IngredientEntry.CONTENT_URI,
                ingredientValues
        );
    }

    /**
     * Takes a list of ContentValues for the Link Table and inserts/updates them into the database
     * depending on whether an entry already exists
     * @param context Interface to global Context
     * @param linkCVList List of ContentValues to be inserted/updated into the Link Table
     */
    public static Pair<Integer, Integer> insertAndUpdateLinkValues(Context context, List<ContentValues> linkCVList) {
        boolean bypass = true;
        if (bypass) {
            ContentValues values = linkCVList.get(0);
            // Selection for querying the Link Table
            String selection =  RecipeEntry.COLUMN_RECIPE_ID + " = ?";

            String[] selectionArgs = new String[] {
                    values.getAsString(RecipeEntry.COLUMN_RECIPE_ID),
            };

            int deleted = context.getContentResolver().delete(
                    LinkIngredientEntry.CONTENT_URI,
                    selection,
                    selectionArgs
            );

            ContentValues[] linkInsertionValues = new ContentValues[linkCVList.size()];
            linkCVList.toArray(linkInsertionValues);

            int inserted = context.getContentResolver().bulkInsert(
                    LinkIngredientEntry.CONTENT_URI,
                    linkInsertionValues
            );

            return new Pair<>(deleted, inserted);
        }
        // Create a new List and copy the contents of the parameter List into it
        List<ContentValues> workingList = new LinkedList<>();
        workingList.addAll(linkCVList);

        // Instantiate the ArrayList to be used for bulk-insertion
        ArrayList<ContentProviderOperation> updateList = new ArrayList<>();

        // Selection for querying the Link Table
        String selection = RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID + " = ? AND " +
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_SOURCE + " = ?";

        // Selection for updating the Link Table
        String updateSelection = RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                IngredientEntry.COLUMN_INGREDIENT_ID + " = ? AND " +
                RecipeEntry.COLUMN_SOURCE + " = ?";

        // Iterate through the List of ContentValues and query the database to see if an equivalent
        // entry already exists
        for (ContentValues linkValue : workingList) {
            // Create the selection arguments for filtering the database from the ContentValues
            String[] selectionArgs = new String[]{
                    linkValue.getAsString(RecipeEntry.COLUMN_RECIPE_ID),
                    linkValue.getAsString(IngredientEntry.COLUMN_INGREDIENT_ID),
                    linkValue.getAsString(RecipeEntry.COLUMN_SOURCE)
            };

            // Query the database
            Cursor cursor = context.getContentResolver().query(
                    LinkIngredientEntry.CONTENT_URI,
                    null,
                    selection,
                    selectionArgs,
                    null
            );

            if (cursor != null && cursor.moveToFirst()) {
                // If an entry exists, then close the Cursor
                cursor.close();

                // Add the ContentValues to the ArrayList to be used for bulk updates
                updateList.add(ContentProviderOperation.newUpdate(LinkIngredientEntry.CONTENT_URI)
                        .withSelection(updateSelection, selectionArgs)
                        .withValue(LinkIngredientEntry.COLUMN_QUANTITY, linkValue.getAsString(LinkIngredientEntry.COLUMN_QUANTITY))
                        .withValue(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, linkValue.getAsLong(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER))
                        .build()
                );

                // Remove the entry from the list to be bulk inserted
                linkCVList.remove(linkValue);
            }
        }
        int valuesAdded, valuesUpdated;

        // Create an Array of ContentValues that need to be inserted
        if ((valuesAdded = linkCVList.size()) > 0) {
            ContentValues[] linkValues = new ContentValues[linkCVList.size()];
            for (int i = 0; i < linkCVList.size(); i++) {
                linkValues[i] = linkCVList.get(i);
            }

            // Bulk insert values into the database
            context.getContentResolver().bulkInsert(
                    LinkIngredientEntry.CONTENT_URI,
                    linkValues
            );
        }

        valuesUpdated = 0;
        try {
            // Batch update values from the updateList
            valuesUpdated = updateList.size();
            context.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, updateList);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Batch update failed!");
        }

        return new Pair<>(valuesAdded, valuesUpdated);
    }

    /**
     * Checks to make sure the recipe doesn't already exist in the database prior to bulk-insertion
     * @param context Interface for global Context
     * @param recipeCVList List containing recipes to be bulk-inserted
     */
    public static void insertAndUpdateRecipeValues(Context context, List<ContentValues> recipeCVList) {
        /** Variables **/
        List<ContentValues> workingList = new ArrayList<>(recipeCVList);    // To prevent ConcurrentModificationError

        /** Constants **/
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        for (ContentValues recipeValue : workingList) {
            String recipeSourceId = recipeValue.getAsString(RecipeEntry.COLUMN_RECIPE_SOURCE_ID);
            // Check if recipe exists in database

            Cursor cursor = context.getContentResolver().query(
                    recipeUri,
                    RecipeEntry.RECIPE_PROJECTION,
                    RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ?",
                    new String[] {recipeSourceId},
                    null
            );

            // Update appropriate recipes
            if (cursor != null && cursor.moveToFirst()) {
                // Remove the recipeValues from the list to be bulk-inserted
                recipeCVList.remove(recipeValue);

                // If cursor returns a row, then update the values if they have changed
                // Get the review and rating values from the ContentValues to be updated
                double dbRating = cursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);
                long dbReviews = cursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);

                if (!recipeValue.containsKey(RecipeEntry.COLUMN_REVIEWS)) {
                    // Close the cursor
                    if (cursor != null) cursor.close();

                    continue;
                }

                if (recipeValue.getAsDouble(RecipeEntry.COLUMN_RATING) != dbRating ||
                        recipeValue.getAsLong(RecipeEntry.COLUMN_REVIEWS) != dbReviews) {
                    // Values do match database values. Update database.
                    // Remove date from ContentValues so that the update doesn't push the recipe to
                    // the top
                    recipeValue.remove(RecipeEntry.COLUMN_DATE_ADDED);

                    context.getContentResolver().update(
                            recipeUri,
                            recipeValue,
                            RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ?",
                            new String[]{recipeSourceId}
                    );
                }
            }

            // Close the cursor
            if (cursor != null) cursor.close();
        }


        // Create a ContentValues[] from the remaining values in the list
        ContentValues[] recipeValues = new ContentValues[recipeCVList.size()];

        // Add values of list to the array
        recipeCVList.toArray(recipeValues);

        // Bulk insert all remaining recipes
        context.getContentResolver().bulkInsert(recipeUri, recipeValues);
    }

    /**
     * Retrieves the recipe ID from the database given the recipe source's ID and the recipe source
     * @param context Interface to global Context
     * @param recipeSourceId The recipe ID given by the source where the recipe was imported from
     * @param recipeSource The source where the recipe was imported from
     * @return Internal ID from the database for the recipe
     */
    public static long getRecipeIdFromSourceId(Context context, String recipeSourceId, String recipeSource) {
        // Query the database and filter for entries with the recipe source and recipe source ID
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                null,
                RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " = ? AND " + RecipeEntry.COLUMN_SOURCE + " = ?",
                new String[] {recipeSourceId, recipeSource},
                null
        );

        // Initialize the variable to hold the recipe ID
        long recipeId = -1;

        if (cursor != null && cursor.moveToFirst()) {
            // If entry is found, set the recipe ID
            recipeId = cursor.getLong(RecipeEntry.IDX_RECIPE_ID);

            // Close the Cursor
            cursor.close();
        }

        if (cursor != null) {
            cursor.close();
        }

        // Return either recipe ID if found or -1 if none found
        return recipeId;
    }

    public static class IngredientHelper {
        Context mContext;
        Map<Long, String> ingredientIdNameMap;

        @SuppressLint("UseSparseArrays")
        public IngredientHelper(Context context) {
            mContext = context;
            ingredientIdNameMap = new HashMap<>();
        }

        public Pair<Boolean, Long> addIngredient(String ingredient) {
            // Check to see if ingredient already exists in database
            long ingredientId = getIngredientIdFromName(mContext, ingredient);
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

            // Final check to see if ingredient already exists in ingredientIdNameMap

            String ingredientMapName = ingredientIdNameMap.get(ingredientId);

            if (ingredient.equals(ingredientMapName)) {
                // If it exists, there is no need to add a duplicate to the ingredient table
                skipAddIngredient = true;
            }

            // Add the ingredient ID to ingredientIdNameMap to keep track of which IDs have
            // already been used
            ingredientIdNameMap.put(ingredientId, ingredient);

            // Return the ingredient ID of the ingredient that was just added
            return new Pair<>(skipAddIngredient, ingredientId);
        }
    }

    /**
     * Retrieves the time the database was last synced from SharedPreferences
     * @param context Interface to global Context
     * @return long time in milliseconds when the database was last synced
     */
    public static long getLastSyncTime(Context context) {
        // Get an instance of SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Return the time the database was last successfully synced
        return prefs.getLong(context.getString(R.string.pref_last_sync), 0);
    }

    /**
     * Sets the last sync variable in SharedPreferences to the curren time
     * @param context Interface to global Context
     */
    public static void updateLastSynced(Context context) {
        // Get the time that the sync finished
        long timeInMillis = getCurrentTime();

        // Set the last sync time to seedTime
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong(context.getString(R.string.pref_last_sync), timeInMillis);

        // Apply the changes
        editor.apply();
    }

    public static String getRecipeSourceIdFromUri(Context context, Uri recipeUri) {
        // Retrieve the recipe ID from the URI
        long recipeId = RecipeEntry.getRecipeIdFromUri(recipeUri);

        // Query the databse to find the entry with the recipe ID
        Cursor cursor = context.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(recipeId)},
                null
        );

        // Initialize the recipeSourceId to be returned if it exists
        String recipeSourceId = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Retrieve the recipeSourceId
                recipeSourceId = cursor.getString(RecipeEntry.IDX_RECIPE_SOURCE_ID);
            }

            // Close the Cursor
            cursor.close();
        }

        return recipeSourceId;
    }
}
