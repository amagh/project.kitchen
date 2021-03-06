package project.kitchen.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Created by Alvin on 2/16/2017.
 */

public class RecipeContract {
    /** Constants **/
    // For accessing the database
    public static final String CONTENT_AUTHORITY = "project.kitchen";
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final String PATH_RECIPE = "recipe";
    public static final String PATH_INGREDIENT = "ingredient";
    public static final String PATH_INGREDIENT_LINK = "ingredient_link";
    public static final String PATH_BOOK = "recipe_book";
    public static final String PATH_CHAPTER = "chapter";
    public static final String PATH_BOOK_LINK = "recipe_book_link";

    // For searching database
    public static final String INGREDIENT_SEARCH_KEY = "ingredient_search";
    public static final String RECIPE_FAVORITES_SEARCH_KEY = "recipe_favorites_search";
    public static final String SEARCH_INGREDIENT = "searchIngredient";
    public static final String SEARCH_FAVORITES = "searchFavorites";

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
        public static final String COLUMN_RECIPE_ID = "recipe_id";                  // REAL NOT NULL
        public static final String COLUMN_RECIPE_SOURCE_ID = "recipe_source_id";
        public static final String COLUMN_RECIPE_NAME = "recipe_name";              // TEXT NOT NULL
        public static final String COLUMN_RECIPE_AUTHOR = "author";                 // TEXT NOT NULL
        public static final String COLUMN_IMG_URL = "image_url";                    // TEXT
        public static final String COLUMN_RECIPE_URL = "recipe_url";                // TEXT NOT NULL
        public static final String COLUMN_SHORT_DESC = "short_description";         // TEXT
        public static final String COLUMN_RATING = "rating";                        // REAL
        public static final String COLUMN_REVIEWS = "reviews";                      // REAL
        public static final String COLUMN_DIRECTIONS = "directions";                // TEXT
        public static final String COLUMN_DATE_ADDED = "date_added";                // REAL NOT NULL
        public static final String COLUMN_FAVORITE = "favorite";                    // INTEGER NOT NULL
        public static final String COLUMN_SOURCE = "source";                        // TEXT NOT NULL
        // Nutrition Info
        public static final String COLUMN_CALORIES = "calories";                    // REAL
        public static final String COLUMN_FAT = "fat";                              // REAL
        public static final String COLUMN_CARBS = "carbs";                          // REAL
        public static final String COLUMN_PROTEIN = "protein";                      // REAL
        public static final String COLUMN_CHOLESTEROL = "cholesterol";              // REAL
        public static final String COLUMN_SODIUM = "sodium";                        // REAL
        public static final String COLUMN_SERVINGS = "servings";                    // INTEGER

        // Column Projection and index
        public static final String[] RECIPE_PROJECTION = new String[] {
                TABLE_NAME + "." + COLUMN_RECIPE_ID,
                COLUMN_RECIPE_SOURCE_ID,
                COLUMN_RECIPE_NAME,
                COLUMN_RECIPE_AUTHOR,
                COLUMN_IMG_URL,
                COLUMN_RECIPE_URL,
                COLUMN_SHORT_DESC,
                COLUMN_RATING,
                COLUMN_REVIEWS,
                COLUMN_DIRECTIONS,
                COLUMN_DATE_ADDED,
                COLUMN_FAVORITE,
                COLUMN_SOURCE,
                COLUMN_CALORIES,
                COLUMN_FAT,
                COLUMN_CARBS,
                COLUMN_PROTEIN,
                COLUMN_CHOLESTEROL,
                COLUMN_SODIUM,
                COLUMN_SERVINGS
        };

