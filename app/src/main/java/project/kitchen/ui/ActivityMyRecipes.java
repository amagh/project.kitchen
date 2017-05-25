package project.kitchen.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.kitchen.R;
import project.kitchen.ui.adapter.AdapterRecipe;

public class ActivityMyRecipes extends ActivityModel implements FragmentModel.RecipeCallback {
    // Member Variables
    private boolean mTwoPane = false;
    private boolean mDetailsVisible = false;
    private ActionBarDrawerToggle mDrawerToggle;

    // Views Bound by ButterKnife
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.my_recipes_fab) FloatingActionButton mFab;

    @OnClick(R.id.my_recipes_fab)
    void onClick(View view) {
        // Launch ActivityCreateRecipe
        Intent intent = new Intent(this, ActivityCreateRecipe.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        setActivityId(R.id.action_my_recipes);

        initNavigationDrawer();

        initLayout(savedInstanceState);
        mTwoPane = getTwoPane();
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        if (!mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            startDetailsActivity(recipeUrl, imageUrl, viewHolder);
        } else {
            inflateDetailsFragment(recipeUrl, imageUrl, viewHolder);

            showDetailsContainer();
            mDetailsVisible = true;
        }
    }

    @Override
    public void selectDrawerItem(MenuItem item) {
        super.selectDrawerItem(item);
        mDetailsVisible = false;
        finish();
    }
}
