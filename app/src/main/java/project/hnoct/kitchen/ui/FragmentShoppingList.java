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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterIngredient;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentShoppingList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // Constants
    private static final String LOG_TAG = FragmentShoppingList.class.getSimpleName();
    private final int SHOPPING_LOADER = 5;

    // Member Variables
    private Context mContext;
    private Cursor mCursor;
    private AdapterIngredient mAdapter;
    private LinearLayoutManager mLayoutManager;

    // ButterKnife Bounds Views
    @BindView(R.id.shopping_list_recyclerView) RecyclerView mRecyclerView;

    public FragmentShoppingList() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_list, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        mAdapter = new AdapterIngredient(mContext);
        mAdapter.useAsShoppingList();

        mLayoutManager = new LinearLayoutManager(mContext);

        // Set the Adapter and LayoutManager for the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Set up parameters for creating the CursorLoader
        Uri linkUri = LinkIngredientEntry.CONTENT_URI;
        String[] projection = LinkIngredientEntry.LINK_PROJECTION;
        String selection = LinkIngredientEntry.COLUMN_SHOPPING + " = ?";
        String[] selectionArgs = new String[] {"1"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC, " +
                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " ASC";

        // Create and return the CursorLoader
        return new CursorLoader(mContext,
                linkUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            // Set the member variable as the returned Cursor
            mCursor = cursor;

            // Swap mCursor into mAdapter
            mAdapter.swapCursor(mCursor);

            if (mAdapter.getToggleStatus()) {
                mAdapter.toggleChecked();
            }

            mAdapter.addRecipeTitles();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap in a null Cursor
        mAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initiailize the CursorLoader
        getLoaderManager().initLoader(SHOPPING_LOADER, null, this);
    }
}
