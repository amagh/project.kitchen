package project.hnoct.kitchen.ui;

import android.content.AbstractThreadedSyncAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;
import java.util.Map;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.search.AllRecipesSearchAsyncTask;

public class ActivitySearch extends AppCompatActivity {
    // Constants
    public static final String SEARCH_TERM = "search_term";

    // Member Variables
    private String mSearchTerm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
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
        mSearchTerm = getIntent().getStringExtra(SEARCH_TERM);

        // Check to make sure search term is not empty and not null
        if (mSearchTerm == null || mSearchTerm.trim().isEmpty()) {
            return;
        }

        // Initialize and pass the searchTerm to the search AsyncTasks
        AllRecipesSearchAsyncTask allrecipesSearchTask = new AllRecipesSearchAsyncTask(this, new AllRecipesSearchAsyncTask.SyncListener() {
            @Override
            public void onFinishLoad(List<Map<String, Object>> recipeList) {

            }
        });
        allrecipesSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSearchTerm);

    }

}
