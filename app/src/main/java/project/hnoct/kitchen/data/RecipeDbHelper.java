package project.hnoct.kitchen.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import project.hnoct.kitchen.data.RecipeContract.RecipeEntry;
import project.hnoct.kitchen.data.RecipeContract.IngredientEntry;
import project.hnoct.kitchen.data.RecipeContract.LinkEntry;

/**
 * Created by hnoct on 2/16/2017.
 */

public class RecipeDbHelper extends SQLiteOpenHelper {
    // Constants
    private static final String LOG_TAG = RecipeDbHelper.class.getSimpleName();
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "recipe.db";

    public RecipeDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Table storing a recipe and its details
        final String SQL_CREATE_RECIPE_TABLE = "CREATE TABLE " + RecipeEntry.TABLE_NAME + " (" +
                RecipeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                RecipeEntry.COLUMN_RECIPE_ID + " REAL NOT NULL, " +
                RecipeEntry.COLUMN_RECIPE_NAME + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_RECIPE_AUTHOR + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_THUMBNAIL_URL + " TEXT, " +
                RecipeEntry.COLUMN_IMG_URL + " TEXT, " +
                RecipeEntry.COLUMN_RECIPE_URL + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_RATING + " REAL, " +
                RecipeEntry.COLUMN_REVIEWS + " REAL, " +
                RecipeEntry.COLUMN_SHORT_DESC + " TEXT, " +
                RecipeEntry.COLUMN_DIRECTIONS + " TEXT, " +
                RecipeEntry.COLUMN_DATE_ADDED + " TEXT NOT NULL, " +
                RecipeEntry.COLUMN_FAVORITE + " INTEGER NOT NULL, " +
                RecipeEntry.COLUMN_SOURCE + " TEXT NOT NULL, " +
                // Links to the relational table to reference the quantity of each ingredient
                "FOREIGN KEY (" + RecipeEntry.COLUMN_RECIPE_ID + ") REFERENCES " +
                LinkEntry.TABLE_NAME + " (" + RecipeEntry.COLUMN_RECIPE_ID + "));";

        // Table storing ingredients
        final String SQL_CREATE_INGREDIENT_TABLE = "CREATE TABLE " + IngredientEntry.TABLE_NAME + " (" +
                IngredientEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                IngredientEntry.COLUMN_INGREDIENT_ID + " REAL NOT NULL, " +
                IngredientEntry.COLUMN_INGREDIENT_NAME + " TEXT NOT NULL, " +
                // Links to the relational table to reference the quantity of each ingredient
                "FOREIGN KEY (" + IngredientEntry.COLUMN_INGREDIENT_ID + ") REFERENCES " +
                LinkEntry.TABLE_NAME + " (" + IngredientEntry.COLUMN_INGREDIENT_ID + "));";

        // Table for relating the amount of ingredients in each recipe
        final String SQL_CRATE_LINK_TABLE = "CREATE TABLE " + LinkEntry.TABLE_NAME + " (" +
                RecipeEntry.COLUMN_RECIPE_ID + " REAL NOT NULL, " +
                IngredientEntry.COLUMN_INGREDIENT_ID + " REAL NOT NULL, " +
                LinkEntry.COLUMN_QUANTITY + " TEXT NOT NULL, " +
                LinkEntry.COLUMN_INGREDIENT_ORDER + " INTEGER NOT NULL, " +
                // Utilize the combination of recipe and ingredient as the primary key
                "PRIMARY KEY (" + RecipeEntry.COLUMN_RECIPE_ID + ", " + IngredientEntry.COLUMN_INGREDIENT_ID + "));";

        sqLiteDatabase.execSQL(SQL_CREATE_RECIPE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_INGREDIENT_TABLE);
        sqLiteDatabase.execSQL(SQL_CRATE_LINK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // Since this only caches weather data from online, there is no need to preserve the data
        // when changing version numbers, so they are merely discarded and tables are created using
        // the updated schema
        if (newVersion != oldVersion) {
            Log.v(LOG_TAG, "onUpgrade: ");
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + RecipeEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + IngredientEntry.TABLE_NAME);
            sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + LinkEntry.TABLE_NAME);
            onCreate(sqLiteDatabase);
        }
    }
}
