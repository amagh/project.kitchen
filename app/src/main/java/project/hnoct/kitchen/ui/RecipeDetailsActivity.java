package project.hnoct.kitchen.ui;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;

public class RecipeDetailsActivity extends AppCompatActivity {
    /** Constants **/

    /** Member Variables **/
    long mRecipeId;
    MenuItem mMenuFavorite;
    RecipeDetailsFragment mDetailsFragment;

    // Views bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        ButterKnife.bind(this);

        // Set Toolbar parameters
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Retrieve the URI passed from the RecipeListActivity
        Uri recipeUri = getIntent().getData();

        // Instantiate mRecipeId
        mRecipeId = RecipeContract.LinkEntry.getRecipeIdFromUri(recipeUri);

        // Add the URI as part of a Bundle to attach to the RecipeDetailsFragment
        Bundle args = new Bundle();
        args.putParcelable(RecipeDetailsFragment.RECIPE_DETAILS_URI, recipeUri);

        // Instantiate the fragment and attach the Bundle containing the recipe URI
        mDetailsFragment = new RecipeDetailsFragment();
        mDetailsFragment.setArguments(args);;

        if (savedInstanceState == null) {
            // Add the RecipeDetailsFragment to the container layout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.recipe_detail_container, mDetailsFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_details, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }

}
