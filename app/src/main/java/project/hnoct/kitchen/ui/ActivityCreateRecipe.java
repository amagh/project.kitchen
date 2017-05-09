package project.hnoct.kitchen.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;

public class ActivityCreateRecipe extends AppCompatActivity {
    private boolean canDelete = false;
    private long mRecipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recipe);

        // Get Toolbar instance and set Toolbar options
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Create a new Bundle to attach to the fragment and add the recipeUri if it is passed from
        // ActivityRecipeDetails
        Bundle args = new Bundle();
        Uri recipeUri = getIntent().getData();
        args.putParcelable(FragmentCreateRecipe.RECIPE_URI, recipeUri);

        // Instantiate the FragmentCreateRecipe and set the args
        FragmentCreateRecipe fragment = new FragmentCreateRecipe();
        fragment.setArguments(args);

        if (savedInstanceState == null) {
            // Add the fragment to the container if new
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.create_recipe_container, fragment)
                    .commit();
        }

        if (recipeUri != null) {
            String recipeSourceId = Utilities.getRecipeSourceIdFromUri(this, recipeUri);
            mRecipeId = RecipeContract.RecipeEntry.getRecipeIdFromUri(recipeUri);

            if (recipeSourceId.substring(0,1).equals("*")) {
                canDelete = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getIntent().getData() != null) {
            getMenuInflater().inflate(R.menu.menu_create_recipe, menu);
        } else {
            getMenuInflater().inflate(R.menu.menu_create_recipe_no_delete, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save: {
                // Initiate the Callback to the FragmentCreateRecipe to inform it that user has clicked
                // the save option
                FragmentCreateRecipe fragment =
                        (FragmentCreateRecipe) getSupportFragmentManager().findFragmentById(R.id.create_recipe_container);
                fragment.onSaveClicked();
                break;
            }
            case R.id.action_clear_recipe: {
                FragmentCreateRecipe fragment =
                        (FragmentCreateRecipe) getSupportFragmentManager().findFragmentById(R.id.create_recipe_container);
                fragment.onClearClicked();
                break;
            }
            case R.id.action_delete_recipe: {
                // Delete the recipe entry
                getContentResolver().delete(
                        RecipeContract.RecipeEntry.CONTENT_URI,
                        RecipeContract.RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                        new String[] {Long.toString(mRecipeId)}
                );

                // Delete the IngredientLink Entries
                getContentResolver().delete(
                        RecipeContract.LinkIngredientEntry.CONTENT_URI,
                        RecipeContract.RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                        new String[] {Long.toString(mRecipeId)}
                );

                // Close the Activity
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // Callback Interface for informing fragment that the save button has been clicked
    public interface MenuButtonClicked {
        void onSaveClicked();

        void onClearClicked();
    }
}
