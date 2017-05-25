package project.kitchen.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.Optional;
import project.kitchen.R;
import project.kitchen.data.Utilities;
import project.kitchen.prefs.SettingsActivity;
import project.kitchen.ui.adapter.AdapterRecipe;

import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_IMAGE_URL;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_URL;

/**
 * Created by hnoct on 5/24/2017.
 */

public class ActivityModel extends AppCompatActivity {
    private final String DETAILS_FRAGMENT = "DFTAG";

    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.toolbar) Toolbar mToolbar;
    ActionBarDrawerToggle mDrawerToggle;
    @Nullable @BindView(R.id.detail_fragment_container) FrameLayout mDetailsContainer;

    int activityId = 0;

    boolean mDetailsVisible = false;
    boolean mTwoPane = false;

    /**
     * Used to set an activityId that will be checked by #selectDrawerItem(MenuItem) so that an
     * Activity does not attempt to open itself
     * @param activityId ID of the activity in the Menu
     */
    public void setActivityId(int activityId) {
        this.activityId = activityId;
    }

    /**
     * Creates transition element Pairs and starts ActivityRecipeDetails
     * @param recipeUrl URL of the recipe that ActivityRecipeDetails will open
     * @param imageUrl URL of the image for the recipe
     * @param viewHolder ViewHolder containing the ImageView for the transition animation
     */
    public void startDetailsActivity(String recipeUrl, String imageUrl,
                                      AdapterRecipe.RecipeViewHolder viewHolder) {
        // Set up the transition animations
        // StatusBar and NavigationBar are also set as transition elements to ensure the recipe
        // image being transitioned does not show above them
        List<Pair<View, String>> pairs = new ArrayList<>();

        View statusBar = null, navigationBar = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            statusBar = findViewById(android.R.id.statusBarBackground);
            navigationBar = findViewById(android.R.id.navigationBarBackground);

            pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
            if (navigationBar != null) {
                // Add the NavigationBar transition only if the user has on-screen buttons
                pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
            }
        }

        pairs.add(Pair.create((View) viewHolder.recipeImage, getString(R.string.transition_recipe_image)));


        // Create ActivityOptionsCompat from the transitions
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                pairs.toArray(new Pair[pairs.size()])
        );

        // Create the Intent and add the recipeUrl and imageUrl to be passed to ActivityRecipeDetails
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        intent.putExtra(getString(R.string.extra_image), imageUrl);

        // Start the Activity with the transition animation Bundle
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    /**
     * Infates FragmentRecipeDetails into the container
     * @param recipeUrl URL of the recipe to be shows in FragmentRecipeDetails
     * @param imageUrl URL for the image of the recipe
     * @param viewHolder ViewHolder containing the ImageView for transition animation
     */
    public void inflateDetailsFragment(String recipeUrl, String imageUrl,
                                        AdapterRecipe.RecipeViewHolder viewHolder) {
        // Create a new FragmentRecipeDetails
        FragmentRecipeDetails fragment = new FragmentRecipeDetails();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Set a fade animation to occur during transition between recipes in two-pane
            fragment.setEnterTransition(new Fade());
        }

        // Create the Bundle and add the recipe's URL to it and set it as the argument for the
        // fragment
        Bundle args = new Bundle();
        args.putParcelable(RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
        args.putString(RECIPE_DETAILS_IMAGE_URL, imageUrl);
        fragment.setArguments(args);

        // Replace the existing FragmentRecipeDetails with the newly created one
        getSupportFragmentManager().beginTransaction()
                .addSharedElement(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
                .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                .commit();
    }

    /**
     * Inflates a FragmentRecipeDetails when the image URL is unknown as in the case of importing
     * a new recipe
     * @param recipeUrl URL of the recipe to be opened by FragmentRecipeDetails
     */
    public void inflateDetailsFragment(String recipeUrl) {
        // Create a new FragmentRecipeDetails
        FragmentRecipeDetails fragment = new FragmentRecipeDetails();

        // Create the Bundle and add the recipe's URL to it and set it as the argument for the
        // fragment
        Bundle args = new Bundle();
        args.putParcelable(RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
        fragment.setArguments(args);

        // Replace the existing FragmentRecipeDetails with the newly created one
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                .commit();

        // Show the FragmentRecipeDetails in the master-flow view
        showDetailsContainer();
    }

    /**
     * Opens the corresponding Activity that is selected by the user
     * @param item The MenuItem the user has selected
     */
    public void selectDrawerItem(MenuItem item) {
        // Check to ensure that the activityId is different than the ID of the menu item
        if (item.getItemId() == activityId) {
            // If it matches, do nothing
            return;
        }

        switch (item.getItemId()) {
            case R.id.action_browse: {
                startActivity(new Intent(this, ActivityRecipeList.class));
                break;
            }
            case R.id.action_favorites: {
                startActivity(new Intent(this, ActivityFavorites.class));
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
            case R.id.action_my_recipes: {
                startActivity(new Intent(this, ActivityMyRecipes.class));
                break;
            }
            case R.id.action_my_recipe_books: {
                startActivity(new Intent(this, ActivityRecipeBook.class));
                break;
            }
            case R.id.action_shopping_list: {
                startActivity(new Intent(this, ActivityShoppingList.class));
                break;
            }
        }

        // Hide the NavigationDrawer
        hideNavigationDrawer();

        if (mDetailsContainer != null) {
            hideDetailsContainer();
        }
    }

    /**
     * Hides the Navigation Drawer
     */
    private void hideNavigationDrawer() {
        mDrawerLayout.closeDrawer(Gravity.START|Gravity.LEFT);
    }

    /**
     * Sets the width of the FrameLayout of the FragmentRecipeDetails container to 0 to hide it from
     * view
     */
    public void hideDetailsContainer() {
        // Check to ensure the Activity has an mDetailsContainer
        if (mDetailsContainer == null) {
            return;
        }

        // Set the boolean to indicate that the DetailsContainer is not showing
        mDetailsVisible = false;

        // Set the width of mDetailsContainer to 0 so that the FragmentRecipeList can take up
        // the entire space
        ViewGroup.LayoutParams params = mDetailsContainer.getLayoutParams();
        params.width = 0;
        mDetailsContainer.setLayoutParams(params);

        FragmentRecipeDetails fragment = (FragmentRecipeDetails) getSupportFragmentManager()
                .findFragmentByTag(DETAILS_FRAGMENT);

        if (fragment != null) {
            // Remove the Fragment to clear the Toolbar of Fragment-specific actions
            getSupportFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }

    }

    /**
     * Sets the width of the FrameLayout of FragmentRecipeDetails container to 600dp so that an
     * inflated view can be seen
     */
    public void showDetailsContainer() {
        // Check to ensure the Activity has an mDetailsContainer
        if (mDetailsContainer == null) {
            return;
        }

        // Set the boolean to indicate that the DetailsContainer is showing
        mDetailsVisible = true;

        // Show the FragmentRecipeDetails in the master-flow view
        ViewGroup.LayoutParams params = mDetailsContainer.getLayoutParams();
        params.width = (int) Utilities.convertDpToPixels(600);
        mDetailsContainer.setLayoutParams(params);
    }

    /**
     * Sets up the NavigationDrawerToggle and sets the OnClickListener for the NavigationDrawer
     */
    public void initNavigationDrawer() {
        // Set up the hamburger menu used for opening mDrawerLayout
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, R.string.button_confirm, R.string.button_deny);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });
    }

    /**
     * Sets up the UI of the interface to either use the phone layout or the tablet master-flow
     * layout
     * @param savedInstanceState The savedInstanceState Bundle passed to the onCreate of the
     *                           calling Activity
     */
    public void initLayout(Bundle savedInstanceState) {
        if (mDetailsContainer == null) {
            // If container is not found, then utilizing phone layout without two panes
            mTwoPane = false;
        } else {
            // Set the boolean indicating that there are two panes in view
            mTwoPane = true;
            if (savedInstanceState == null && !mDetailsVisible) {
                // If no recipe has been selected yet, then set boolean indicating that the
                // details fragment is not visible
                mDetailsVisible = false;

                // Set the width of mDetailsContainer to 0 so that the FragmentRecipeList can take up
                // the entire space
                hideDetailsContainer();
            }
        }
    }

    /**
     * Retrieve the value of mTwoPane
     * @return mTwoPane
     */
    public boolean getTwoPane() {
        return mTwoPane;
    }

    @Optional
    @OnClick(R.id.temp_button)
    public void closePreview() {
        // Set the boolean to indicate that mDetailsContainer is not visible
        mDetailsVisible = false;

        // Hide mDetailsContainer
        hideDetailsContainer();

        // Reset the layout columns of the Fragment so that the correct number of columns show
        FragmentRecipeList fragment = (FragmentRecipeList) getSupportFragmentManager()
                .findFragmentById(R.id.fragment);
        fragment.setLayoutColumns();
    }
}
