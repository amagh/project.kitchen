package project.hnoct.kitchen.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import project.hnoct.kitchen.data.RecipeContract.RecipeEntry;
import project.hnoct.kitchen.data.RecipeContract.IngredientEntry;
import project.hnoct.kitchen.data.RecipeContract.LinkEntry;


/**
 * Created by hnoct on 2/16/2017.
 */

public class RecipeProvider extends ContentProvider {
    /** Constants **/
    static final int RECIPE = 100;
    static final int RECIPE_WITH_ID = 101;
    static final int RECIPE_AND_INGREDIENT = 200;
    static final int RECIPE_AND_INGREDIENT_QUERY = 201;
    static final int INGREDIENT = 300;
    static final int INGREDIENT_WITH_ID = 301;

    private static final SQLiteQueryBuilder sRecipeAndIngredientQueryBuilder;

    static {
        // Joins the three tables together so that they can be queried as a single entity
        sRecipeAndIngredientQueryBuilder = new SQLiteQueryBuilder();
        sRecipeAndIngredientQueryBuilder.setTables(
                RecipeEntry.TABLE_NAME + " INNER JOIN " +
                        LinkEntry.TABLE_NAME + " ON " +
                        RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = " +
                        LinkEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " INNER JOIN " +
                        IngredientEntry.TABLE_NAME + " ON " +
                        LinkEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID + " = " +
                        IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID
        );
    }

    // URI Matcher used by this Content Provider
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    /** Member Variables **/
    private RecipeDbHelper mDbHelper;

    /**
     * Matches URIs to the content provider can access the correct database and rows
     * @return UriMatcher that will match the query to the operation
     */
    static UriMatcher buildUriMatcher() {
        /** Constants **/
        final String authority = RecipeContract.CONTENT_AUTHORITY;

        // Root URI
        final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // URIs to be matched
        uriMatcher.addURI(authority, RecipeContract.PATH_RECIPE, RECIPE);
        uriMatcher.addURI(authority, RecipeContract.PATH_RECIPE + "/#", RECIPE_WITH_ID);
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT, INGREDIENT);
        uriMatcher.addURI(authority, RecipeContract.PATH_INGREDIENT + "/#", INGREDIENT_WITH_ID);
        uriMatcher.addURI(authority, RecipeContract.PATH_LINK, RECIPE_AND_INGREDIENT);      // Used for inserting into Link Table
        uriMatcher.addURI(authority, RecipeContract.PATH_LINK + "/*", RECIPE_AND_INGREDIENT_QUERY);

