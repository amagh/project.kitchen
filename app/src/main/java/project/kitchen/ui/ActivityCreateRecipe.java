package project.kitchen.ui;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import project.kitchen.R;
import project.kitchen.data.RecipeContract;
import project.kitchen.data.Utilities;

public class ActivityCreateRecipe extends AppCompatActivity {
    // Constants
    public static String DELETE_GENERIC_EXTRA = "delete_generic_extra";

    // Member Variables
    public static boolean deleteOriginal = false;
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
        deleteOriginal = getIntent().getBooleanExtra(DELETE_GENERIC_EXTRA, false);

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
            // Retrieve the recipeSourceId
            String recipeSourceId = Utilities.getRecipeSourceIdFromUri(this, recipeUri);

            // Retrieve the recipeIdArray so that if the user deletes the recipe, it can be easily
            // referenced
            mRecipeId = RecipeContract.RecipeEntry.getRecipeIdFromUri(recipeUri);

            if (recipeSourceId.contains(getString(R.string.custom_prefix))) {
                // If the recipe is a custom recipe, load the menu that includes the option to delete
                canDelete = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (canDelete) {
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
                        (FragmentCreateRecipe) getSupportFragmentManager()
                                .findFragmentById(R.id.create_recipe_container);
                fragment.onSaveClicked();
                break;
            }
            case R.id.action_clear_recipe: {
                FragmentCreateRecipe fragment =
                        (FragmentCreateRecipe) getSupportFragmentManager()
                                .findFragmentById(R.id.create_recipe_container);
                fragment.onClearClicked();
                break;
            }
            case R.id.action_delete_recipe: {
                // Show a dialog asking the user to confirm deletion of the recipe
                final AlertDialog dialog = new AlertDialog.Builder(this).create();

                // Set Dialog message
                dialog.setMessage(getString(R.string.dialog_confirm_delete_recipe));

                // Set the positive and negative buttons
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the dialog
                        dialog.dismiss();

                        // Delete the recipe entry'
                        deleteRecipe();

                        // Delete any auto-saved data
                        FragmentCreateRecipe fragment =
                                (FragmentCreateRecipe) getSupportFragmentManager()
                                        .findFragmentById(R.id.create_recipe_container);
                        fragment.onClearClicked();

                        // Close the Activity
                        finish();
                    }
                });

                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.button_deny), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Close the dialog and do nothing
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // Callback Interface for informing fragment that the save button has been clicked
    public interface MenuButtonClicked {
        void onSaveClicked();

        void onClearClicked();
    }

    private void deleteRecipe() {
        // Delete the recipe's entry
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        int recipesDeleted = prefs.getInt(getString(R.string.recipes_deleted_key), 0);
        editor.putInt(getString(R.string.recipes_deleted_key), recipesDeleted + 1);

        editor.apply();
    }
}
