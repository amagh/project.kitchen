package project.kitchen.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.kitchen.R;
import project.kitchen.prefs.SettingsActivity;

public class ActivityShoppingList extends ActivityModel {
    // Member Variables
    private ActionBarDrawerToggle mDrawerToggle;
    FragmentShoppingList mShoppingListFragment;

    // ButterKnife Bounds Views
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
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

        setSupportActionBar(mToolbar);
        setActivityId(R.id.action_shopping_list);

        // Set up the hamburger menu used for opening mDrawerLayout
        initNavigationDrawer();

        if (savedInstanceState == null) {
            mShoppingListFragment = new FragmentShoppingList();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, mShoppingListFragment)
                    .commit();
        }

    }

    @Override
    public void selectDrawerItem(MenuItem item) {
        super.selectDrawerItem(item);
        finish();
    }
}
