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
    private static final int POSITION_MODIFIER = 100000;

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private RecipeBookAdapter mRecipeBookAdapter;
    private CursorManager mCursorManager;
    private int mPosition;

    @BindView(R.id.recipe_book_recyclerview) RecyclerView mRecyclerView;

    public RecipeBookFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipebook, container, false);
        ButterKnife.bind(this, rootView);

        // Instantiate the member variables
        mContext = getActivity();
        mCursorManager = new CursorManager(mContext);

        // Initialize mRecipeBookAdapter
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

        // Set the LayoutManager
        setLayoutColumns();

        // Set mRecipeBookAdapter to mRecyclerView
        mRecyclerView.setAdapter(mRecipeBookAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Check whether the main CursorLoader for mRecipeBookAdapter is being initialized or for
        // one of the embedded RecyclerViews
        if (id == RECIPE_BOOK_LOADER) {
            // For main CursorLoader
            Uri bookUri = RecipeBookEntry.CONTENT_URI;
            String sortOrder = RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME + " ASC";

            // Build and return the CursorLoader from the parameters
            return new CursorLoader(
                    mContext,
                    bookUri,
                    RecipeBookEntry.PROJECTION,
                    null,
                    null,
                    sortOrder
            );
        } else if (args != null) {
            // For helper CursorLoaders
            // Retrieve the parameters from the Bundle with which to generate the CursorLoader
            Uri bookWithChapterUri = args.getParcelable(getString(R.string.uri_key));
            String[] projection = args.getStringArray(getString(R.string.projection_key));
            String sortOrder = args.getString(getString(R.string.sort_order_key));

            // Build and return a CursorLoader with the given parameters
            return new CursorLoader(
                    mContext,
                    bookWithChapterUri,
                    projection,
                    null,
                    null,
                    sortOrder
            );
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == RECIPE_BOOK_LOADER) {
            // Main Cursor for the RecipeBookAdapter
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    // Retrieve the bookId to query for chapters
                    long bookId = cursor.getLong(RecipeBookEntry.IDX_BOOK_ID);

                    // Initialize the parameters to query the database
                    Uri bookUri = LinkRecipeBookEntry.buildChapterUriFromRecipeBookId(bookId);
                    String[] projection = LinkRecipeBookEntry.PROJECTION;
                    String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " ASC, " + ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

                    // Create a Bundle and add the arguments needed to generate the Cursor for the
                    // requesting recipe book
                    Bundle args = new Bundle();
                    args.putParcelable(getString(R.string.uri_key), bookUri);
                    args.putStringArray(getString(R.string.projection_key), projection);
                    args.putString(getString(R.string.sort_order_key), sortOrder);

                    // Initialize a new Loader with a position modifier so that its position in the
                    // Adapter is maintained
                    generateHelperLoader(i, args);

                    // Move the Cursor to the next position to prepare for the next iteration
                    cursor.moveToNext();
                }
            }

            // Swap the Cursor into the RecipeBookAdapter
            mCursor = cursor;
            mRecipeBookAdapter.swapCursor(mCursor);

        } else {
            // If Cursor is for an embedded RecyclerView, add the Cursor to the CursorManager
            mCursorManager.addManagedCursor(loader.getId() - POSITION_MODIFIER, cursor);
        }
    }

    /**
     * Generates a CursorLoader for the embedded RecyclerViews
     * @param position Position of the ViewHolder requesting the Cursor
     * @param args Parameters used to generate the CursorLoader
     */
    private void generateHelperLoader(int position, Bundle args) {
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
        // Swap in a null Cursor
        mRecipeBookAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the CursorLoader for mRecipeBookAdapter
        getLoaderManager().initLoader(RECIPE_BOOK_LOADER, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(RECIPE_BOOK_LOADER, null, this);
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    private void setLayoutColumns() {
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
