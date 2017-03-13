package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.dialog.ImportRecipeDialog;
import project.hnoct.kitchen.prefs.SettingsActivity;
import project.hnoct.kitchen.sync.AllRecipesListAsyncTask;

public class RecipeListActivity extends AppCompatActivity implements RecipeListFragment.RecipeCallBack, ImportRecipeDialog.ImportRecipeDialogListener {
    /** Constants **/
    private static final String LOG_TAG = RecipeListActivity.class.getSimpleName();
    private final String IMPORT_DIALOG = "ImportRecipeDialog";

    /** Member Variables **/
    private static boolean mFabMenuOpen;

    // Bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.main_menu_fab) FloatingActionButton mFab;
    @BindView(R.id.main_add_recipe_fab) FloatingActionButton mAddRecipeFab;
    @BindView(R.id.main_import_recipe_fab) FloatingActionButton mImportRecipeFab;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_menu_text) TextView mMainFabText;
    @BindView(R.id.main_add_recipe_text) TextView mAddRecipeText;
    @BindView(R.id.main_import_recipe_text) TextView mImportRecipeText;

    @OnClick(R.id.main_menu_fab)
    public void onClickFabMenu() {
        if (!mFabMenuOpen) {
            showFabMenu();
        } else {
            closeFabMenu();
        }
    }

    @OnClick(R.id.main_import_recipe_fab)
    public void onClickFabImport() {
        closeFabMenu();
        showImportDialog();
    }

    @OnClick(R.id.main_add_recipe_fab)
    public void createRecipe() {
        closeFabMenu();
        startActivity(new Intent(this, CreateRecipeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });

        AllRecipesListAsyncTask syncTask = new AllRecipesListAsyncTask(this);
        syncTask.execute();


    }

    public void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorites: {
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(RecipeListActivity.this, SettingsActivity.class));
                break;
            }
            case R.id.action_my_recipes: {
                break;
            }
            case R.id.action_clear_data: {
                // Delete the database and restart the application to rebuild it
                boolean deleted = deleteDatabase(RecipeDbHelper.DATABASE_NAME);
                Log.d(LOG_TAG, "Database deleted " + deleted);

                // Set an Alarm to re-open the Application right after it is closed
                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                PendingIntent restartIntent = PendingIntent.getActivity(
                        getBaseContext(), 0, new Intent(getIntent()),
                        PendingIntent.FLAG_ONE_SHOT);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);

                // Exit the application
                System.exit(2);
                break;
            }
        }
    }

    /**
     * Opens the FAB Menu
     */
    void showFabMenu() {
        // Set the boolean to true
        mFabMenuOpen = true;

        // Make the menu options VISIBLE
        mAddRecipeFab.setVisibility(View.VISIBLE);
        mImportRecipeFab.setVisibility(View.VISIBLE);
        mMainFabText.setVisibility(View.VISIBLE);
        mAddRecipeText.setVisibility(View.VISIBLE);
        mImportRecipeText.setVisibility(View.VISIBLE);

        // Set the icon for the FAB to the cancel icon
        mFab.setImageResource(R.drawable.ic_menu_close_clear_cancel);
    }

    /**
     * Closes the FAB Menu
     */
    void closeFabMenu() {
        // Set the boolean to false
        mFabMenuOpen = false;

        // Make the menu options GONE
        mAddRecipeFab.setVisibility(View.GONE);
        mImportRecipeFab.setVisibility(View.GONE);
        mMainFabText.setVisibility(View.GONE);
        mAddRecipeText.setVisibility(View.GONE);
        mImportRecipeText.setVisibility(View.GONE);

        // Set the FAB icon to the add icon
        mFab.setImageResource(R.drawable.ic_menu_add_custom);
    }

    /**
     * Shows a Dialog with an EditText to allow copy-pasting of a recipe URL so it can be imported
     */
    void showImportDialog() {
        ImportRecipeDialog dialog = new ImportRecipeDialog();
        dialog.show(getFragmentManager(), IMPORT_DIALOG);
    }

    @Override
    public void onItemSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder) {
        Intent intent = new Intent(this, RecipeDetailsActivity.class);
        intent.setData(Uri.parse(recipeUrl));
        startActivity(intent);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String recipeUrl) {
        Intent intent = new Intent(this, RecipeDetailsActivity.class);
        intent.setData(Uri.parse(recipeUrl));
        startActivity(intent);
    }
}
