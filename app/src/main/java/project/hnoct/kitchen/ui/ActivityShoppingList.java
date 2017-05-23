package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.prefs.SettingsActivity;

public class ActivityShoppingList extends AppCompatActivity {
    // Member Variables
    private ActionBarDrawerToggle mDrawerToggle;
    FragmentShoppingList mShoppingListFragment;

    // ButterKnife Bounds Views
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.shopping_list_fab) FloatingActionButton mDeleteFab;

    @OnClick(R.id.shopping_list_fab)
    void onClick(View view) {
        ((FragmentShoppingList) getSupportFragmentManager().findFragmentById(R.id.fragment_container))
                .deleteCheckedItems();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
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

        if (savedInstanceState == null) {
            mShoppingListFragment = new FragmentShoppingList();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mShoppingListFragment)
                    .commit();
        }

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
                startActivity(new Intent(this, ActivityRecipeBook.class));
                hideNavigationDrawer();
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_shopping_list: {
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
