package project.kitchen.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import project.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/16/2017.
 */

public class RecipeDbHelper extends SQLiteOpenHelper {
    // Constants
    private static final String LOG_TAG = RecipeDbHelper.class.getSimpleName();
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "recipe.db";

    // SQL Statements for upgrades
    private final String SQL_ADD_SHOPPING_COLUMN = "ALTER TABLE " + LinkIngredientEntry.TABLE_NAME + " " +
            "ADD COLUMN " + LinkIngredientEntry.COLUMN_SHOPPING + " INTEGER;";

    private final String SQL_ADD_CHECKED_COLUMN = "ALTER TABLE " + LinkIngredientEntry.TABLE_NAME + " " +
            "ADD COLUMN " + LinkIngredientEntry.COLUMN_SHOPPING_CHECKED + " INTEGER;";

    public RecipeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Table storing a recipe and its details
        final String SQL_CREATE_RECIPE_TABLE = "CREATE TABLE " + RecipeEntry.TABLE_NAME + " (" +
                RecipeEntry.COLUMN_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_RECIPE_NAME + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_RECIPE_AUTHOR + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_IMG_URL + " TEXT, " +
                RecipeEntry.COLUMN_RECIPE_URL + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_SHORT_DESC + " TEXT, " +
                RecipeEntry.COLUMN_RATING + " REAL, " +
                RecipeEntry.COLUMN_REVIEWS + " INTEGER, " +
                RecipeEntry.COLUMN_DIRECTIONS + " TEXT, " +
                RecipeEntry.COLUMN_DATE_ADDED + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_FAVORITE + " INTEGER NOT NULL, " +
                RecipeEntry.COLUMN_SOURCE + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_CALORIES + " REAL, " +
                RecipeEntry.COLUMN_FAT + " REAL, " +
                RecipeEntry.COLUMN_CARBS + " REAL, " +
                RecipeEntry.COLUMN_PROTEIN + " REAL, " +
                RecipeEntry.COLUMN_CHOLESTEROL + " REAL, " +
                RecipeEntry.COLUMN_SODIUM + " REAL, " +
                RecipeEntry.COLUMN_SERVINGS+ " INTEGER, " +
                "UNIQUE (" + RecipeEntry.COLUMN_RECIPE_SOURCE_ID + ", " + RecipeEntry.COLUMN_SOURCE + "));";

        // Table storing ingredients
        final String SQL_CREATE_INGREDIENT_TABLE = "CREATE TABLE " + IngredientEntry.TABLE_NAME + " (" +
                IngredientEntry.COLUMN_INGREDIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                IngredientEntry.COLUMN_INGREDIENT_NAME + " TEXT NOT NULL, " +
                IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID + " REAL, " +
                IngredientEntry.COLUMN_FOOD_INGREDIENT_ID + " REAL);";

        // Table for relating the amount of ingredients in each recipe
        final String SQL_CREATE_INGREDIENT_LINK_TABLE = "CREATE TABLE " + LinkIngredientEntry.TABLE_NAME + " (" +
                LinkIngredientEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecipeEntry.COLUMN_RECIPE_ID + " INTEGER NOT NULL, " +
                IngredientEntry.COLUMN_INGREDIENT_ID + " INTEGER NOT NULL, " +
                LinkIngredientEntry.COLUMN_QUANTITY + " TEXT NOT NULL, " +
                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " INTEGER NOT NULL, " +
                LinkIngredientEntry.COLUMN_SHOPPING + " INTEGER, " +
                LinkIngredientEntry.COLUMN_SHOPPING_CHECKED + " INTEGER, " +
                // Utilize the combination of unique index and ingredient as the primary key
//                "PRIMARY KEY (" + RecipeEntry.COLUMN_RECIPE_ID + "," +
//                RecipeEntry.COLUMN_SOURCE + ") " +
                "UNIQUE (" + RecipeEntry.COLUMN_RECIPE_ID + ", " +
                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + ") " +
                // Foreign keys to recipes.unique_id_source & ingredients.ingredient_id
                "FOREIGN KEY (" + RecipeEntry.COLUMN_RECIPE_ID + ") REFERENCES " +
                RecipeEntry.TABLE_NAME + " (" + RecipeEntry.COLUMN_RECIPE_ID + ") " +
                "FOREIGN KEY (" + IngredientEntry.COLUMN_INGREDIENT_ID + ") REFERENCES " +
                IngredientEntry.TABLE_NAME + " (" + IngredientEntry.COLUMN_INGREDIENT_ID + "));";

