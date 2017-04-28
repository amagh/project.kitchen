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
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.ui.adapter.RecipeItemAnimator;
import project.hnoct.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

/**
 * Fragment for the main view displaying all recipes loaded from web
 */
public class FragmentRecipeList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final int RECIPE_LOADER = 0;

    /** Member Variables **/
    private Context mContext;                   // Interface for global context
    Cursor mCursor;
    private ContentResolver mResolver;          // Reference to ContentResolver
    AdapterRecipe mRecipeAdapter;
    private int mPosition;                      // Position of mCursor
    StaggeredGridLayoutManager mStaggeredLayoutManager;

    // Views bound by ButterKnife
    @BindView(R.id.recipe_recycler_view) RecyclerView mRecipeRecyclerView;

    public FragmentRecipeList() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Bind views using ButterKnife
        ButterKnife.bind(this, rootView);

        // Instantiate member variables
        mContext = getActivity();

        // Instantiate the Adapter for the RecyclerView
        mRecipeAdapter = new AdapterRecipe(getActivity(), new AdapterRecipe.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, AdapterRecipe.RecipeViewHolder viewHolder) {


                // Set position to the position of the clicked item
                mPosition = viewHolder.getAdapterPosition();

                if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {
                    // If using the detail fragment within AdapterRecipe, do not launch a new
                    // FragmentRecipeDetails
                    return;
                } else {
                    // Initiate Callback to Activity which will launch Details Activity
                    ((RecipeCallBack) getActivity()).onItemSelected(
                            Utilities.getRecipeUrlFromRecipeId(mContext, recipeId),
                            viewHolder
                    );

                    setLayoutColumns();
                }



            }
        });

        mRecipeAdapter.setHasStableIds(true);

        // Set whether the RecyclerAdapter should utilize the detail layout
        boolean useDetailView = getResources().getBoolean(R.bool.recipeAdapterUseDetailView);
        mRecipeAdapter.setUseDetailView(useDetailView, getChildFragmentManager());
        if (useDetailView) {
            mRecipeAdapter.setVisibilityListener(new AdapterRecipe.DetailVisibilityListener() {
                @Override
                public void onDetailsHidden() {
                    ((ActivityRecipeList) getActivity()).mToolbar.getMenu().clear();
                }
            });
        }

        // The the number of columns that will be used for the view
        setLayoutColumns();

        // Set the adapter to the RecyclerView
        mRecipeRecyclerView.setAdapter(mRecipeAdapter);

        // Initialize and set the RecipeItemAnimator
        RecipeItemAnimator animator = new RecipeItemAnimator(mContext);
        animator.setRecipeAnimatorListener(new RecipeItemAnimator.RecipeAnimatorListener() {
            @Override
            public void onFinishAnimateDetail() {
                // Listen for when the animation for the detail fragment inflation has been
                // completed so that the view can be scrolled to the top of the recipe's view
                mRecipeRecyclerView.smoothScrollToPosition(mPosition);
            }
        });
        mRecipeRecyclerView.setItemAnimator(animator);

        // Initialize the listener for when the user attempts to search for a recipe
        ((ActivityRecipeList) getActivity()).setSearchListener(new ActivityRecipeList.SearchListener() {
            @Override
            public void onSearch(String searchTerm) {
                search(searchTerm);
            }
        });

        return rootView;
    }

    /**
     * Reloads the CursorLoader to only show results that include the user's search term
     * @param searchTerm The user-input search term for the recipes they wish to find
     */
    private void search(String searchTerm) {
        // Initialize the selection argument, passing in the searchTerm
        String[] selectionArgs = new String[] {"%" + searchTerm + "%"};

        // Create a Bundle and add the search argument to it
        Bundle args = new Bundle();
        args.putStringArray(getString(R.string.selection_args_key), selectionArgs);

        // Restart the CursorLoader and pass the Bundle as an argument
        getLoaderManager().restartLoader(RECIPE_LOADER, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Get the URI for the recipe table
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        // Set the column projection (All columns for now)
        String[] projection = RecipeEntry.RECIPE_PROJECTION;

        // Set the sort order to newest recipes first
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";

        String selection = null;
        String[] selectionArgs = null;

        if (args != null) {
            // If arguments have been passed, set the new selection and selection arguments
            selection = RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
            selectionArgs = args.getStringArray(getString(R.string.selection_args_key));
        }

        // Return CursorLoader set to recipe table
        return new CursorLoader(mContext,
                recipeUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Set member variable mCursor to the loaded Cursor
        mCursor = cursor;

        // Swap in the loaded Cursor into the Adapter
        mRecipeAdapter.swapCursor(mCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap null in for cursor adapter to clear view
        mRecipeAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();
        mResolver = mContext.getContentResolver();
        /* Constants */

        getLoaderManager().initLoader(RECIPE_LOADER, null, this);
    }

    interface RecipeCallBack {
        void onItemSelected(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder);
    }


    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns;
        if (ActivityRecipeList.mTwoPane && ActivityRecipeList.mDetailsVisible) {
            columns = getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columns = getResources().getInteger(R.integer.recipe_columns);
        }

        if (mRecipeRecyclerView.getLayoutManager() == null) {
            // Instantiate the LayoutManager
            mStaggeredLayoutManager = new StaggeredGridLayoutManagerWithSmoothScroll(
                    columns,
                    StaggeredGridLayoutManager.VERTICAL
            );

            // Set the LayoutManager for the RecyclerView
            mRecipeRecyclerView.setLayoutManager(mStaggeredLayoutManager);

        } else {
//            mStaggeredLayoutManager =
//                    (StaggeredGridLayoutManagerWithSmoothScroll) mRecipeRecyclerView
//                            .getLayoutManager();
            mStaggeredLayoutManager.setSpanCount(columns);
        }


        AdapterRecipe adapter = ((AdapterRecipe) mRecipeRecyclerView.getAdapter());
        if (adapter != null) {
            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        if (ActivityRecipeList.mTwoPane) {
            mRecipeRecyclerView.smoothScrollToPosition(mPosition);
        } else {

        }
    }
}
