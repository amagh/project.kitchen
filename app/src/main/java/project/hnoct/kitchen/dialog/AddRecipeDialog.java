package project.hnoct.kitchen.dialog;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.ui.ActivityRecipeList;

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
    @Nullable @BindView(R.id.dialog_add_recipe_buttons) LinearLayout mButtonLayout;
    @Nullable @BindView(R.id.dialog_add_recipe_search_layout) LinearLayout mSearchLayout;
    @Nullable @BindView(R.id.dialog_add_recipe_search) EditText mSearchEditText;
    @Nullable @BindView(R.id.dialog_add_recipe_search_icon) ImageView mSearchButton;
    @Nullable @BindView(R.id.dialog_add_recipe_favorites) LinearLayout mFavoritesButton;
    @Nullable @BindView(R.id.dialog_add_recipe_my_recipes) LinearLayout mMyRecipesButton;
    @Nullable @BindView(R.id.dialog_add_recipe_favorites_icon) ImageView mFavoritesIcon;
    @Nullable @BindView(R.id.dialog_add_recipe_my_recipes_icon) ImageView mMyRecipesIcon;

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
                            // Utilize Callback interface to send information about recipe that was
                            // selected
                            mListener.onRecipeSelected(recipeUrl);

                            // Close the Cursor
                            if (mCursor != null) mCursor.close();

                            dismiss();
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
     * Sets the member chapterId so that it can be checked whether the chapter already contains
     * a selected recipe
     * @param chapterId
     */
    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    @OnClick(R.id.dialog_add_recipe_favorites)
    void onClickFavorites(View view) {
        if (!favoritesOnly) {
            favoritesOnly = true;
            myRecipesOnly = false;
            swapCursorFavorites();
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_on_normal_holo_light);
            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes);
        } else {
            favoritesOnly = false;
            swapCursorNoFilters();
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_off_normal_holo_light);
        }
    }

    @OnClick(R.id.dialog_add_recipe_my_recipes)
    void onClickMyRecipes(View view) {
        if (!myRecipesOnly) {
            myRecipesOnly = true;
            favoritesOnly = false;
            swapCursorMyRecipes();
            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes_selected);
            mFavoritesIcon.setImageResource(R.drawable.btn_rating_star_off_normal_holo_light);
        } else {
            myRecipesOnly = false;
            swapCursorNoFilters();
            mMyRecipesIcon.setImageResource(R.drawable.ic_my_recipes);
        }
    }

    @OnClick(R.id.dialog_add_recipe_search_icon)
    void onClickSearch(View view) {
        if (search) {
            search = false;
            mSearchButton.setImageResource(R.drawable.ic_menu_search);
            mSearchEditText.setText("");
            if (favoritesOnly) {
                swapCursorFavorites();
            } else if (myRecipesOnly) {
                swapCursorMyRecipes();
            } else {
                swapCursorNoFilters();
            }
        } else {
            search = true;
            String searchTerm = mSearchEditText.getText().toString();
            if (favoritesOnly) {
                searchFavorites(searchTerm);
            } else if (myRecipesOnly) {
                searchMyRecipes(searchTerm);
            } else {
                search(searchTerm);
            }

            mSearchButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
        }

    }

    @OnEditorAction(R.id.dialog_add_recipe_search)
    boolean onEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            search = true;
            String searchTerm = mSearchEditText.getText().toString();
            if (favoritesOnly) {
                searchFavorites(searchTerm);
            } else if (myRecipesOnly) {
                searchMyRecipes(searchTerm);
            } else {
                search(searchTerm);
            }

            mSearchButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);

            return true;
        }

        return false;
    }

    private void swapCursorNoFilters() {
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";
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

    private void search(String searchTerm) {
        // Query the database for all recipes sorting by favorites and then date added
        String selection = RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";
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

    private void searchFavorites(String searchTerm) {
        String selection = RecipeEntry.COLUMN_FAVORITE + " = ? AND " +
                RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"1", "%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        mCursor = cursor;

        mRecipeAdapter.swapCursor(mCursor);
    }

    private void searchMyRecipes(String searchTerm) {
        String selection = RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " LIKE ? AND " +
                RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
        String[] selectionArgs = new String[] {"*%", "%" + searchTerm + "%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        mCursor = cursor;

        mRecipeAdapter.swapCursor(mCursor);
    }

    private void swapCursorFavorites() {
        myRecipesOnly = false;
        favoritesOnly = true;
        String selection = RecipeEntry.COLUMN_FAVORITE + " = ?";
        String[] selectionArgs = new String[] {"1"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        mCursor = cursor;

        mRecipeAdapter.swapCursor(mCursor);
    }

    private void swapCursorMyRecipes() {
        favoritesOnly = false;
        myRecipesOnly = true;
        String selection = RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " LIKE ?";
        String[] selectionArgs = new String[] {"*%"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        Cursor cursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );

        mCursor = cursor;

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
