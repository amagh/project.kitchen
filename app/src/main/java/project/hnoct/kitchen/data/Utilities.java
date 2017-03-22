package project.hnoct.kitchen.data;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
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

    public static final int CUSTOM_RECIPE_URI = 0;
    public static final int ALLRECIPES_URI = 1;

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

                // Return the Strings as a Pair
                return new Pair<>(ingredient, ingredientQuantity);
            }
        }
        // If the input String does not contain any known measurements, then use Regex to
        // obtain the quantity of the ingredient
        Pattern pattern = Pattern.compile("([0-9]* *[1-9]*\\/*[1-9]*) (.*)");
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
    public static long getRecipeIdFromAllRecipesUrl(String recipeUrl) {
        // Check to ensure that the URL is prepended with "http://"
        if (!recipeUrl.contains("http://") && !recipeUrl.substring(0, 7).equals("http://")) {
            // Correct the URL
            recipeUrl = "http://" + recipeUrl;
        }
        // Convert URL to URI
        Uri recipeLinkUri = Uri.parse(recipeUrl);

        // Return 2nd segment of the URI as the recipeId
        return Long.parseLong(recipeLinkUri.getPathSegments().get(1));
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
    public static String generateAllRecipesUrlFromRecipeId(long recipeId) {
        final String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
        Uri recipeUri = Uri.parse(ALL_RECIPES_BASE_URL).buildUpon()
                .appendPath("recipe")
                .appendPath(Long.toString(recipeId))
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
        cursor.moveToFirst();

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
            /** See above **/
            favoriteValue.put(RecipeEntry.COLUMN_FAVORITE, 1);
            context.getContentResolver().update(
                    RecipeEntry.CONTENT_URI,
                    favoriteValue,
                    selection,
                    selectionArgs
            );
        }

        // Close the cursor so the new status can be written to the database
        cursor.close();

        // Return the new favorite value
        return !favorite;
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
     * @param type
     * @return
     */
    public static long generateNewId(Context context, int type) {
        long id = -1;
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

                // Add all recipeIds to the List
                if (cursor != null && cursor.moveToFirst()) {
                    id = cursor.getLong(cursor.getColumnIndex(RecipeEntry.COLUMN_RECIPE_ID)) + 1;
                }

                // Close the cursor
                cursor.close();

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

                cursor.close();
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
        cursor.close();
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
        if (cursor.moveToFirst()) {
            ingredientId = cursor.getLong(cursor.getColumnIndex(IngredientEntry.COLUMN_INGREDIENT_ID));
        }
        cursor.close();
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
                double calorieDV = nutrientValue / dailyCalories;
                return calorieDV;
            }
            case RecipeEntry.NUTRIENT_FAT: {
                double fatPercentage = Double.parseDouble(context.getString(R.string.fat_percent_float_default));
                double calFromFat = dailyCalories * fatPercentage;
                double gramsOfFat = calFromFat / Double.parseDouble(context.getString(R.string.fat_cal_per_gram));
                double fatDV = nutrientValue / gramsOfFat;
                return fatDV;
            }
            case RecipeEntry.NUTRIENT_CARB: {
                double carbPercentage = Double.parseDouble(context.getString(R.string.carb_percent_float_default));
                double calFromCarbs = dailyCalories * carbPercentage;
                double gramsOfCarbs = calFromCarbs / Double.parseDouble(context.getString(R.string.carb_cal_per_gram));
                double carbsDV = nutrientValue / gramsOfCarbs;
                return carbsDV;
            }
            case RecipeEntry.NUTRIENT_PROTEIN: {
                double proteinPercentage = Double.parseDouble(context.getString(R.string.protein_percent_float_default));
                double calFromProtein = dailyCalories * proteinPercentage;
                double gramsOfProtein = calFromProtein / Double.parseDouble(context.getString(R.string.protein_cal_per_gram));
                double proteinDV = nutrientValue / gramsOfProtein;
                return proteinDV;
            }
            case RecipeEntry.NUTRIENT_CHOLESTEROL: {
                double mgOfCholesterol = Double.parseDouble(context.getString(R.string.cholesterol_mg_default));
                double cholesterolDV = nutrientValue / mgOfCholesterol;
                return cholesterolDV;
            }
            case RecipeEntry.NUTRIENT_SODIUM: {
                double mgOfSodium = Double.parseDouble(context.getString(R.string.sodium_mg_default));
                double sodiumDV = nutrientValue / mgOfSodium;
                return sodiumDV;
            }
        }
        return 0;
    }

    /**
     * Formats the nutrient value to be displayed by the {@link project.hnoct.kitchen.ui.NutritionAdapter}
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
    public static long getRecipeIdFromUrl(Context context, String recipeUrl) {
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
            default: throw new UnsupportedOperationException("Unknown URL: " + recipeUrl);
        }
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
    private static long getRecipeIdFromCustomUrl(String recipeUrl) {
        Uri recipeUri = Uri.parse(recipeUrl);
        return Long.parseLong(recipeUri.getPathSegments().get(0));
    }

    /**
     * Saves a user-selected Bitmap image to file so it can be used for the recipe's image
     * @param context Interface to global Context
     * @param recipeId ID of the recipe to link the image to
     * @param bitmap Image supplied by the user
     * @return URI for the image location on file
     */
    public static Uri saveImageToFile(Context context, long recipeId, Bitmap bitmap) {
        File directory = context.getDir(
                context.getString(R.string.food_image_dir),
                Context.MODE_PRIVATE
        );

        if (!directory.exists()) {
            directory.mkdir();
        }

        File imagePath = new File(directory, recipeId + ".jpg");
        if (imagePath.exists()) {
            System.out.println(imagePath.delete());
        }

        FileOutputStream fileOutputStream;

        Uri imageUri = null;

        try {
            fileOutputStream = new FileOutputStream(imagePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);
            fileOutputStream.close();
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
    public static void insertIngredientValues(Context mContext, List<ContentValues> ingredientCVList) {
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
    public static void insertAndUpdateLinkValues(Context context, List<ContentValues> linkCVList) {
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

        // Iterate through the List of ContentValues and query the databse to see if an equivalent
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

        // Create an Array of ContentValues that need to be inserted
        ContentValues[] linkValues = new ContentValues[linkCVList.size()];
        for (int i = 0; i < linkCVList.size(); i++) {
            linkValues[i] = linkCVList.get(i);
        }

        // Bulk insert values into the database
        context.getContentResolver().bulkInsert(
                LinkIngredientEntry.CONTENT_URI,
                linkValues
        );

        try {
            // Batch update values from the updateList
            context.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, updateList);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
            Log.d(LOG_TAG, "Batch update failed!");
        }
    }
}
