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
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.sync.AllRecipesAsyncTask;
import project.hnoct.kitchen.sync.RecipeImporter;

/**
 * A placeholder fragment containing a simple view.
 */
public class RecipeDetailsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = RecipeDetailsFragment.class.getSimpleName();
    private static final int DETAILS_LOADER = 1;
    public static final String RECIPE_DETAILS_URI = "recipe_and_ingredients_uri";
    public static final String RECIPE_DETAILS_URL = "recipe_url";

    // Projection of columns to query
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
            LinkEntry.COLUMN_QUANTITY,
            LinkEntry.COLUMN_INGREDIENT_ORDER
    };

    // Column index for the projection
    static final int IDX_RECIPE_ID = 0;
    static final int IDX_RECIPE_NAME = 1;
    static final int IDX_RECIPE_AUTHOR = 2;
    static final int IDX_IMG_URL = 3;
    static final int IDX_RECIPE_URL = 4;
    static final int IDX_SHORT_DESC = 5;
    static final int IDX_RECIPE_RATING = 6;
    static final int IDX_RECIPE_REVIEWS = 7;
    static final int IDX_RECIPE_DIRECTIONS = 8;
    static final int IDX_RECIPE_FAVORITE = 9;
    static final int IDX_RECIPE_SERVINGS = 10;
    static final int IDX_RECIPE_CALORIES = 11;
    static final int IDX_RECIPE_FAT = 12;
    static final int IDX_RECIPE_CARBS = 13;
    static final int IDX_RECIPE_PROTEIN = 14;
    static final int IDX_RECIPE_CHOLESTEROL = 15;
    static final int IDX_RECIPE_SODIUM = 16;
    static final int IDX_RECIPE_SOURCE = 17;
    static final int IDX_INGREDIENT_ID = 18;
    static final int IDX_INGREDIENT_NAME = 19;
    static final int IDX_LINK_QUANTITY = 20;
    static final int IDX_LINK_INGREDIENT_ORDER = 21;

    /** Member Variables **/
    Uri mRecipeUri;
    String mRecipeUrl;
    long mRecipeId;
    Context mContext;
    Cursor mCursor;
    ContentResolver mContentResolver;
    IngredientAdapter mIngredientAdapter;
    DirectionAdapter mDirectionAdapter;
    NutritionAdapter mNutritionAdapter;
    boolean mSyncing = false;

    // Views bound by ButterKnife
    @BindView(R.id.details_ingredient_recycler_view) RecyclerView mIngredientsRecyclerView;
    @BindView(R.id.details_direction_recycler_view) RecyclerView mDirectionsRecyclerView;
    @BindView(R.id.details_recipe_image) ImageView mRecipeImageView;
    @BindView(R.id.details_recipe_title_text) TextView mRecipeTitleText;
    @BindView(R.id.details_recipe_author_text) TextView mRecipeAuthorText;
    @BindView(R.id.details_recipe_attribution_text) TextView mRecipeAttributionText;
    @BindView(R.id.details_recipe_reviews_text) TextView mRecipeReviewsText;
    @BindView(R.id.details_ratings_text) TextView mRecipeRatingText;
    @BindView(R.id.details_recipe_short_description_text) TextView mRecipeShortDescriptionText;
    @BindView(R.id.details_ingredient_title_text) TextView mIngredientTitleText;
    @BindView(R.id.details_direction_title_text) TextView mDirectionTitleText;
    @BindView(R.id.details_line_separator_top) View mLineSeparatorTop;
    @BindView(R.id.details_line_separator_bottom) View mLineSeparatorBottom;
    @BindView(R.id.nutrition_drawer_serving_disclosure_text) TextView mNutritionServingDisclosureText;
    @BindView(R.id.nutrition_drawer_calorie_disclosure_text) TextView mNutritionCalorieDiscloureText;
    @BindView(R.id.nutrition_drawer_recycler_view) RecyclerView mNutrientRecyclerView;

    public RecipeDetailsFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        ButterKnife.bind(this, rootView);
        setHasOptionsMenu(true);

        // Initialize member variables
        mContext = getActivity();
        mContentResolver = mContext.getContentResolver();

        if (getArguments() != null) {
            // Get the URL passed from the RecipeListActivity/RecipeDetailsActivity
            mRecipeUri = getArguments().getParcelable(RECIPE_DETAILS_URI);
            if(getArguments().getParcelable(RECIPE_DETAILS_URL) != null) {
                mRecipeUrl = getArguments().getParcelable(RECIPE_DETAILS_URL).toString();

                // Get the recipeId and generate recipeUri for database
                mRecipeId = Utilities.getRecipeIdFromUrl(mContext, mRecipeUrl);
                mRecipeUri = LinkEntry.buildIngredientUriFromRecipe(mRecipeId);
            }
        } else {
            Log.d(LOG_TAG, "No bundle found!");
            return rootView;
        }

        // Initialize the IngredientAdapter and DirectionAdapter
        mIngredientAdapter = new IngredientAdapter(getActivity());
        mDirectionAdapter = new DirectionAdapter(getActivity());
        mNutritionAdapter = new NutritionAdapter(getActivity());

        // Initialize and set the LayoutManagers
        LinearLayoutManager llm = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        LinearLayoutManager llm2 = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        LinearLayoutManager llm3 = new LinearLayoutManager(getActivity());
        mIngredientsRecyclerView.setLayoutManager(llm);
        mDirectionsRecyclerView.setLayoutManager(llm2);
        mNutrientRecyclerView.setLayoutManager(llm3);

        // Set the IngredientAdapter for the ingredient's RecyclerView
        mIngredientsRecyclerView.setAdapter(mIngredientAdapter);
        mDirectionsRecyclerView.setAdapter(mDirectionAdapter);
        mNutrientRecyclerView.setAdapter(mNutritionAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort the columns by order that ingredient was added to link table
        String sortOrder = LinkEntry.COLUMN_INGREDIENT_ORDER + " ASC";

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
            // Ensure a Cursor was returned
            mCursor = cursor;
        } else {
            return;
        }

        // Move Cursor to first row or end
        if (!mCursor.moveToFirst()) {
            if (!mSyncing) {
                mSyncing = true;

                // If recipe is missing information, then load details from web
                RecipeImporter.importRecipeFromUrl(mContext, new RecipeImporter.UtilitySyncer() {
                    @Override
                    public void onFinishLoad() {
                        getLoaderManager().restartLoader(DETAILS_LOADER, null, RecipeDetailsFragment.this);
                        mSyncing = false;
                    }
                }, mRecipeUrl);
            }
            return;
        }

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
        int recipeServings = mCursor.getInt(IDX_RECIPE_SERVINGS);

        // Populate the views with the data
        Glide.with(mContext)
                .load(recipeImageUrl)
                .into(mRecipeImageView);
        mRecipeTitleText.setText(recipeTitle);
        mRecipeAuthorText.setText(Utilities.formatAuthor(mContext, recipeAuthor));
        mRecipeAttributionText.setText(recipeSource);
        mRecipeRatingText.setText(Utilities.formatRating(recipeRating));
        mRecipeReviewsText.setText(Utilities.formatReviews(mContext, recipeReviews));
        mRecipeShortDescriptionText.setText(recipeDescription);
        mNutritionServingDisclosureText.setText(
                mContext.getString(R.string.nutrition_info_serving_disclosure, recipeServings)
        );
        mNutritionCalorieDiscloureText.setText(
                mContext.getString(R.string.nutrition_info_disclosure, Utilities.getUserCalories(mContext))
        );

        // Set the visibility of the ingredient and direction section titles to VISIBLE
        mIngredientTitleText.setVisibility(View.VISIBLE);
        mDirectionTitleText.setVisibility(View.VISIBLE);

        // Set visibility of line separators to VISIBLE
        mLineSeparatorTop.setVisibility(View.VISIBLE);
        mLineSeparatorBottom.setVisibility(View.VISIBLE);

        // Swap the Cursor into the Adapter so that data can be displayed in the ingredient list
        mIngredientAdapter.swapCursor(cursor);

        // Set the direction list for the DirectionAdapter so steps can be displayed
        mDirectionAdapter.setDirectionList(Utilities.getDirectionList(recipeDirections));

        // Set the nutrition list for the NutritionAdapter for the slide out drawer
        mNutritionAdapter.setNutritionList(getNutritionList());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mIngredientAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize CursorLoader
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_details_fragment, menu);

        // Query the db to check whether the recipe is a favorite
        Cursor cursor = mContentResolver.query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(mRecipeId)},
                null
        );

        if (cursor.moveToFirst()) {
            // Instantiate the menu-item associated with favorites
            MenuItem menuFavorite = menu.findItem(R.id.detail_favorites);

            // Set the icon for the favorites action depending on the favorite status of the recipe
            menuFavorite.setIcon(cursor.getInt(RecipeEntry.IDX_FAVORITE) == 1 ?
                    R.drawable.favorite_star_enabled : R.drawable.favorite_star_disabled);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.detail_favorites: {
                // Update the favorite status of the recipe when selected and change the icon accordingly
                boolean favorite = Utilities.setRecipeFavorite(mContext, mRecipeId);
                if (favorite) {
                    item.setIcon(R.drawable.favorite_star_enabled);
                } else {
                    item.setIcon(R.drawable.favorite_star_disabled);
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private List<Pair<Integer, Double>> getNutritionList() {
        // Use linked list to hold nutrition information to keep order the same
        List<Pair<Integer, Double>> nutritionList = new LinkedList<>();

        // Retrieve nutrition information from database utilizing Cursor from CursorLoader
        double calories = mCursor.getDouble(IDX_RECIPE_CALORIES);
        double fatGrams = mCursor.getDouble(IDX_RECIPE_FAT);
        double carbGrams = mCursor.getDouble(IDX_RECIPE_CARBS);
        double proteinGrams = mCursor.getDouble(IDX_RECIPE_PROTEIN);
        double cholesterolMg = mCursor.getDouble(IDX_RECIPE_CHOLESTEROL);
        double sodiumMg = mCursor.getDouble(IDX_RECIPE_SODIUM);

        @RecipeEntry.NutrientType int calorieType = RecipeEntry.NUTRIENT_CALORIE;
        @RecipeEntry.NutrientType int fatType = RecipeEntry.NUTRIENT_FAT;
        @RecipeEntry.NutrientType int carbType = RecipeEntry.NUTRIENT_CARB;
        @RecipeEntry.NutrientType int proteinType = RecipeEntry.NUTRIENT_PROTEIN;
        @RecipeEntry.NutrientType int cholesterolType = RecipeEntry.NUTRIENT_CHOLESTEROL;
        @RecipeEntry.NutrientType int sodiumType = RecipeEntry.NUTRIENT_SODIUM;

        nutritionList.add(new Pair<>(calorieType, calories));
        nutritionList.add(new Pair<>(fatType, fatGrams));
        nutritionList.add(new Pair<>(carbType, carbGrams));
        nutritionList.add(new Pair<>(proteinType, proteinGrams));
        nutritionList.add(new Pair<>(cholesterolType, cholesterolMg));
        nutritionList.add(new Pair<>(sodiumType, sodiumMg));

        return nutritionList;
    }
}