        // Table for organizing recipes in recipe books
        final String SQL_CREATE_RECIPE_BOOK_TABLE = "CREATE TABLE " + RecipeBookEntry.TABLE_NAME + " (" +
//                RecipeBookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME + " TEXT," +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_DESCRIPTION + " TEXT);";

        // Table for organizing chapters
        final String SQL_CREATE_CHAPTER_TABLE = "CREATE TABLE " + ChapterEntry.TABLE_NAME + " (" +
//                ChapterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ChapterEntry.COLUMN_CHAPTER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                ChapterEntry.COLUMN_CHAPTER_NAME + " TEXT, " +
                ChapterEntry.COLUMN_CHAPTER_DESCRIPTION + " TEXT, " +
                ChapterEntry.COLUMN_CHAPTER_ORDER + " INTEGER NOT NULL, " +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY (" + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + ") REFERENCES " +
                RecipeBookEntry.TABLE_NAME + " (" + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + "));";

        // Table for relating recipe books, chapters, and recipes
        final String SQL_CREATE_RECIPE_BOOK_LINK_TABLE = "CREATE TABLE " + LinkRecipeBookEntry.TABLE_NAME + " (" +
                RecipeBookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " REAL NOT NULL, " +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " INTEGER NOT NULL, " +
//                ChapterEntry.COLUMN_CHAPTER_ID + " REAL NOT NULL, " +
                ChapterEntry.COLUMN_CHAPTER_ID + " INTEGER NOT NULL, " +
                LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " INTEGER NOT NULL, " +
                RecipeEntry.COLUMN_RECIPE_ID + " INTEGER NOT NULL, " +
                // Utilize a combination of all three columns as the primary key because each
                // chapter in the recipe book should be unique and each recipe should only occupy
                // a spot in the ordering within the chapter
                "UNIQUE (" + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + ", " +
                ChapterEntry.COLUMN_CHAPTER_ID + ", " +
                LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + ") " +
                // Foreign keys to recipe_books.recipe_book_id & chapters.chapter_id
                "FOREIGN KEY (" + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + ") REFERENCES " +
                RecipeBookEntry.TABLE_NAME + " (" + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + ") " +
                "FOREIGN KEY (" + ChapterEntry.COLUMN_CHAPTER_ID + ") REFERENCES " +
                ChapterEntry.TABLE_NAME + " (" + ChapterEntry.COLUMN_CHAPTER_ID + ") " +
                "FOREIGN KEY (" + RecipeEntry.COLUMN_RECIPE_ID + ") REFERENCES " +
                RecipeEntry.TABLE_NAME + " (" + RecipeEntry.COLUMN_RECIPE_ID + "));";

