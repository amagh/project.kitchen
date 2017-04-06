package project.hnoct.kitchen.ui;

import android.content.Context;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * A placeholder fragment containing a simple view.
 */
public class RecipeBookFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = RecipeBookFragment.class.getSimpleName();
    private static final int RECIPE_BOOK_LOADER = 3;

    /** Member Variables **/
    Context mContext;
    Cursor mCursor;
    RecipeBookAdapter mRecipeBookAdapter;
    CursorManager mCursorManager;
    int mPosition;

    @BindView(R.id.recipe_book_recyclerview) RecyclerView mRecyclerView;

    public RecipeBookFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipebook, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getActivity();
        mCursorManager = new CursorManager(mContext);

        mRecipeBookAdapter = new RecipeBookAdapter(mContext, new RecipeBookAdapter.RecipeBookAdapterOnClickHandler() {
            @Override
            public void onClick(RecipeBookAdapter.RecipeBookViewHolder viewHolder, long bookId) {
                // Start the ChapterActivity when a specific recipe is clicked
                Intent intent = new Intent(mContext, ChapterActivity.class);

                // Set the Uri for the recipe book as the data to pass to the Activity
                Uri bookUri = RecipeBookEntry.buildUriFromRecipeBookId(bookId);
                intent.setData(bookUri);

                // Launch the ChapterActivity from the Intent
                startActivity(intent);
            }
        }, mCursorManager);

        setLayoutColumns();

        mRecyclerView.setAdapter(mRecipeBookAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri bookUri = RecipeBookEntry.CONTENT_URI;
        String sortOrder = RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME + " ASC";

        return new CursorLoader(
                mContext,
                bookUri,
                RecipeBookEntry.PROJECTION,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "in onLoadFinished");
        if ((mCursor = cursor) != null & mCursor.moveToFirst()) {
            mRecipeBookAdapter.swapCursor(mCursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecipeBookAdapter.swapCursor(null);
        mCursorManager.closeAllCursors();
    }

    @Override
    public void onStop() {
        super.onStop();
        mCursorManager.closeAllCursors();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(RECIPE_BOOK_LOADER, null, this);
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns;
        if (RecipeListActivity.mTwoPane && RecipeListActivity.mDetailsVisible) {
            columns = getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columns = getResources().getInteger(R.integer.recipe_columns);
        }

        // Instantiate the LayoutManager
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(
                columns,
                StaggeredGridLayoutManager.VERTICAL
        );

        // Set the LayoutManager for the RecyclerView
        mRecyclerView.setLayoutManager(sglm);

        RecipeAdapter adapter = ((RecipeAdapter) mRecyclerView.getAdapter());
        if (adapter != null) {
            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        sglm.scrollToPositionWithOffset(mPosition, 0);
    }
}
