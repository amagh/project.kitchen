package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
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
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.prefs.SettingsActivity;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityMyRecipes extends AppCompatActivity implements FragmentMyRecipes.RecipeCallback {

    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ButterKnife.bind(this);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        // If in single-view mode, then start the ActivityRecipeDetails
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                new Pair(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
        );
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        intent.putExtra(getString(R.string.extra_image), imageUrl);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    /**
     * Hides the Navigation Drawer
     */
    private void hideNavigationDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
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
                break;
            }
            case R.id.action_my_recipe_books: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, ActivityRecipeBook.class);
                startActivity(intent);
                ActivityRecipeList.mDetailsVisible = false;
                finish();
                break;
            }
            case R.id.action_copy_db: {
                File sd = Environment.getExternalStorageDirectory();
                File database = getApplicationContext().getDatabasePath(RecipeDbHelper.DATABASE_NAME + ".db");
                if (sd.canWrite()) {
                    File dbCopy = new File(sd, RecipeDbHelper.DATABASE_NAME + ".db");
                    if (database.exists()) {
                        try {
                            FileChannel src = new FileInputStream(database).getChannel();
                            FileChannel dst = new FileInputStream(dbCopy).getChannel();
                            dst.transferFrom(src, 0, src.size());

                            src.close();
                            dst.close();
                            Toast.makeText(this, "Database copied to external storage!", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                break;
            }
            case R.id.action_clear_data: {
                // Delete the database and restart the application to rebuild it
                boolean deleted = deleteDatabase(RecipeDbHelper.DATABASE_NAME);

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
}
