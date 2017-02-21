package project.hnoct.kitchen.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import project.hnoct.kitchen.R;

public class RecipeDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve the URI passed from the RecipeListActivity
        Uri recipeUri = getIntent().getData();

        // Add the URI as part of a Bundle to attach to the RecipeDetailsFragment
        Bundle args = new Bundle();
        args.putParcelable(RecipeDetailsFragment.RECIPE_DETAILS_URI, recipeUri);

        // Instantiate the fragment and attach the Bundle containing the recipe URI
        RecipeDetailsFragment recipeDetailsFragment = new RecipeDetailsFragment();
        recipeDetailsFragment.setArguments(args);;

        if (savedInstanceState == null) {
            // Add the RecipeDetailsFragment to the container layout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.recipe_detail_container, recipeDetailsFragment)
                    .commit();
        }
    }

}
