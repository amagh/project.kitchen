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
public class RecipeBookActivityFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final int RECIPE_BOOK_LOADER = 3;

    /** Member Variables **/
    Context mContext;
    Cursor mCursor;
    RecipeBookAdapter mRecipeBookAdapter;
    CursorManager mCursorManager;

    @BindView(R.id.recipe_book_recyclerview) RecyclerView mRecyclerView;

    public RecipeBookActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipe_book, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getActivity();
        mCursorManager = new CursorManager();

        mRecipeBookAdapter = new RecipeBookAdapter(mContext, new RecipeBookAdapter.RecipeBookAdapterOnClickHandler() {
            @Override
            public void onClick(RecipeBookAdapter.RecipeBookViewHolder viewHolder, int position) {

            }
        }, mCursorManager);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri recipeBookUri = RecipeBookEntry.CONTENT_URI;
        String sortOrder = RecipeBookEntry.TABLE_NAME + " ASC";

        return new CursorLoader(
                mContext,
                recipeBookUri,
                RecipeBookEntry.PROJECTION,
                null,
                null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if ((mCursor = cursor) != null & mCursor.moveToFirst()) {
            mRecipeBookAdapter.swapCursor(mCursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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
}
