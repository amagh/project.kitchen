package project.hnoct.kitchen.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.view.SlidingAlphabeticalIndex;
import project.hnoct.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentFavorites extends FragmentModel implements LoaderManager.LoaderCallbacks<Cursor> {
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
    private StaggeredGridLayoutManagerWithSmoothScroll mStaggeredLayoutManager;
    private boolean animateCard = false;

    // Views bound by ButterKnife
    @BindView(R.id.favorites_sliding_index) SlidingAlphabeticalIndex mIndex;
    @BindView(R.id.fragment_recyclerview) RecyclerView mRecipeRecyclerView;
    @BindView(R.id.favorites_cardview) CardView mCardView;

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

        // Initialize AdapterRecipe and obtain a reference to it
        initRecipeAdapter();
        mRecipeAdapter = getRecipeAdapter();

        // Set the Adapter to animate un-favoriting items
        mRecipeAdapter.inFavoriteView();

        // Init the StaggeredLayoutManager and obtain a reference to it
        setLayoutColumns();
        mStaggeredLayoutManager = getStaggeredLayoutManager();

        // Init the RecyclerView and obtain a reference to it
        initRecyclerView();
        mRecipeRecyclerView = getRecyclerView();

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
        mStaggeredLayoutManager.scrollToPositionWithOffset(mRecipeIndex.get(letter), 0);
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

        if (cursor != null) {
            // Instantiate the member variable mCursor
            mCursor = cursor;

            // Move the Cursor to the first position
            if (mCursor.moveToFirst()) {
                // Hide the CardView informing user to add a favorite
                mCardView.setVisibility(View.INVISIBLE);

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

                // Set the boolean so that the card is animated the next time it is shown
                animateCard = true;
            } else {
                // Reveal the CardView
                if (animateCard) {
                    animateCardIn();
                } else {
                    mCardView.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    /**
     * Sets up and plays an animation to reveal the CardView
     */
    private void animateCardIn() {
        // Initialize the interpolator used for the animation
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        // Initialize the AnimatorSet to play the animation
        AnimatorSet animSet = new AnimatorSet();

        // Set up the animations for scaling X and Y axes
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(mCardView, "scaleX", 0.1f, 1.0f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(mCardView, "scaleY", 0.1f, 1.0f);

        // Set up the AnimatorSet
        animSet.playTogether(scaleXAnim, scaleYAnim);
        animSet.setDuration(300);
        animSet.setInterpolator(interpolator);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mCardView.setVisibility(View.VISIBLE);
            }
        });

        animSet.start();
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
}