        public static final int IDX_RECIPE_ID = 0;
        public static final int IDX_RECIPE_SOURCE_ID = 1;
        public static final int IDX_RECIPE_NAME = 2;
        public static final int IDX_RECIPE_AUTHOR = 3;
        public static final int IDX_IMG_URL = 4;
        public static final int IDX_RECIPE_URL = 5;
        public static final int IDX_SHORT_DESCRIPTION = 6;
        public static final int IDX_RECIPE_RATING = 7;
        public static final int IDX_RECIPE_REVIEWS = 8;
        public static final int IDX_RECIPE_DIRECTIONS = 9;
        public static final int IDX_DATE_ADDED = 10;
        public static final int IDX_FAVORITE = 11;
        public static final int IDX_RECIPE_SOURCE = 12;
        public static final int IDX_CALORIES = 13;
        public static final int IDX_FAT = 14;
        public static final int IDX_CARBS = 15;
        public static final int IDX_PROTEIN = 16;
        public static final int IDX_CHOLESTEROL = 17;
        public static final int IDX_SODIUM = 18;
        public static final int IDX_SERVINGS = 19;

        /** Integer definition for nutrient types **/
        @Retention(SOURCE)
        @IntDef({NUTRIENT_CALORIE, NUTRIENT_FAT, NUTRIENT_CARB, NUTRIENT_PROTEIN, NUTRIENT_CHOLESTEROL, NUTRIENT_SODIUM})
        public @interface NutrientType{}
        public static final int NUTRIENT_CALORIE = 0;
        public static final int NUTRIENT_FAT = 1;
        public static final int NUTRIENT_CARB = 2;
        public static final int NUTRIENT_PROTEIN = 3;
        public static final int NUTRIENT_CHOLESTEROL = 4;
        public static final int NUTRIENT_SODIUM = 5;

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
        public static final String COLUMN_INGREDIENT_ID = "ingredient_id";              // INTEGER PRIMARY KEY AUTOINCREMENT
        public static final String COLUMN_INGREDIENT_NAME = "ingredient";               // TEXT NOT NULL
        public static final String COLUMN_ALLRECIPES_INGREDIENT_ID = "allrecipes_id";   // REAL
        public static final String COLUMN_FOOD_INGREDIENT_ID = "food_id";               // REAL

        public static final String[] measurements = new String[] {
                "[Ll]oaf", "[Cc]an", "[Pp]ackage", "[Cc]ontainer", "[Tt]ablespoon", "[Tt]easpoon",
                "[Cc]love", "[Cc]up", "[Pp]int", "[Qq]uart", "[Gg]allon", "[Oo]unce",
                "[Ff]luid [Oo]unce", "[Jj]ar", "[Dd]ashes", "[Dd]ash", "[Pp]inch", "[Ss]lice",
                "[Pp]ound", "[Dd]rop", "lb", "[Bb]unch", "[Pp]ack", "tsp", "tbsp", "g", "ml", "oz"
        };

        public static final String[] preparations = new String[] {
                "[Cc]hopped", "[Dd]iced", "[Mm]inced", "[Ss]eparated", "[Pp]eeled", "[Dd]eveined",
                "[Jj]uiced", "[Cc]ut", "[Ww]ith", "[Ww]ithout", "[Dd]ivided", "[Ss]helled",
                "[Tt]rimmed", "[Ss]liced", "[Ww]hipped", "[G]grated", "[Ss]plit", "[Ss]oftened",
                "[Cc]rushed", "[Mm]elted", "°", "[Qq]uartered", "[Cc]ored", "[Bb]eaten",
                "[Tt]emperature", "[Ss]haved", "[Tt]hawed", "[Hh]alved", "[Rr]insed", "[Rr]emoved",
                "[Ss]eeded", "[Pp]icked"
        };

        /** See RecipeEntry for comments on following methods **/