        sqLiteDatabase.execSQL(SQL_CREATE_RECIPE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_INGREDIENT_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_INGREDIENT_LINK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_RECIPE_BOOK_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_CHAPTER_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_RECIPE_BOOK_LINK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Since this only caches weather data from online, there is no need to preserve the data
        // when changing version numbers, so they are merely discarded and tables are created using
        // the updated schema

        switch (oldVersion) {
            case 1: {
                sqLiteDatabase.execSQL(SQL_ADD_SHOPPING_COLUMN);
            }
            case 2: {
                sqLiteDatabase.execSQL(SQL_ADD_CHECKED_COLUMN);
            }
        }

//        // Table storing a recipe and its details
//        final String SQL_CREATE_RECIPE_TABLE = "CREATE TABLE " + RecipeEntry.TABLE_NAME + " (" +
//                RecipeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                RecipeEntry.COLUMN_RECIPE_ID + " INTEGER NOT NULL, " +
//                RecipeEntry.COLUMN_RECIPE_NAME + " TEXT NOT NULL, " +
//                RecipeEntry.COLUMN_RECIPE_AUTHOR + " TEXT NOT NULL, " +
//                RecipeEntry.COLUMN_IMG_URL + " TEXT, " +
//                RecipeEntry.COLUMN_RECIPE_URL + " TEXT NOT NULL, " +
//                RecipeEntry.COLUMN_SHORT_DESC + " TEXT, " +
//                RecipeEntry.COLUMN_RATING + " REAL, " +
//                RecipeEntry.COLUMN_REVIEWS + " INTER, " +
//                RecipeEntry.COLUMN_DIRECTIONS + " TEXT, " +
//                RecipeEntry.COLUMN_DATE_ADDED + " TEXT NOT NULL, " +
//                RecipeEntry.COLUMN_FAVORITE + " INTEGER NOT NULL, " +
//                RecipeEntry.COLUMN_SOURCE + " TEXT NOT NULL, " +
//                RecipeEntry.COLUMN_CALORIES + " REAL, " +
//                RecipeEntry.COLUMN_FAT + " REAL, " +
//                RecipeEntry.COLUMN_CARBS + " REAL, " +
//                RecipeEntry.COLUMN_PROTEIN + " REAL, " +
//                RecipeEntry.COLUMN_CHOLESTEROL + " REAL, " +
//                RecipeEntry.COLUMN_SODIUM + " REAL, " +
//                RecipeEntry.COLUMN_SERVINGS+ " INTEGER, " +
//                "UNIQUE (" + RecipeEntry.COLUMN_RECIPE_ID + ", " + RecipeEntry.COLUMN_SOURCE + "));";
//
//        // Table storing ingredients
//        final String SQL_CREATE_INGREDIENT_TABLE = "CREATE TABLE " + IngredientEntry.TABLE_NAME + " (" +
//                IngredientEntry.COLUMN_INGREDIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                IngredientEntry.COLUMN_INGREDIENT_NAME + " TEXT NOT NULL, " +
//                IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID + " REAL, " +
//                IngredientEntry.COLUMN_FOOD_INGREDIENT_ID + " REAL);";
//
//        // Table for relating the amount of ingredients in each recipe
//        final String SQL_CREATE_INGREDIENT_LINK_TABLE = "CREATE TABLE " + LinkIngredientEntry.TABLE_NAME + " (" +
//                LinkRecipeBookEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
//                RecipeEntry.COLUMN_RECIPE_ID + " REAL NOT NULL, " +
//                RecipeEntry.COLUMN_SOURCE + " TEXT NOT NULL, " +
//                IngredientEntry.COLUMN_INGREDIENT_ID + " REAL NOT NULL, " +
//                LinkIngredientEntry.COLUMN_QUANTITY + " TEXT NOT NULL, " +
//                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " INTEGER NOT NULL, " +
//                // Utilize the combination of unique index and ingredient as the primary key
////                "PRIMARY KEY (" + RecipeEntry.COLUMN_RECIPE_ID + "," +
////                RecipeEntry.COLUMN_SOURCE + ") " +
//                "UNIQUE (" + RecipeEntry.COLUMN_RECIPE_ID + ", " +
//                RecipeEntry.COLUMN_SOURCE + ", " +
//                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + ") " +
//                // Foreign keys to recipes.unique_id_source & ingredients.ingredient_id
//                "FOREIGN KEY (" + RecipeEntry.COLUMN_RECIPE_ID + ") REFERENCES " +
//                RecipeEntry.TABLE_NAME + " (" + RecipeEntry.COLUMN_RECIPE_ID + ") " +
//                "FOREIGN KEY (" + RecipeEntry.COLUMN_SOURCE + ") REFERENCES " +
//                RecipeEntry.TABLE_NAME + " (" + RecipeEntry.COLUMN_SOURCE + ") " +
//                "FOREIGN KEY (" + IngredientEntry.COLUMN_INGREDIENT_ID + ") REFERENCES " +
//                IngredientEntry.TABLE_NAME + " (" + IngredientEntry.COLUMN_INGREDIENT_ID + "));";
//
//
//        if (oldVersion < 0) {
//            Log.v(LOG_TAG, "onUpgrade: ");
//            final String SQL_RENAME_RECIPE_TABLE = "ALTER TABLE " + RecipeEntry.TABLE_NAME + " " +
//                    "RENAME TO tmp_" + RecipeEntry.TABLE_NAME + ";";
//
//            final String SQL_COPY_RECIPE_DATA = "INSERT INTO " + RecipeEntry.TABLE_NAME + "(" +
//                    RecipeEntry._ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_NAME + ", " +
//                    RecipeEntry.COLUMN_RECIPE_AUTHOR + ", " +
//                    RecipeEntry.COLUMN_IMG_URL + ", " +
//                    RecipeEntry.COLUMN_RECIPE_URL + ", " +
//                    RecipeEntry.COLUMN_RATING + ", " +
//                    RecipeEntry.COLUMN_REVIEWS + ", " +
//                    RecipeEntry.COLUMN_SHORT_DESC + ", " +
//                    RecipeEntry.COLUMN_DIRECTIONS + ", " +
//                    RecipeEntry.COLUMN_DATE_ADDED + ", " +
//                    RecipeEntry.COLUMN_FAVORITE + ", " +
//                    RecipeEntry.COLUMN_SOURCE + ", " +
//                    RecipeEntry.COLUMN_CALORIES + ", " +
//                    RecipeEntry.COLUMN_FAT + ", " +
//                    RecipeEntry.COLUMN_CARBS + ", " +
//                    RecipeEntry.COLUMN_PROTEIN + ", " +
//                    RecipeEntry.COLUMN_CHOLESTEROL + ", " +
//                    RecipeEntry.COLUMN_SODIUM + ", " +
//                    RecipeEntry.COLUMN_SERVINGS+ ") " +
//                    "SELECT " +
//                    RecipeEntry._ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_NAME + ", " +
//                    RecipeEntry.COLUMN_RECIPE_AUTHOR + ", " +
//                    RecipeEntry.COLUMN_IMG_URL + ", " +
//                    RecipeEntry.COLUMN_RECIPE_URL + ", " +
//                    RecipeEntry.COLUMN_SHORT_DESC + ", " +
//                    RecipeEntry.COLUMN_RATING + ", " +
//                    RecipeEntry.COLUMN_REVIEWS + ", " +
//                    RecipeEntry.COLUMN_DIRECTIONS + ", " +
//                    RecipeEntry.COLUMN_DATE_ADDED + ", " +
//                    RecipeEntry.COLUMN_FAVORITE + ", " +
//                    RecipeEntry.COLUMN_SOURCE + ", " +
//                    RecipeEntry.COLUMN_CALORIES + ", " +
//                    RecipeEntry.COLUMN_FAT + ", " +
//                    RecipeEntry.COLUMN_CARBS + ", " +
//                    RecipeEntry.COLUMN_PROTEIN + ", " +
//                    RecipeEntry.COLUMN_CHOLESTEROL + ", " +
//                    RecipeEntry.COLUMN_SODIUM + ", " +
//                    RecipeEntry.COLUMN_SERVINGS+ " " +
//                    "FROM tmp_" + RecipeEntry.TABLE_NAME + ";";
//
//            final String SQL_RENAME_INGREDIENT_TABLE = "ALTER TABLE " + IngredientEntry.TABLE_NAME + " " +
//                    "RENAME TO tmp_" + IngredientEntry.TABLE_NAME + ";";
//
//            final String SQL_COPY_INGREDIENT_DATA = "INSERT INTO " + IngredientEntry.TABLE_NAME + "(" +
//                    IngredientEntry.COLUMN_INGREDIENT_ID + ", " +
//                    IngredientEntry.COLUMN_INGREDIENT_NAME + ", " +
//                    IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID + ") " +
//                    "SELECT " +
//                    IngredientEntry.COLUMN_INGREDIENT_ID + ", " +
//                    IngredientEntry.COLUMN_INGREDIENT_NAME + ", " +
//                    "ingredient_id " +
//                    "FROM tmp_" + IngredientEntry.TABLE_NAME;
//
//            final String SQL_RENAME_INGREDIENT_LINK_TABLE = "ALTER TABLE " + LinkIngredientEntry.TABLE_NAME + " " +
//                    "RENAME TO tmp_" + LinkIngredientEntry.TABLE_NAME + ";";
//
//            final String SQL_COPY_INGREDIENT_LINK_DATA = "INSERT INTO " + LinkIngredientEntry.TABLE_NAME + "(" +
//                    LinkRecipeBookEntry._ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_ID + ", " +
//                    RecipeEntry.COLUMN_SOURCE + ", " +
//                    IngredientEntry.COLUMN_INGREDIENT_ID + ", " +
//                    LinkIngredientEntry.COLUMN_QUANTITY + ", " +
//                    LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + ") " +
//                    "SELECT " +
//                    LinkRecipeBookEntry._ID + ", " +
//                    RecipeEntry.COLUMN_RECIPE_ID + ", " +
//                    RecipeEntry.COLUMN_SOURCE + ", " +
//                    "ingredient_id, " +
//                    LinkIngredientEntry.COLUMN_QUANTITY + ", " +
//                    LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " " +
//                    "FROM tmp_" + LinkIngredientEntry.TABLE_NAME + ";";
//
//            final String SQL_DROP_TMP_RECIPE_TABLE = "DROP TABLE IF EXISTS tmp_" + RecipeEntry.TABLE_NAME;
//            final String SQL_DROP_TMP_INGREIDENT_TABLE = "DROP TABLE IF EXISTS tmp_" + IngredientEntry.TABLE_NAME;
//            final String SQL_DROP_TMP_INGREDIENT_LINK_TABLE = "DROP TABLE IF EXISTS tmp_" + LinkIngredientEntry.TABLE_NAME;
//
//            sqLiteDatabase.beginTransaction();
//            sqLiteDatabase.execSQL(SQL_RENAME_RECIPE_TABLE);
//            sqLiteDatabase.execSQL(SQL_CREATE_RECIPE_TABLE);
//            sqLiteDatabase.execSQL(SQL_COPY_RECIPE_DATA);
//            sqLiteDatabase.execSQL(SQL_DROP_TMP_RECIPE_TABLE);
//            sqLiteDatabase.execSQL(SQL_RENAME_INGREDIENT_TABLE);
//            sqLiteDatabase.execSQL(SQL_CREATE_INGREDIENT_TABLE);
//            sqLiteDatabase.execSQL(SQL_COPY_INGREDIENT_DATA);
//            sqLiteDatabase.execSQL(SQL_DROP_TMP_INGREIDENT_TABLE);
//            sqLiteDatabase.execSQL(SQL_RENAME_INGREDIENT_LINK_TABLE);
//            sqLiteDatabase.execSQL(SQL_CREATE_INGREDIENT_LINK_TABLE);
//            sqLiteDatabase.execSQL(SQL_COPY_INGREDIENT_LINK_DATA);
//            sqLiteDatabase.execSQL(SQL_DROP_TMP_INGREDIENT_LINK_TABLE);
//            sqLiteDatabase.setTransactionSuccessful();
//            sqLiteDatabase.endTransaction();
//        }
    }
}
