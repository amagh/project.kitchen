package project.hnoct.kitchen.ui;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.dialog.AddRecipeDialog;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChapterFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = ChapterFragment.class.getSimpleName();
    private static final int CHAPTER_LOADER = 4;
    static final String RECIPE_BOOK_URI = "recipe_book_uri";

    /** Member Variables **/
    Context mContext;
    Cursor mCursor;
    CursorManager mCursorManager;
    Uri mRecipeBookUri;
    ChapterAdapter mChapterAdapter;
    int mPosition;

    @BindView(R.id.chapter_recyclerview) RecyclerView mRecyclerView;

    public ChapterFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chapter, container, false);
        ButterKnife.bind(this, view);

        // Instantiate member variables
        mContext = getActivity();

        // Get the Uri of the recipe book to be accessed
        Bundle bundle = getArguments();
        if (bundle != null) {
            mRecipeBookUri = bundle.getParcelable(RECIPE_BOOK_URI);
        }

        mChapterAdapter = new ChapterAdapter(
                mContext,
                getChildFragmentManager(),
                mCursorManager = new CursorManager(mContext)
        );

        // Set the layout manager and the number of columns to use
        setLayoutColumns();

        mRecyclerView.setAdapter(mChapterAdapter);
        mChapterAdapter.setRecipeClickListener(new ChapterAdapter.RecipeClickListener() {
            @Override
            public void onRecipeClicked(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder) {
                String recipeUrl = Utilities.getRecipeUrlFromRecipeId(mContext, recipeId);
                ((ChapterActivity) getActivity()).onRecipeSelected(recipeUrl, viewHolder);
            }

            @Override
            public void onAddRecipeClicked(long chapterId) {
                ((ChapterActivity) getActivity()).onAddRecipeClicked(chapterId);
            }
        });

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mRecipeBookUri != null) {
            Log.d(LOG_TAG, "in onCreateLoader");
            // Get the recipe book ID that is being queried from the Uri passed
            long recipeBookId = RecipeBookEntry.getRecipeBookIdFromUri(mRecipeBookUri);

            // Instantiate the parameters used to query the database
//            Uri uri = LinkRecipeBookTable.CONTENT_URI;
//            String selection = RecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
//            String[] selectionArgs = new String[] {Long.toString(recipeBookId)};
//            String sortOrder = ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

            Uri uri = ChapterEntry.CONTENT_URI;
            String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
            String[] selectionArgs = new String[] {Long.toString(recipeBookId)};
            String sortOrder = ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

            // Build and return the CursorLoader
            return new CursorLoader(
                    mContext,
                    uri,
                    ChapterEntry.PROJECTION,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        Log.v(LOG_TAG, "in onLoadFinished");
        mCursor = cursor;
        mChapterAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mChapterAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the Cursor Loader
        getLoaderManager().initLoader(CHAPTER_LOADER, null, this);
    }

    interface RecipeCallBack {
        void onRecipeSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder);
        void onAddRecipeClicked(long chapterId);
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