        public static Uri buildIngredientUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildUriFromIngredientId(long ingredientId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(ingredientId))
                    .build();
        }

        public static Uri buildSearchUriForIngredient(String ingredient) {
            return CONTENT_URI.buildUpon()
                    .appendQueryParameter(COLUMN_INGREDIENT_NAME, ingredient)
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
    public static class LinkIngredientEntry implements BaseColumns {
        // URI for linking the two tables with their measurements
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_INGREDIENT_LINK).build();

        // Since this table will not be directly selected for, only the item type is used for
        // insertions
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INGREDIENT_LINK;

        // Table name
        public static final String TABLE_NAME = "ingredient_link";

        // Columns (Only the quantity column is required because all other columns will be
        // foreign keys)
        public static final String COLUMN_QUANTITY = "quantity";                    // TEXT NOT NULL
        public static final String COLUMN_INGREDIENT_ORDER = "ingredient_order";    // INTEGER NOT NULL
        public static final String COLUMN_SHOPPING = "shopping_list";               // INTEGER
        public static final String COLUMN_SHOPPING_CHECKED = "checked";             // INTEGER

        // Column projection and index
        public static final String[] LINK_PROJECTION = new String[] {
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID,
                RecipeEntry.COLUMN_RECIPE_SOURCE_ID,
                RecipeEntry.COLUMN_RECIPE_NAME,
                RecipeEntry.COLUMN_RECIPE_AUTHOR,
                RecipeEntry.COLUMN_IMG_URL,
                RecipeEntry.COLUMN_RECIPE_URL,
                RecipeEntry.COLUMN_SHORT_DESC,
                RecipeEntry.COLUMN_RATING,
                RecipeEntry.COLUMN_REVIEWS,
                RecipeEntry.COLUMN_DIRECTIONS,
                RecipeEntry.COLUMN_FAVORITE,
                RecipeEntry.COLUMN_SERVINGS,
                RecipeEntry.COLUMN_CALORIES,
                RecipeEntry.COLUMN_FAT,
                RecipeEntry.COLUMN_CARBS,
                RecipeEntry.COLUMN_PROTEIN,
                RecipeEntry.COLUMN_CHOLESTEROL,
                RecipeEntry.COLUMN_SODIUM,
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_SOURCE,
                IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID,
                IngredientEntry.COLUMN_INGREDIENT_NAME,
                COLUMN_QUANTITY,
                COLUMN_INGREDIENT_ORDER,
                COLUMN_SHOPPING,
                COLUMN_SHOPPING_CHECKED,
                _ID
        };

        public static final int IDX_RECIPE_ID = 0;
        public static final int IDX_RECIPE_SOURCE_ID = 1;
        public static final int IDX_RECIPE_NAME = 2;
        public static final int IDX_RECIPE_AUTHOR = 3;
        public static final int IDX_IMG_URL = 4;
        public static final int IDX_RECIPE_URL = 5;
        public static final int IDX_SHORT_DESC = 6;
        public static final int IDX_RECIPE_RATING = 7;
        public static final int IDX_RECIPE_REVIEWS = 8;
        public static final int IDX_RECIPE_DIRECTIONS = 9;
        public static final int IDX_RECIPE_FAVORITE = 10;
        public static final int IDX_RECIPE_SERVINGS = 11;
        public static final int IDX_RECIPE_CALORIES = 12;
        public static final int IDX_RECIPE_FAT = 13;
        public static final int IDX_RECIPE_CARBS = 14;
        public static final int IDX_RECIPE_PROTEIN = 15;
        public static final int IDX_RECIPE_CHOLESTEROL = 16;
        public static final int IDX_RECIPE_SODIUM = 17;
        public static final int IDX_RECIPE_SOURCE = 18;
        public static final int IDX_INGREDIENT_ID = 19;
        public static final int IDX_INGREDIENT_NAME = 20;
        public static final int IDX_LINK_QUANTITY = 21;
        public static final int IDX_LINK_INGREDIENT_ORDER = 22;
        public static final int IDX_LINK_SHOPPING = 23;
        public static final int IDX_LINK_CHECKED = 24;
        public static final int IDX_LINK_ID = 25;

        /** See RecipeEntry for comments on following methods **/

        public static Uri buildIngredientLinkUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildRecipeUriFromIngredientId(long ingredientId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(IngredientEntry.TABLE_NAME)
                    .appendPath(Long.toString(ingredientId))
                    .build();
        }

        public static Uri buildIngredientUriFromRecipe(long recipeId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(RecipeEntry.TABLE_NAME)
                    .appendPath(Long.toString(recipeId))
                    .build();
        }

        public static long getRecipeIdFromUri(Uri uri) {
            if (uri.getPathSegments().get(1).equals(RecipeEntry.TABLE_NAME)) {
                return Long.parseLong(uri.getPathSegments().get(2));
            } else {
                return -1;
            }
        }

        public static long getIngredientIdFromUri(Uri uri) {
            if (uri.getPathSegments().get(1).equals(IngredientEntry.TABLE_NAME)) {
                return Long.parseLong(uri.getPathSegments().get(2));
            } else {
                return -1;
            }
        }
    }

    public static class RecipeBookEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOK).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOK;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOK;

        // SCHEMA
        // Table name
        public static final String TABLE_NAME = "recipe_books";

        // Columns
        public static final String COLUMN_RECIPE_BOOK_ID = "recipe_book_id";                    // INTEGER AUTOINCREMENT
        public static final String COLUMN_RECIPE_BOOK_NAME = "recipe_book";                     // TEXT
        public static final String COLUMN_RECIPE_BOOK_DESCRIPTION = "recipe_book_description";  // TEXT

        // Column projection and index
        public static final String[] PROJECTION = new String[] {
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID,
                RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME,
                RecipeBookEntry.COLUMN_RECIPE_BOOK_DESCRIPTION,
        };

        public static final int IDX_BOOK_ID = 0;
        public static final int IDX_BOOK_NAME = 1;
        public static final int IDX_BOOK_DESCRIPTION = 2;

        public static Uri buildRecipeBookUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildUriFromRecipeBookId(long recipeBookId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(recipeBookId))
                    .build();
        }

        public static long getRecipeBookIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    public static class ChapterEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_CHAPTER).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHAPTER;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_CHAPTER;

        // SCHEMA
        // Table name
        public static final String TABLE_NAME = "chapters";

        // Columns
        public static final String COLUMN_CHAPTER_ID = "chapter_id";                    // REAL NOT NULL
        public static final String COLUMN_CHAPTER_NAME = "chapter_name";                // TEXT
        public static final String COLUMN_CHAPTER_DESCRIPTION = "chapter_description";  // TEXT
        public static final String COLUMN_CHAPTER_ORDER = "chapter_order";              // INTEGER NOT NULL

        public static final String[] PROJECTION = {
                COLUMN_CHAPTER_ID,
                COLUMN_CHAPTER_NAME,
                COLUMN_CHAPTER_DESCRIPTION,
                COLUMN_CHAPTER_ORDER,
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID
        };

        public static final int IDX_CHAPTER_ID = 0;
        public static final int IDX_CHAPTER_NAME = 1;
        public static final int IDX_CHAPTER_DESCRIPTION = 2;
        public static final int IDX_CHAPTER_ORDER = 3;
        public static final int IDX_BOOK_ID = 4;


        public static Uri buildChapterUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        public static Uri buildUriFromChapterId(long chapterId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(chapterId))
                    .build();
        }

        public static long getChapterIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }
    }

    public static class LinkRecipeBookEntry implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOK_LINK).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_BOOK_LINK;

        // SCHEMA
        // Table name
        public static final String TABLE_NAME = "recipe_book_link";

        // Columns
        public static final String COLUMN_RECIPE_ORDER = "recipe_order";

        public static Uri buildRecipeBookLinkUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }

        // Column projection and index
        public static final String[] PROJECTION = new String[] {
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID,
                RecipeEntry.COLUMN_RECIPE_NAME,
                RecipeEntry.COLUMN_RECIPE_AUTHOR,
                RecipeEntry.COLUMN_IMG_URL,
                RecipeEntry.COLUMN_RECIPE_URL,
                RecipeEntry.COLUMN_SHORT_DESC,
                RecipeEntry.COLUMN_RATING,
                RecipeEntry.COLUMN_REVIEWS,
                RecipeEntry.COLUMN_DIRECTIONS,
                RecipeEntry.COLUMN_FAVORITE,
                RecipeEntry.COLUMN_SERVINGS,
                RecipeEntry.COLUMN_CALORIES,
                RecipeEntry.COLUMN_FAT,
                RecipeEntry.COLUMN_CARBS,
                RecipeEntry.COLUMN_PROTEIN,
                RecipeEntry.COLUMN_CHOLESTEROL,
                RecipeEntry.COLUMN_SODIUM,
                RecipeEntry.COLUMN_SOURCE,
                RecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID,
                RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME,
                RecipeBookEntry.COLUMN_RECIPE_BOOK_DESCRIPTION,
                ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID,
                ChapterEntry.COLUMN_CHAPTER_NAME,
                ChapterEntry.COLUMN_CHAPTER_DESCRIPTION,
                ChapterEntry.COLUMN_CHAPTER_ORDER,
                COLUMN_RECIPE_ORDER
        };

        public static final int IDX_RECIPE_ID = 0;
        public static final int IDX_RECIPE_NAME = 1;
        public static final int IDX_RECIPE_AUTHOR = 2;
        public static final int IDX_IMG_URL = 3;
        public static final int IDX_RECIPE_URL = 4;
        public static final int IDX_SHORT_DESC = 5;
        public static final int IDX_RECIPE_RATING = 6;
        public static final int IDX_RECIPE_REVIEWS = 7;
        public static final int IDX_RECIPE_DIRECTIONS = 8;
        public static final int IDX_RECIPE_FAVORITE = 9;
        public static final int IDX_RECIPE_SERVINGS = 10;
        public static final int IDX_RECIPE_CALORIES = 11;
        public static final int IDX_RECIPE_FAT = 12;
        public static final int IDX_RECIPE_CARBS = 13;
        public static final int IDX_RECIPE_PROTEIN = 14;
        public static final int IDX_RECIPE_CHOLESTEROL = 15;
        public static final int IDX_RECIPE_SODIUM = 16;
        public static final int IDX_RECIPE_SOURCE = 17;
        public static final int IDX_BOOK_ID = 18;
        public static final int IDX_BOOK_NAME = 19;
        public static final int IDX_BOOK_DESCRIPTION = 20;
        public static final int IDX_CHAPTER_ID = 21;
        public static final int IDX_CHAPTER_NAME = 22;
        public static final int IDX_CHAPTER_DESCRIPTION = 23;
        public static final int IDX_CHAPTER_ORDER = 24;
        public static final int IDX_RECIPE_ORDER = 25;

        /** Methods for building URIs for filtering the database **/
        // Not sure this is needed
        public static Uri buildChapterUriFromRecipeBookId(long recipeBookId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(recipeBookId))
                    .appendPath(ChapterEntry.TABLE_NAME)
                    .build();
        }

        public static Uri buildRecipeUriFromBookAndChapterId(long recipeBookId, long chapterId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(recipeBookId))
                    .appendPath(ChapterEntry.TABLE_NAME)
                    .appendPath(Long.toString(chapterId))
                    .build();
        }

        public static Uri buildRecipeUriFromRecipeBookId(long recipeBookId) {
            return CONTENT_URI.buildUpon()
                    .appendPath(Long.toString(recipeBookId))
                    .appendPath(RecipeEntry.TABLE_NAME)
                    .build();
        }

        public static long getRecipeBookIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(1));
        }

        public static long getChapterIdFromUri(Uri uri) {
            return Long.parseLong(uri.getPathSegments().get(3));
        }
    }
}
