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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * A placeholder fragment containing a simple view.
 */
public class ChapterFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = ChapterFragment.class.getSimpleName();
    private static final int CHAPTER_LOADER = 4;
    static final String RECIPE_BOOK_URI = "recipe_book_uri";
    private static final int POSITION_MODIFIER = 10000;

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
        setHasOptionsMenu(true);

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

        ((ChapterActivity) getActivity()).setChapterListener(new ChapterActivity.ChapterListener() {
            @Override
            public void onNewChapter() {
                restartLoader();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_chapter_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_mode: {
                if (mChapterAdapter.isInEditMode()) {
                    mChapterAdapter.exitEditMode();
                } else {
                    mChapterAdapter.enterEditMode();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(CHAPTER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mRecipeBookUri != null && id == CHAPTER_LOADER) {
            // Get the recipe book ID that is being queried from the Uri passed
            long recipeBookId = RecipeBookEntry.getRecipeBookIdFromUri(mRecipeBookUri);

            // Instantiate the parameters used to query the database
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
        } else if (args != null) {
            // The Loader is for Cursors used by RecyclerViews within ChapterAdapter
            // Retrieve the arguments from the passed Bundle
            Uri uri = args.getParcelable(Utilities.URI);
            String[] projection = args.getStringArray(Utilities.PROJECTION);
            String selection = args.getString(Utilities.SELECTION);
            String[] selectionArgs = args.getStringArray(Utilities.SELECTION_ARGS);
            String sortOrder = args.getString(Utilities.SORT_ORDER);

            // Return a new CursorLoader with the given parameters
            return new CursorLoader(
                    mContext,
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == CHAPTER_LOADER) {
            if (cursor != null && cursor.moveToFirst()) {
                mCursor = cursor;
                for (int i = 0; i < cursor.getCount(); i++) {
                    // Retrieve the parameters for generating the URI for querying the database
                    long chapterId = cursor.getLong(ChapterEntry.IDX_CHAPTER_ID);
                    long recipeBookId = cursor.getLong(ChapterEntry.IDX_BOOK_ID);

                    // Instantiate the Cursor by querying for the recipes of the chapter
                    Uri recipesOfChapterUri = LinkRecipeBookEntry.buildRecipeUriFromBookAndChapterId(
                            recipeBookId,
                            chapterId
                    );

                    // Initialize parameters for querying the database
                    String[] projection = RecipeEntry.RECIPE_PROJECTION;
                    String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                    String[] selectionArgs = new String[] {Long.toString(chapterId)};
                    String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " ASC";

                    // Pass the parameters to a Bundle to be passed to the new CursorLoaders
                    Bundle args = new Bundle();
                    args.putParcelable(getString(R.string.uri_key), recipesOfChapterUri);
                    args.putStringArray(getString(R.string.projection_key), projection);
                    args.putString(getString(R.string.selection_key), selection);
                    args.putStringArray(getString(R.string.selection_args_key), selectionArgs);
                    args.putString(getString(R.string.sort_order_key), sortOrder);

                    // Initialize a new Loader
                    generateHelperLoader(i, args);

                    cursor.moveToNext();
                }
            }

            // After all the other required Cursors have been initialized, swap the Cursor into
            // ChapterAdapter
            mChapterAdapter.swapCursor(mCursor);
        } else {
            mCursorManager.addManagedCursor(loader.getId() - POSITION_MODIFIER, cursor);
        }
    }

    /**
     * Generates a CursorLoader for the embedded RecyclerViews
     * @param position Position of the ViewHolder requesting the Cursor
     * @param args Parameters used to generate the CursorLoader
     */
    void generateHelperLoader(int position, Bundle args) {
        // Check whether the CursorLoader has already been started
        if (getLoaderManager().getLoader(position + POSITION_MODIFIER) != null) {
            // If it has already been started, restart it
            getLoaderManager().restartLoader(position + POSITION_MODIFIER, args, this);
        } else {
            // If it has not been started, initialize a new CursorLoader
            getLoaderManager().initLoader(position + POSITION_MODIFIER, args, this);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap a null Cursor into ChapterAdapter
        mChapterAdapter.swapCursor(null);

//        // Close all previously opened Cursors used by the ChapterAdapter
//        mCursorManager.closeAllCursors();
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
