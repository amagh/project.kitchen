package project.hnoct.kitchen.ui;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.sync.AllRecipeAsyncTask;

/**
 * A placeholder fragment containing a simple view.
 */
public class RecipeDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    /** Constants **/
    private static final String LOG_TAG = RecipeDetailsFragment.class.getSimpleName();
    private static final int DETAILS_LOADER = 1;
    public static final String RECIPE_DETAILS_URI = "recipe_and_ingredients_uri";

    private static final String[] DETAILS_PROJECTION = new String[] {
            RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID,
            RecipeEntry.COLUMN_RECIPE_NAME,
            RecipeEntry.COLUMN_RECIPE_AUTHOR,
            RecipeEntry.COLUMN_IMG_URL,
            RecipeEntry.COLUMN_RECIPE_URL,
            RecipeEntry.COLUMN_SHORT_DESC,
            RecipeEntry.COLUMN_RATING,
            RecipeEntry.COLUMN_REVIEWS,
            RecipeEntry.COLUMN_DIRECTIONS,
            RecipeEntry.COLUMN_FAVORITED,
            RecipeEntry.COLUMN_SOURCE,
            IngredientEntry.TABLE_NAME + "." + IngredientEntry.COLUMN_INGREDIENT_ID,
            IngredientEntry.COLUMN_INGREDIENT_NAME,
            LinkEntry.COLUMN_QUANTITY,
            LinkEntry.TABLE_NAME + "." + LinkEntry._ID
    };

    // Column index for the projection
    private static final int IDX_RECIPE_ID = 0;
    private static final int IDX_RECIPE_NAME = 1;
    private static final int IDX_RECIPE_AUTHOR = 2;
    private static final int IDX_IMG_URL = 3;
    private static final int IDX_RECIPE_URL = 4;
    private static final int IDX_SHORT_DESC = 5;
    private static final int IDX_RECIPE_RATING = 6;
    private static final int IDX_RECIPE_REVIEWS = 7;
    private static final int IDX_RECIPE_DIRECTIONS = 8;
    private static final int IDX_RECIPE_FAVORITE = 9;
    private static final int IDX_RECIPE_SOURCE = 10;
    private static final int IDX_INGREDIENT_ID = 11;
    private static final int IDX_INGREDIENT_NAME = 12;
    private static final int IDX_LINK_QUANTITY = 13;
    private static final int IDX_LINK_ID = 14;

    /** Member Variables **/
    Uri mRecipeUri;
    Context mContext;
    Cursor mCursor;
    ContentResolver mContentResolver;

    // Views bound by ButterKnife

    public RecipeDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipe_details, container, false);

        if (getArguments() != null) {
            mRecipeUri = getArguments().getParcelable(RECIPE_DETAILS_URI);
        } else {
            Log.d(LOG_TAG, "No bundle found!");
        }


        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort the columns by order that ingredient was added to link table
        String sortOrder = LinkEntry.TABLE_NAME + "." + LinkEntry._ID + " ASC";

        // Initialize and return CursorLoader
        return new CursorLoader(
                mContext,
                mRecipeUri,
                DETAILS_PROJECTION,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            // Ensure that cursor was returned
            mCursor = cursor;
        } else {
            return;
        }

        // Move Cursor to first row
        mCursor.moveToFirst();

        // Retrieve recipe information from database
        long recipeId = mCursor.getLong(IDX_RECIPE_ID);
        String recipeTitle = mCursor.getString(IDX_RECIPE_NAME);
        String recipeAuthor = mCursor.getString(IDX_RECIPE_AUTHOR);
        String recipeImageUrl = mCursor.getString(IDX_IMG_URL);
        String recipeUrl = mCursor.getString(IDX_RECIPE_URL);
        String recipeDescription = mCursor.getString(IDX_SHORT_DESC);
        double recipeRating = mCursor.getDouble(IDX_RECIPE_RATING);
        long recipeReviews = mCursor.getLong(IDX_RECIPE_REVIEWS);
        String recipeDirections = mCursor.getString(IDX_RECIPE_DIRECTIONS);
        boolean recipeFavorite = mCursor.getInt(IDX_RECIPE_FAVORITE) == 1;
        String recipeSource = mCursor.getString(IDX_RECIPE_SOURCE);
        long ingredientId = mCursor.getLong(IDX_INGREDIENT_ID);
        String ingredientName = mCursor.getString(IDX_INGREDIENT_NAME);
        String ingredientQuantity = mCursor.getString(IDX_LINK_QUANTITY);

        if (recipeDirections == null || recipeDirections.isEmpty()) {
            // If recipe directions are empty or null, then start a new task to fetch the directions
            AllRecipeAsyncTask syncTask = new AllRecipeAsyncTask(getActivity());
            syncTask.execute(recipeUrl, Long.toString(recipeId));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize member variables
        mContext = getActivity();
        mContentResolver = mContext.getContentResolver();

        // Initialize CursorLoader
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);
    }
}