        return uriMatcher;
    }

    // Static constants for querying with selection
    private static final String sRecipeSelection = RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ?";
    private static final String sIngredientSelection = IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";

    /**
     * Helper method for creating a {@link Cursor} given a URI containing a recipeId
     * @param uri the Uri containing the recipeId
     * @param projection columns to return
     * @param sortOrder sort order
     * @return {@link Cursor} selecting the recipe in the database
     */
    private Cursor getRecipeById(Uri uri, String[] projection, String sortOrder) {
        // Retrieve the recipeId from the provided URI
        long recipeId = RecipeEntry.getRecipeIdFromUri(uri);

        // Set the selection and its args equal to the recipeId column and the recipeId respectively
        String selection = sRecipeSelection;
        String[] selectionArgs = new String[] {Long.toString(recipeId)};

        // Create and return the cursor
        return mDbHelper.getReadableDatabase().query(
                RecipeEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /** See {@link #getRecipeById(Uri, String[], String)} **/
    private Cursor getIngredientById(Uri uri, String[] projection, String sortOrder) {
        long ingredientId = IngredientEntry.getIngredientIdFromUri(uri);

        String selection = sIngredientSelection;
        String[] selectionArgs = new String[] {Long.toString(ingredientId)};

        return mDbHelper.getReadableDatabase().query(
                IngredientEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    /**
     * Helper method for building a query for all ingredients for a given recipe or vice-versa
     * @param uri Uri containing either a query for a recipe or an ingredient
     * @param projection columns to return
     * @param sortOrder order to sort in
     * @return Cursor selecting all returned items of a given query (e.g. all ingredients for a recipe)
     */
    private Cursor filterRecipeAndIngredientById(Uri uri, String[] projection, String sortOrder) {
        // Attempt to retrieve both a recipeId and ingredientId from the URI
        long recipeId = LinkEntry.getRecipeIdFromUri(uri);
        long ingredientId = LinkEntry.getIngredientIdFromUri(uri);

        if (recipeId != -1) {
            // If recipeId can be retrieved, then query all ingredients for a given recipe

            // Select for the ingredientId column
            String selection = sRecipeSelection;

            // Filter for ingredients that match recipeId
            String[] selectionArgs = new String[] {Long.toString(recipeId)};

            // Return the Cursor
            return sRecipeAndIngredientQueryBuilder.query(
                    mDbHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        } else {
            // If recipeId cannot be retrieved, then ingredientId is being queried

            // Select for the ingredientsId column
            String selection = sIngredientSelection;

            // Filter by recipes that contain an ingredient
            String[] selectionArgs = new String[] {Long.toString(ingredientId)};

            // Return the Cursor
            return sRecipeAndIngredientQueryBuilder.query(
                    mDbHelper.getReadableDatabase(),
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        }
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new RecipeDbHelper(getContext());
        return true;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case RECIPE:                            // For accessing Recipe Table
                return RecipeEntry.CONTENT_TYPE;
            case RECIPE_WITH_ID:                    // For accessing a single row on the Recipe Table
                return RecipeEntry.CONTENT_ITEM_TYPE;
            case INGREDIENT:                        // For accessing the Ingredient Table
                return IngredientEntry.CONTENT_TYPE;
            case INGREDIENT_WITH_ID:                // For accessing a single row on the Ingredient Table
                return IngredientEntry.CONTENT_ITEM_TYPE;
            case RECIPE_AND_INGREDIENT:             // For accessing the linked tables
                return LinkEntry.CONTENT_TYPE;
            case RECIPE_AND_INGREDIENT_QUERY:       // For filtering the linked table for one or the other
                return LinkEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor;

        switch(sUriMatcher.match(uri)) {
            case RECIPE: {
                cursor = mDbHelper.getReadableDatabase().query(
                        RecipeEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case RECIPE_WITH_ID: {
                cursor = getRecipeById(uri, projection, sortOrder);
                break;
            }
            case INGREDIENT: {
                cursor = mDbHelper.getReadableDatabase().query(
                        IngredientEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            }
            case INGREDIENT_WITH_ID: {
                cursor = getIngredientById(uri, projection, sortOrder);
                break;
            }
            case RECIPE_AND_INGREDIENT_QUERY: {
                cursor = filterRecipeAndIngredientById(uri, projection, sortOrder);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        Uri returnUri;
        switch(sUriMatcher.match(uri)) {
            case RECIPE: {
                long _id = mDbHelper.getWritableDatabase().insert(
                        RecipeEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = RecipeEntry.buildRecipeUri(_id);
                } else {
                    throw new SQLException("Error inserting recipe into database.");
                }
                break;
            }
            case INGREDIENT: {
                long _id = mDbHelper.getWritableDatabase().insert(
                        IngredientEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = IngredientEntry.buildIngredientUri(_id);
                } else {
                    throw new SQLException("Error inserting ingredient into database.");
                }
                break;
            }
            case RECIPE_AND_INGREDIENT: {
                long _id = mDbHelper.getWritableDatabase().insert(
                        LinkEntry.TABLE_NAME,
                        null,
                        contentValues
                );
                if (_id > 0) {
                    returnUri = LinkEntry.buildLinkUri(_id);
                } else {
                    throw new SQLException("Error inserting quantity into link table");
                }
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        return 0;
    }

}
