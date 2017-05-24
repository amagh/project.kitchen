package project.hnoct.kitchen.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.FrameLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityFavorites extends ActivityModel implements FragmentModel.RecipeCallback {
    // Member Variables
    private boolean mTwoPane = false;
    private boolean mDetailsVisible = false;
    private ActionBarDrawerToggle mDrawerToggle;

    // Views Bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @Nullable @BindView(R.id.detail_fragment_container) FrameLayout mDetailsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        setActivityId(R.id.action_favorites);

        // Set up the hamburger menu used for opening mDrawerLayout
        initNavigationDrawer();

        // Set up whether to use tablet or phone layout
        initLayout(savedInstanceState);
        mTwoPane = getTwoPane();
    }

    @Override
    public void selectDrawerItem(MenuItem item) {
        super.selectDrawerItem(item);
        mDetailsVisible = false;
        finish();
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        if (!mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            startDetailsActivity(recipeUrl, imageUrl, viewHolder);
        } else {
            inflateDetailsFragment(recipeUrl, imageUrl, viewHolder);

            // Show the FragmentRecipeDetails in the master-flow view
            showDetailsContainer();

            mDetailsVisible = true;
        }
    }
}
