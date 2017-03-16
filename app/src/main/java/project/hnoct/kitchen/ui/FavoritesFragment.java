package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.view.SlidingAlphabeticalIndex;

/**
 * A placeholder fragment containing a simple view.
 */
public class FavoritesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FavoritesFragment.class.getSimpleName();
    private static final int FAVORITES_LOADER = 2;
    private static final String ALPHABET = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Member Variables **/
    Context mContext;
    RecipeAdapter mRecipeAdapter;
    Cursor mCursor;
    int mPosition;
    Map<String, Integer> mRecipeIndex;
    LayoutInflater mInflater;       // Used to inflate the list_item_alphabet_index layout
    LinearLayoutManager mLayoutManager;

    // Views bound by ButterKnife
    @BindView(R.id.favorites_index) SlidingAlphabeticalIndex mIndex;
    @BindView(R.id.favorites_recycler_view) RecyclerView mRecyclerView;

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();
        mInflater = inflater;
        mRecipeIndex = new HashMap<>();

        mRecipeAdapter = new RecipeAdapter(mContext, new RecipeAdapter.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder) {
                ((RecipeCallBack) getActivity()).onItemSelected(
                        Utilities.getRecipeUrlFromRecipeId(mContext, recipeId),
                        viewHolder
                );
            }
        });

        mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mRecipeAdapter);

        int height;
        mIndex.post(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "Height: " + mIndex.getMeasuredHeight());
            }
        });

        mIndex.setOnValueChangedListener(new SlidingAlphabeticalIndex.OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                scrollToIndex(Character.toString(ALPHABET.charAt(value)));
            }
        });

        populateIndex();

        return rootView;
    }

    /**
     * Initializes the index on the right side of the screen to be used for fast scrolling through
     * the list of favorites
     */
    private void populateIndex() {
        // Init ALPHABET to be used as index


        // Set LayoutParams so that height is set to 0 and uses layout weight instead of evenly distribute
        // index the entire length of mIndex
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        for (int i = 0; i < ALPHABET.length(); i++) {
            // Inflate the view from the layout file
            TextView textView = (TextView) mInflater.inflate(R.layout.list_item_alphabet_index, null);

            // Set the character as the text to show
            textView.setText(Character.toString(ALPHABET.charAt(i)));
            textView.setTag(Character.toString(ALPHABET.charAt(i)));

            // Set OnClickListener so that RecyclerView can jump to the correct index
//            textView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    scrollToIndex((String) view.getTag());
//                }
//            });

            // Set LayoutParams
            textView.setLayoutParams(params);

            // Add the View to mIndex
            mIndex.addView(textView);
            mRecipeIndex.put(Character.toString(ALPHABET.charAt(i)), -1);
        }
    }

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
        if (cursor != null && cursor.moveToFirst()) {
            mCursor = cursor;

            do {
                String letter = cursor.getString(RecipeEntry.IDX_RECIPE_NAME).substring(0,1);
                if (mRecipeIndex.get(letter) != -1) {
                    continue;
                }

                mRecipeIndex.put(letter, cursor.getPosition());

            } while (cursor.moveToNext());

            cursor.moveToFirst();
            mRecipeAdapter.swapCursor(mCursor);

            int lastPos = cursor.getCount();
            for (int i = 1; i < ALPHABET.length() + 1; i++) {
                String letter = Character.toString(ALPHABET.charAt(ALPHABET.length() - i));

                if (mRecipeIndex.get(letter) == -1) {
                    mRecipeIndex.put(letter, lastPos);
                } else {
                    lastPos = mRecipeIndex.get(letter);
                }

            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecipeAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the CursorLoader
        getLoaderManager().initLoader(FAVORITES_LOADER, null, this);
    }

    interface RecipeCallBack {
        void onItemSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder);
    }
}
