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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Fragment for the main view displaying all recipes loaded from web
 */
public class RecipeListFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private final int RECIPE_LOADER = 0;

    /** Member Variables **/
    Context mContext;                   // Interface for global context
    Cursor mCursor;
    ContentResolver mResolver;          // Reference to ContentResolver
    static RecipeAdapter mRecipeAdapter;
    int mPosition;                      // Position of mCursor

    // Views bound by ButterKnife
    @BindView(R.id.recipe_recycler_view) RecyclerView mRecipeRecyclerView;

    public RecipeListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Bind views using ButterKnife
        ButterKnife.bind(this, rootView);

        // Instantiate the Adapter for the RecyclerView
        mRecipeAdapter = new RecipeAdapter(getActivity(), new RecipeAdapter.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder) {
                // Initiate Callback to Activity which will initiate launch of new Details Activity
                ((RecipeCallBack) getActivity()).onItemSelected(
                        Utilities.getRecipeUrlFromRecipeId(mContext, recipeId),
                        viewHolder
                );

                // Set position to the position of the clicked item
                mPosition = viewHolder.getAdapterPosition();
            }
        });

        // Set LayoutManager
        LinearLayoutManager llm = new LinearLayoutManager(mContext);
        mRecipeRecyclerView.setLayoutManager(llm);

        // Set the adapter to the RecyclerView
        mRecipeRecyclerView.setAdapter(mRecipeAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Get the URI for the recipe table
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        // Set the column projection (All columns for now)
        String[] projection = RecipeEntry.RECIPE_PROJECTION;

        // Set the sort order to newest recipes first
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";

        // Return CursorLoader set to recipe table
        return new CursorLoader(mContext,
                recipeUri,
                projection,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Swap in the loaded Cursor into the Adapter
        mRecipeAdapter.swapCursor(cursor);
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
        getLoaderManager().initLoader(RECIPE_LOADER, null, this);
    }

    interface RecipeCallBack {
        void onItemSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder);
    }
}
