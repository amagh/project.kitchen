package project.hnoct.kitchen.ui;

import android.content.AbstractThreadedSyncAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.search.AllRecipesSearchAsyncTask;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivitySearch extends AppCompatActivity implements FragmentSearch.RecipeCallback {
    // Constants
    public static final String SEARCH_TERM = "search_term";

    // Member Variables
    private String mSearchTerm;

    // Views bound by ButterKnife

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve the search term from the Intent
        mSearchTerm = getIntent().getStringExtra(SEARCH_TERM).trim();

        // Check to make sure search term is not empty
        if (mSearchTerm.isEmpty()) {
            return;
        }

        if (savedInstanceState == null) {
            // Create a Bundle and add the search term to it
            Bundle args = new Bundle();
            args.putString(SEARCH_TERM, mSearchTerm);

            // Attach the Bundle to a new FragmentSearch
            FragmentSearch fragment = new FragmentSearch();
            fragment.setArguments(args);

            // Add FragmentSearch
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.search_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onRecipeSelected(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        if (!ActivityRecipeList.mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    new Pair(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
            );
            Intent intent = new Intent(this, ActivityRecipeDetails.class);
            intent.setData(Uri.parse(recipeUrl));
            ActivityCompat.startActivity(this, intent, options.toBundle());
        } else {
//            ActivityRecipeList.mDetailsVisible = true;
//
//            // Create a new FragmentRecipeDetails
//            FragmentRecipeDetails fragment = new FragmentRecipeDetails();
//
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                // Set a fade animation to occur during transition between recipes in two-pane
//                fragment.setEnterTransition(new Fade());
//            }
//
//            // Create the Bundle and add the recipe's URL to it and set it as the argument for the
//            // fragment
//            Bundle args = new Bundle();
//            args.putParcelable(FragmentRecipeDetails.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
//            fragment.setArguments(args);
//
//            // Replace the existing FragmentRecipeDetails with the newly created one
//            getSupportFragmentManager().beginTransaction()
//                    .addSharedElement(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
//                    .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
//                    .commit();
//
//            // Show the FragmentRecipeDetails in the master-flow view
//            ViewGroup.LayoutParams params = mContainer.getLayoutParams();
//            params.width = (int) Utilities.convertDpToPixels(600);
//            mContainer.setLayoutParams(params);
        }
    }
}
