package project.hnoct.kitchen.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

public class ChapterActivity extends AppCompatActivity {
    /** Constants**/

    /** Member Variables **/

    // Views bound by ButterKnife
    @BindView(R.id.chapter_fab) FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chapter);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Retrieve URI data to pass to the Fragment
        Uri bookUri = getIntent() != null ? getIntent().getData() : null;

        // Pass the URI to the ChapterFragment as part of a Bundle
        ChapterFragment fragment = new ChapterFragment();
        Bundle args = new Bundle();
        args.putParcelable(ChapterFragment.RECIPE_BOOK_URI, bookUri);

        // Inflate ChapterFragment into the container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chapter_container, fragment)
                    .commit();
        }
    }

}
