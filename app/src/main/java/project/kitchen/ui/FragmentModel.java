package project.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import project.kitchen.R;
import project.kitchen.data.RecipeContract;
import project.kitchen.ui.adapter.AdapterRecipe;
import project.kitchen.ui.adapter.RecipeItemAnimator;
import project.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

/**
 * Created by hnoct on 5/24/2017.
 */

public class FragmentModel extends Fragment {
    // Member Variables
    private StaggeredGridLayoutManagerWithSmoothScroll mStaggeredLayoutManager;
    private AdapterRecipe mRecipeAdapter;
    private int mPosition;
    private Context mContext;

    // Views Bound by ButterKnife
    @BindView(R.id.fragment_recyclerview) RecyclerView mRecipeRecyclerView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContext = getActivity();
        return null;
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns;
        if (((ActivityModel) getActivity()).mTwoPane && ((ActivityModel) getActivity()).mDetailsVisible) {
            columns = getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columns = getResources().getInteger(R.integer.recipe_columns);
        }

        if (mRecipeRecyclerView.getLayoutManager() == null) {
            // Instantiate the LayoutManager
            mStaggeredLayoutManager = new StaggeredGridLayoutManagerWithSmoothScroll(
                    columns,
                    StaggeredGridLayoutManager.VERTICAL
            );

            // Set the LayoutManager for the RecyclerView
            mRecipeRecyclerView.setLayoutManager(mStaggeredLayoutManager);

        } else {
            mStaggeredLayoutManager.setSpanCount(columns);
        }


        AdapterRecipe adapter = ((AdapterRecipe) mRecipeRecyclerView.getAdapter());
        if (adapter != null) {
            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        if (((ActivityModel) getActivity()).mTwoPane) {
            mRecipeRecyclerView.smoothScrollToPosition(mPosition);
        }
    }

    /**
     * Initializes the AdapterRecipe, sets onClickListeners, and sets Adapter parameters
     */
    public void initRecipeAdapter() {
        // Instantiate the Adapter for the RecyclerView
        mRecipeAdapter = new AdapterRecipe(getActivity(), new AdapterRecipe.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
                // Set position to the position of the clicked item
                mPosition = viewHolder.getAdapterPosition();

//                if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {
//                    // If using the detail fragment within AdapterRecipe, do not launch a new
//                    // FragmentRecipeDetails
//                    return;
//                } else {
                    // Initiate Callback to Activity which will launch Details Activity
                    ((RecipeCallback) getActivity()).onItemSelected(
                            recipeUrl,
                            imageUrl,
                            viewHolder
                    );

                    setLayoutColumns();
//                }
            }
        });

        // Allows for animations even when notifyDatasetChanged() is called
        mRecipeAdapter.setHasStableIds(true);

        // Set whether the RecyclerAdapter should utilize the detail layout
        boolean useDetailView = getResources().getBoolean(R.bool.recipeAdapterUseDetailView);
//        mRecipeAdapter.setUseDetailView(useDetailView, getChildFragmentManager());

        if (useDetailView) {
            // If FragmentRecipeDetails is being inflated from within mRecipeAdapter, then remove
            // Fragment-specific items from mToolBar
            mRecipeAdapter.setVisibilityListener(new AdapterRecipe.DetailVisibilityListener() {
                @Override
                public void onDetailsHidden() {
                    ((ActivityModel) getActivity()).mToolbar.getMenu().clear();
                }
            });
        }
    }

    /**
     * Instantiates the RecyclerView, sets the correct ItemAnimator, and sets the AdapterRecipe to
     * be used by the RecyclerView
     */
    public void initRecyclerView() {
        // Set the adapter to the RecyclerView
        mRecipeRecyclerView.setAdapter(mRecipeAdapter);

        // Initialize and set the RecipeItemAnimator
        RecipeItemAnimator animator = new RecipeItemAnimator(mContext);
        animator.setRecipeAnimatorListener(new RecipeItemAnimator.RecipeAnimatorListener() {
            @Override
            public void onFinishAnimateDetail() {
                // Listen for when the animation for the detail fragment inflation has been
                // completed so that the view can be scrolled to the top of the recipe's view
                mRecipeRecyclerView.smoothScrollToPosition(mPosition);
            }
        });
        mRecipeRecyclerView.setItemAnimator(animator);
    }

    /**
     * Retrieves the AdapterRecipe instantiated by FragmentModel
     * @return Adapter instantiated by FragmentModel
     */
    public AdapterRecipe getRecipeAdapter() {
        return mRecipeAdapter;
    }

    /**
     * Retrieves the RecyclerView instantiated by FragmentModel
     * @return RecyclerView instantiated by FragmentModel
     */
    public RecyclerView getRecyclerView() {
        return mRecipeRecyclerView;
    }

    public StaggeredGridLayoutManagerWithSmoothScroll getStaggeredLayoutManager() {
        return mStaggeredLayoutManager;
    }

    interface RecipeCallback {
        void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder);
    }

    public Map<String, Integer> populateRecipeIndex(Map<String, Integer> recipeIndex, Cursor cursor) {
        String ALPHABET = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        // Create the Map used as the index
        do {
            // Get the first letter of the recipe
            String letter = cursor.getString(RecipeContract.RecipeEntry.IDX_RECIPE_NAME).substring(0,1);
            if (recipeIndex.get(letter) != -1) {
                // If the position of the first instance of the letter already exists, then skip
                continue;
            }

            // Put the position of the first instance of the letter in mRecipeIndex
            recipeIndex.put(letter, cursor.getPosition());

        } while (cursor.moveToNext());

        // Set the position of any letters that haven't been favorite'd
        int lastPos = cursor.getCount();    // Used to hold the last position that had a favorite'd recipe with the first letter

        for (int i = 1; i < ALPHABET.length() + 1; i++) {
            // Iterate in reverse so that missing letters will jump to the position of the next letter
            String letter = Character.toString(ALPHABET.charAt(ALPHABET.length() - i));

            if (recipeIndex.get(letter) == -1) {
                // If no recipe has been favorite'd that start with that letter, then set it go
                // go to the same position as the letter after it (e.g. If no N exist, it will
                // go to the first position of O)
                recipeIndex.put(letter, lastPos);
            } else {
                // If recipe does exist that starts with that letter, then set it as the last
                // position to be used for the next unused letter
                lastPos = recipeIndex.get(letter);
            }
        }

        return recipeIndex;
    }
}
