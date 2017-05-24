package project.hnoct.kitchen.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.ui.adapter.RecipeItemAnimator;
import project.hnoct.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

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
        return super.onCreateView(inflater, container, savedInstanceState);
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

                if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {
                    // If using the detail fragment within AdapterRecipe, do not launch a new
                    // FragmentRecipeDetails
                    return;
                } else {
                    // Initiate Callback to Activity which will launch Details Activity
                    ((RecipeCallback) getActivity()).onItemSelected(
                            recipeUrl,
                            imageUrl,
                            viewHolder
                    );

                    setLayoutColumns();
                }
            }
        });

        // Allows for animations even when notifyDatasetChanged() is called
        mRecipeAdapter.setHasStableIds(true);

        // Set whether the RecyclerAdapter should utilize the detail layout
        boolean useDetailView = getResources().getBoolean(R.bool.recipeAdapterUseDetailView);
        mRecipeAdapter.setUseDetailView(useDetailView, getChildFragmentManager());

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
}
