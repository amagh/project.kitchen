package project.hnoct.kitchen.ui;

import android.content.AbstractThreadedSyncAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebView;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.search.AllRecipesSearchAsyncTask;

public class ActivitySearch extends AppCompatActivity {
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

}
