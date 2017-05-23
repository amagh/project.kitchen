package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.dialog.RecipeBookDetailsDialog;
import project.hnoct.kitchen.prefs.SettingsActivity;

public class ActivityRecipeBook extends AppCompatActivity implements RecipeBookDetailsDialog.RecipeBookDetailsListener {
    /** Constants **/
    private static final String RECIPE_BOOK_DETAILS_DIALOG = "recipe_book_details_dialog";
    private static final String LOG_TAG = ActivityRecipeBook.class.getSimpleName();

    /** Member Variables **/
    private ActionBarDrawerToggle mDrawerToggle;

    // Views bound by ButterKnife
    @BindView(R.id.recipe_book_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.recipe_book_fab) FloatingActionButton mFab;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.toolbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipebook);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

    @OnClick(R.id.recipe_book_fab)
    void onFabClick() {
        showRecipeBookDetailDialog();
    }

    /**
     * Shows the dialog for creating/editing the details of the recipe book
     */
    private void showRecipeBookDetailDialog() {
        RecipeBookDetailsDialog dialog = new RecipeBookDetailsDialog();
        dialog.show(getFragmentManager(), RECIPE_BOOK_DETAILS_DIALOG);
        dialog.setPositiveClickListener(this);
    }

    /**
     * Creates a new RecipeBook and inserts it into the database. Then opens the RecipeBook in the
     * ChaptersActivity
     * @param dialog Dialog for creating new RecipeBooks
     * @param titleText Title of the RecipeBook from user-input
     * @param descriptionText Description of the RecipeBook from user-input
     */
    @Override
    public void onPositiveDialogClick(DialogFragment dialog, String titleText, String descriptionText) {
        // Initialize parameters for inserting recipe book into database
        Uri uri = RecipeBookEntry.CONTENT_URI;

        // Create ContentValues for insertion
        ContentValues values = new ContentValues();
        values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME, titleText);
        values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_DESCRIPTION, descriptionText);

        // Insert recipe book into database
        Uri bookUri = getContentResolver().insert(
                uri,
                values
        );

        // Get the bookId from the URI generated from inserting into DB
        long bookId = Long.parseLong(bookUri != null ? bookUri.getLastPathSegment() : null);

        // Start the ActivityChapter that lists all chapters of a recipe book of the recipe that was
        // just created
        Intent intent = new Intent(this, ActivityChapter.class);
        intent.setData(RecipeBookEntry.buildUriFromRecipeBookId(bookId));
        startActivity(intent);
    }

    private void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_browse: {
                Intent intent = new Intent(this, ActivityRecipeList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_favorites: {
                startActivity(new Intent(this, ActivityFavorites.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_my_recipes: {
                startActivity(new Intent(this, ActivityMyRecipes.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_my_recipe_books: {
                break;
            }
            case R.id.action_shopping_list: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, ActivityShoppingList.class);
                startActivity(intent);
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
        }
    }

    /**
     * Hides the Navigation Drawer
     */
    private void hideNavigationDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }
}
