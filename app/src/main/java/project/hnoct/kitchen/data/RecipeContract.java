package project.hnoct.kitchen.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.GregorianCalendar;

/**
 * Created by hnoct on 2/16/2017.
 */

public class RecipeContract {
    // Constants
    /** For accessing the database */
    public static final String CONTENT_AUTHORITY = "project.hnoct.kitchen";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_RECIPE = "recipe";
    public static final String PATH_INGREDIENT = "ingredient";
    public static final String PATH_LINK = "link";

    /**
     * Method for standardizing the time that is added to the database
     * @return
     */
    public static long getCurrentTime() {
        GregorianCalendar gc = new GregorianCalendar();
        return gc.getTimeInMillis();
    }

    /**
     * Entry for the Recipe Table
     */
    public static class RecipeEntry implements BaseColumns {
        // URI for recipe data
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_RECIPE).build();

        // For selecting each individual recipe
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RECIPE;

        // For selecting recipes by category
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_RECIPE;

        // Table name
        public static final String TABLE_NAME = "recipes";

        // Columns
        public static final String COLUMN_RECIPE_ID = "recipe_id";              // REAL NOT NULL
        public static final String COLUMN_RECIPE_NAME = "recipe_name";          // TEXT NOT NULL
        public static final String COLUMN_THUMBNAIL_URL = "thumbnail_url";      // TEXT
        public static final String COLUMN_IMG_URL = "image_url";                // TEXT
        public static final String COLUMN_RECIPE_URL = "recipe_url";            // TEXT NOT NULL
        public static final String COLUMN_SHORT_DESC = "short_description";     // TEXT
        public static final String COLUMN_RATING = "rating";                    // REAL
        public static final String COLUMN_REVIEWS = "reviews";                  // REAL
        public static final String COLUMN_DIRECTIONS = "directions";            // TEXT NOT NULL
        public static final String COLUMN_DATE_ADDED = "date_added";            // REAL NOT NULL

        /**
         * Builds URI for a specific row in the database
         * @param id Id returned by a database insertion
         * @return Uri URI of the row in the database
         */
        public static Uri buildRecipeUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        /**
         * Builds URI using the recipeId
         * @param id recipe Id
         * @return URI of the specific row containing the recipeId
         */
        public static Uri buildRecipeUriFromId(long id) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(id))
                    .build();
        }

        /**
         * Retrieves the recipeId from a URI
         * @param uri Uri containing recipeId
         * @return recipeId
         */
        public static long getRecipeIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    /**
     * Entry for the Ingredients Table
     */
    public static class IngredientEntry implements BaseColumns {
        // URI for ingredient data
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_INGREDIENT).build();

        // For selecting/inserting single rows in the database
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INGREDIENT;

        // For selecting multiple rows from the database
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INGREDIENT;

        // Table name
        public static final String TABLE_NAME = "ingredients";

        // Columns
        public static final String COLUMN_INGREDIENT_ID = "ingredient_id";  // REAL NOT NULL
        public static final String COLUMN_INGREDIENT_NAME = "ingredient";   // TEXT NOT NULL

        /** See RecipeEntry for comments on following methods **/

        public static Uri buildIngredientUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildUriFromIngredientId(long ingredientId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(ingredientId))
                    .build();
        }

        public static long getIngredientIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    /**
     * Entry for the Link Table
     * The link table functions to relate the quantity of each ingredient with the recipe because
     * databases are not set up for lists.
     */
    public static class LinkEntry implements BaseColumns {
        // URI for linking the two tables with their quantities
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LINK).build();

        // Since this table will not be directly selected for, only the item type is used for
        // insertions
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_LINK;

        // Table name
        public static final String TABLE_NAME = "link";

        // Columns (Only the quantity column is required because all other columns will be
        // foreign keys)
        public static final String COLUMN_QUANTITY = "quantity";    // TEXT NOT NULL

        /** See RecipeEntry for comments on following methods **/

        public static Uri buildLinkUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildRecipeUriFromIngredientId(long ingredientId) {
            return CONTENT_URI.buildUpon()
                    .appendPath("ingredients")
                    .appendQueryParameter(IngredientEntry.COLUMN_INGREDIENT_ID, Long.toString(ingredientId))
                    .build();
        }

        public static Uri buildIngredientUriFromRecipe(long recipeId) {
            return CONTENT_URI.buildUpon()
                    .appendPath("recipes")
                    .appendQueryParameter(RecipeEntry.COLUMN_RECIPE_ID, Long.toString(recipeId))
                    .build();
        }

        public static long getRecipeIdFromUri(Uri uri) {
            String recipeString  = uri.getQueryParameter(RecipeEntry.COLUMN_RECIPE_ID);
            return (recipeString != null && recipeString.length() > 0) ? Long.parseLong(recipeString) : -1;
        }

        public static long getIngredientIdFromUri(Uri uri) {
            String ingredientString = uri.getQueryParameter(IngredientEntry.COLUMN_INGREDIENT_ID);
            return (ingredientString != null && ingredientString.length() > 0) ? Long.parseLong(ingredientString) : -1;
        }
    }
}
