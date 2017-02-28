package project.hnoct.kitchen.ui;

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

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.prefs.SettingsActivity;
import project.hnoct.kitchen.sync.AllRecipesListAsyncTask;

public class RecipeListActivity extends AppCompatActivity implements RecipeListFragment.RecipeCallBack {
    /** Constants **/
    private static final String LOG_TAG = RecipeListActivity.class.getSimpleName();

    /** Member Variables **/
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.fab) FloatingActionButton mFab;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean deleted = deleteDatabase(RecipeDbHelper.DATABASE_NAME);
                Log.d(LOG_TAG, "Database deleted " + deleted);
                AllRecipesListAsyncTask syncTask = new AllRecipesListAsyncTask(RecipeListActivity.this);
                syncTask.execute();
            }
        });

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

            }
            case R.id.action_settings: {
                startActivity(new Intent(RecipeListActivity.this, SettingsActivity.class));
            }
            case R.id.action_my_recipes: {

            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public void onItemSelected(Uri recipeUri, RecipeAdapter.RecipeViewHolder viewHolder) {
        Intent intent = new Intent(this, RecipeDetailsActivity.class);
        intent.setData(recipeUri);
        startActivity(intent);
    }

}
