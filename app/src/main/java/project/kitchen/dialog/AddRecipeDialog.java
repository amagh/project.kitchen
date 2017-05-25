package project.kitchen.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.RuntimeExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;
import project.kitchen.ui.adapter.AdapterRecipe;
import project.kitchen.ui.ActivityRecipeList;

/**
 * Created by hnoct on 4/5/2017.
 */

public class AddRecipeDialog extends DialogFragment {
    /** Constants **/

    private AdapterRecipe mRecipeAdapter;
    private SelectionListener mListener;
    private Cursor mCursor;
    Context mContext;
    private boolean favoritesOnly = false;
    private boolean myRecipesOnly = false;
    private boolean search = false;
    private int chapterId;

    // Views Bound by ButterKnife
    @BindView(R.id.dialog_add_recipe_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.dialog_add_recipe_buttons) LinearLayout mButtonLayout;
    @BindView(R.id.dialog_add_recipe_search_layout) LinearLayout mSearchLayout;
    @BindView(R.id.dialog_add_recipe_search) EditText mSearchEditText;
    @BindView(R.id.dialog_add_recipe_search_icon) ImageView mSearchButton;
    @BindView(R.id.dialog_add_recipe_favorites) LinearLayout mFavoritesButton;
    @BindView(R.id.dialog_add_recipe_my_recipes) LinearLayout mMyRecipesButton;
    @BindView(R.id.dialog_add_recipe_favorites_icon) ImageView mFavoritesIcon;
    @BindView(R.id.dialog_add_recipe_my_recipes_icon) ImageView mMyRecipesIcon;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Initialize the Builder for the Dialg
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the view to be used
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_recyclerview, null);
        ButterKnife.bind(this, view);

        mRecipeAdapter = new AdapterRecipe(
                getActivity(),
                new AdapterRecipe.RecipeAdapterOnClickHandler() {
                    @Override
                    public void onClick(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
                        if (mListener != null) {
                            // Retrieve the recipeId
                            long recipeId = Utilities.getRecipeIdFromUrl(getActivity(), recipeUrl);

                            // Check if the chapter already contains the recipe
                            if (isRecipeInChapter(recipeId)) {
                                // Chapter contains recipe, show a Toast to inform the user
                                Toast.makeText(getActivity(), "Recipe already in chapter!", Toast.LENGTH_LONG).show();
                            } else {
                                // Utilize Callback interface to send information about recipe that was
                                // selected
                                mListener.onRecipeSelected(recipeUrl);

                                // Close the Cursor
                                if (mCursor != null) mCursor.close();

                                dismiss();
                            }
                        }
                    }
                }
        );

        // Get the number of columns to be used in the RecyclerView
        int columns = (ActivityRecipeList.mTwoPane && ActivityRecipeList.mDetailsVisible) ?
                getResources().getInteger(R.integer.recipe_twopane_columns) :
                getResources().getInteger(R.integer.recipe_columns);

        // Initialize the StaggeredLayoutManager for the RecyclerView
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);

        // Set the LayoutManager and the Adapter for the RecyclerView
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setAdapter(mRecipeAdapter);

        // Query the database for all recipes sorting by favorites and then date added
        swapCursorNoFilters();

        // Show the normally hidden layouts for search and filter buttons
        mButtonLayout.setVisibility(View.VISIBLE);
        mSearchLayout.setVisibility(View.VISIBLE);

        // Set the View to be displayed
        builder.setView(view);

        return builder.create();
    }

    /**
     * Checks whether the chapter already contains a given recipe
     * @param recipeId Recipe ID of the recipe to check if it is already in the chapter
     * @return Boolean value for whether the chapter contains the recipe
     */
    private boolean isRecipeInChapter(long recipeId) {
        // Set up the selection and selection args for the Cursor
        String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ? AND " +
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ?";
        String[] selectionArgs = new String[] {Integer.toString(chapterId), Long.toString(recipeId)};

        // Generate a Cursor filtering for chapter and recipeId
        Cursor cursor = getActivity().getContentResolver().query(
                LinkRecipeBookEntry.CONTENT_URI,
                LinkRecipeBookEntry.PROJECTION,
                selection,
                selectionArgs,
                null
        );

        // Check if the Cursor contains any entries
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Cursor found a match
                cursor.close();
                return true;
            } else {
                // Cursor did not find a match
                cursor.close();
                return false;
            }
        } else {
            // Invalid query
            return false;
        }
    }

    /**
     * Sets the member chapterId so that it can be checked whether the chapter already contains
     * a selected recipe
     * @param chapterId ID of the chapter attempting to add a recipe
     */
    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    @OnClick(R.id.dialog_add_recipe_favorites)
    void onClickFavorites(View view) {
        // Check if already filtering for favorites
        if (!favoritesOnly) {
            // Not yet filtered, so filter for favorites
            favoritesOnly = true;

            // Set the boolean for filtering for custom-recipes to false
            myRecipesOnly = false;

            // Generate and swap the new Cursor in
            swapCursorFavorites();

            // Changed the icons so user has visual representation for what filter is being applied
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_on_normal_holo_light);
            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes);
        } else {
            // Disable filter by favorites
            favoritesOnly = false;

            // Reset the Cursor to return all recipes
            swapCursorNoFilters();

            // Reset the icon to indicate no filter is being applied
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_off_normal_holo_light);
        }
    }

    /**
     * @see #onClickFavorites
     * @param view View being clicked
     */
    @OnClick(R.id.dialog_add_recipe_my_recipes)
    void onClickMyRecipes(View view) {
        if (!myRecipesOnly) {
            // Filter for custom-recipes
            myRecipesOnly = true;
            favoritesOnly = false;

            swapCursorMyRecipes();

            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes_selected);
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_off_normal_holo_light);
        } else {
            // Disable filter
            myRecipesOnly = false;

            swapCursorNoFilters();

            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes);
        }
    }

    @OnClick(R.id.dialog_add_recipe_search_icon)
    void onClickSearch(View view) {
        // Check whether user is already in search mode
        if (search) {
            // Already searching, so disable filter
            search = false;

            // Set the icon for a new  search
            mSearchButton.setImageResource(R.drawable.ic_menu_search);

            // Clear mSearchEditText to prepare for new input
            mSearchEditText.setText("");

            // Check if a filter is already being applied
            if (favoritesOnly) {
                // Return to showing all favorites
                swapCursorFavorites();
            } else if (myRecipesOnly) {
                // Return to showing all custom-recipes
                swapCursorMyRecipes();
            } else {
                // Return to showing all recipes
                swapCursorNoFilters();
            }
        } else {
            // User has input a term and clicked search, set the boolean to true
            search = true;

            // Retrieve the searchTerm from the EditText
            String searchTerm = mSearchEditText.getText().toString();

            // Check if a filter is already being applied
            if (favoritesOnly) {
                // Search only favorites
                searchFavorites(searchTerm);
            } else if (myRecipesOnly) {
                // Search only custom-recipes
                searchMyRecipes(searchTerm);
            } else {
                // Search all recipes
                search(searchTerm);
            }

            // Set the icon to a clear to allow the user to quickly reset their search
            mSearchButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
        }

    }

    @OnEditorAction(R.id.dialog_add_recipe_search)
    boolean onEditorAction(int actionId) {
        // Check if the user has pressed the search key on the soft keyboard
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
            // User has input a term, set the boolean to true
            search = true;

            // Retrieve the searchTerm from the EditText
            String searchTerm = mSearchEditText.getText().toString();

            // Check if a filter is already being applied
            if (favoritesOnly) {
                // Search only favorites
                searchFavorites(searchTerm);
            } else if (myRecipesOnly) {
                // Search only custom-recipes
                searchMyRecipes(searchTerm);
            } else {
                // Search all recipes
                search(searchTerm);
            }

            // Set the icon to a clear to allow the user to quickly reset their search
            mSearchButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
        }

        return false;
    }

    /**
     * Generates a new Cursor, returning all recipes with no filter, and swaps it into
     * mRecipeAdapter
     */
    private void swapCursorNoFilters() {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Set up the parameters for querying the database
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";

        // Generate a Cursor by querying the database
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                null,
                null,
                sortOrder
        );

        // Swap the Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);
    }

    /**
     * Generates a Cursor, filtering recipes for those that match a search term, and swaps it into
     * mRecipeAdapter
     * @param searchTerm Search term to filter by
     */
    private void search(String searchTerm) {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Initialize parameters for filtering database
        String selection = RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";

        // Generate Cursor by querying the database
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);
    }

    /**
     * Generates a Cursor, filtering the favorites results for those that match a search term, and
     * swaps it into mRecipeAdapter
     * @param searchTerm Search term to filter the favorites results by
     */
    private void searchFavorites(String searchTerm) {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Initialize parameters for the Cursor
        String selection = RecipeEntry.COLUMN_FAVORITE + " = ? AND " +
                RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"1", "%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Generate a Cursor by querying the database
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor into AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);
    }

    /**
     * Generates a Cursor, filtering custom-recipe results for those that match a search term, and
     * swaps it into mRecipeAdapter
     * @param searchTerm Search term to filter custom-recipes results by
     */
    private void searchMyRecipes(String searchTerm) {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Initialize parameters for the Cursor
        String selection = RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " LIKE ? AND " +
                RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"*%", "%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Generate a Cursor by querying the database
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor into mRecipeAdapter
        mRecipeAdapter.swapCursor(mCursor);
    }

    /**
     * Generates a Cursor filtering recipes for only those that are favorites and swaps it into
     * mRecipeAdapter
     */
    private void swapCursorFavorites() {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Initialize parameters for the Cursor
        String selection = RecipeEntry.COLUMN_FAVORITE + " = ?";
        String[] selectionArgs = new String[] {"1"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Generate Cursor
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);
    }

    /**
     * Generates a Cursor, filtering for only custom-recipes and swaps it into mRecipeAdapter
     */
    private void swapCursorMyRecipes() {
        // Close the previous Cursor
        if (mCursor != null) {
            mCursor.close();
        }

        // Parameters for Cursor
        String selection = RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " LIKE ?";
        String[] selectionArgs = new String[] {"*%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Generate Cursor
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // Close the Cursor if it is still opened
        if (mCursor != null) mCursor.close();
    }

    /**
     * Interface for notifying which recipe has been selected
     */
    public interface SelectionListener {
        void onRecipeSelected(String recipeUrl);
    }

    /**
     * Registers an observer to be notified for recipe selection
     * @param listener SelectionListener to be set to the member variable
     */
    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

}
