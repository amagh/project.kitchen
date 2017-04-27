package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
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

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.view.SlidingAlphabeticalIndex;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentFavorites extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FragmentFavorites.class.getSimpleName();
    private static final int FAVORITES_LOADER = 2;
    private static final String ALPHABET = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Member Variables **/
    private Context mContext;
    private AdapterRecipe mRecipeAdapter;
    private Cursor mCursor;
    private int mPosition;
    private Map<String, Integer> mRecipeIndex;
    private StaggeredGridLayoutManager mLayoutManager;

    // Views bound by ButterKnife
    @BindView(R.id.favorites_sliding_index) SlidingAlphabeticalIndex mIndex;
    @BindView(R.id.favorites_recyclerview) RecyclerView mRecipeRecyclerView;

    public FragmentFavorites() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();
        mRecipeIndex = new HashMap<>();

        mRecipeAdapter = new AdapterRecipe(mContext, new AdapterRecipe.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, AdapterRecipe.RecipeViewHolder viewHolder) {
                boolean resetLayout = !ActivityRecipeList.mDetailsVisible;

                ((RecipeCallBack) getActivity()).onItemSelected(
                        Utilities.getRecipeUrlFromRecipeId(mContext, recipeId),
                        viewHolder
                );

                // Set position to the position of the clicked item
                mPosition = viewHolder.getAdapterPosition();

//                if (resetLayout) setLayoutColumns();
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
                    ((ActivityFavorites) getActivity()).mToolbar.getMenu().clear();
                }
            });
        }

        setLayoutColumns();

        // Set the adapter to the RecyclerView
        mRecipeRecyclerView.setAdapter(mRecipeAdapter);

        // Set the onValueChangedListener to allow for scrolling to the appropriate recipe as the
        // user slides their finger along mIndex
        mIndex.setOnValueChangedListener(new SlidingAlphabeticalIndex.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                scrollToIndex(Character.toString(ALPHABET.charAt(value))
                );
            }
        });

        // Set the Alphabet to be used by the ScrollingAlphabeticalIndex
        mIndex.setAlphabet(ALPHABET);

        return rootView;
    }


    /**
     * Used to interpreting the position of the scroll indicator to find the correct letter to
     * scroll to
     * @param letter Letter the user is scrolling to
     */
    private void scrollToIndex(String letter) {
        mLayoutManager.scrollToPositionWithOffset(mRecipeIndex.get(letter), 0);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Initialize variables for the CursorLoader
        String selection = RecipeEntry.COLUMN_FAVORITE + " = ?";
        String[] selectionArgs = new String[] {Integer.toString(1)};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Initialize and return the CursorLoader
        return new CursorLoader(
                mContext,
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Retrieved the index populated by the ScrollingAlphabeticalIndex
        mRecipeIndex = mIndex.getIndex();

        if (cursor != null && cursor.moveToFirst()) {
            // Instantiate the member variable mCursor
            mCursor = cursor;

            // Create the Map used as the index
            do {
                // Get the first letter of the recipe
                String letter = cursor.getString(RecipeEntry.IDX_RECIPE_NAME).substring(0,1);
                if (mRecipeIndex.get(letter) != -1) {
                    // If the position of the first instance of the letter already exists, then skip
                    continue;
                }

                // Put the position of the first instance of the letter in mRecipeIndex
                mRecipeIndex.put(letter, cursor.getPosition());

            } while (cursor.moveToNext());

            // Reset the Cursor to the first position and swap it into mRecipeAdapter
            cursor.moveToFirst();
            mRecipeAdapter.swapCursor(mCursor);

            // Set the position of any letters that haven't been favorite'd
            int lastPos = cursor.getCount();    // Used to hold the last position that had a favorite'd recipe with the first letter

            for (int i = 1; i < ALPHABET.length() + 1; i++) {
                // Iterate in reverse so that missing letters will jump to the position of the next letter
                String letter = Character.toString(ALPHABET.charAt(ALPHABET.length() - i));

                if (mRecipeIndex.get(letter) == -1) {
                    // If no recipe has been favorite'd that start with that letter, then set it go
                    // go to the same position as the letter after it (e.g. If no N exist, it will
                    // go to the first position of O)
                    mRecipeIndex.put(letter, lastPos);
                } else {
                    // If recipe does exist that starts with that letter, then set it as the last
                    // position to be used for the next unused letter
                    lastPos = mRecipeIndex.get(letter);
                }

            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap a null Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the CursorLoader
        getLoaderManager().initLoader(FAVORITES_LOADER, null, this);
    }

    // CallBack for starting the ActivityRecipeDetails (preparation for master-view flow)
    interface RecipeCallBack {
        void onItemSelected(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder);
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    private void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns;
        if (ActivityRecipeList.mTwoPane && ActivityRecipeList.mDetailsVisible) {
            columns = getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columns = getResources().getInteger(R.integer.recipe_columns);
        }

        // Instantiate the LayoutManager
        mLayoutManager = new StaggeredGridLayoutManager(
                columns,
                StaggeredGridLayoutManager.VERTICAL
        );

        // Set the LayoutManager for the RecyclerView
        mRecipeRecyclerView.setLayoutManager(mLayoutManager);

        AdapterRecipe adapter = ((AdapterRecipe) mRecipeRecyclerView.getAdapter());
        if (adapter != null) {
            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        mLayoutManager.scrollToPositionWithOffset(mPosition, 0);
    }
}
