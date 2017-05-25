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
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.view.SlidingAlphabeticalIndex;
import project.hnoct.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentMyRecipes extends FragmentModel implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FragmentMyRecipes.class.getSimpleName();
    private static final int MY_RECIPES_LOADER = 4;
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
    @BindView(R.id.my_recipes_sliding_index) SlidingAlphabeticalIndex mIndex;
    @BindView(R.id.fragment_recyclerview) RecyclerView mRecipeRecyclerView;
    @BindView(R.id.my_recipes_cardview) CardView mCardView;

    public FragmentMyRecipes() {
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(MY_RECIPES_LOADER, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_recipes, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();
        mRecipeIndex = new HashMap<>();

        // Init the AdapterRecipe and obtain a reference to it
        initRecipeAdapter();
        mRecipeAdapter = getRecipeAdapter();

        // Init the StaggeredLayoutManager and obtain a reference to it
        setLayoutColumns();
        mStaggeredLayoutManager = getStaggeredLayoutManager();

        // Init the RecyclerView and obtain a reference to it
        initRecyclerView();
        mRecipeRecyclerView = getRecyclerView();

        // Add ScrollListener to hide FAB when user is scrolling and show again when stopped
        mRecipeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                ((ActivityMyRecipes)getActivity()).mFab.hide();
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    ((ActivityMyRecipes)getActivity()).mFab.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

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
        String selection = RecipeContract.RecipeEntry.COLUMN_RECIPE_SOURCE_ID + " LIKE ?";
        String[] selectionArgs = new String[] {"*%"};
        String sortOrder = RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME + " ASC";

        // Initialize and return the CursorLoader
        return new CursorLoader(
                mContext,
                RecipeContract.RecipeEntry.CONTENT_URI,
                RecipeContract.RecipeEntry.RECIPE_PROJECTION,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Retrieved the index populated by the ScrollingAlphabeticalIndex
        mRecipeIndex = mIndex.getIndex();

        // Instantiate the member variable mCursor
        mCursor = cursor;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Create the Map used as the index
                populateRecipeIndex(mRecipeIndex, cursor);

                // Reset the Cursor to the first position and swap it into mRecipeAdapter
                cursor.moveToFirst();

                // Hide mCardView
                mCardView.setVisibility(View.INVISIBLE);

                // Set the boolean so the CardView is animated next time it is shown
                animateCard = true;
            } else {
                // Show mCardView
                if (animateCard) {
                    animateCardIn();
                } else {
                    mCardView.setVisibility(View.VISIBLE);
                }
            }
        }

        // Swap the Cursor into mRecipeAdapter
        mRecipeAdapter.swapCursor(mCursor);
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
        getLoaderManager().initLoader(MY_RECIPES_LOADER, null, this);
    }
}
