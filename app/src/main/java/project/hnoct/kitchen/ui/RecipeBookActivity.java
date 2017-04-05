package project.hnoct.kitchen.ui;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.net.URI;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.dialog.RecipeBookDetailsDialog;

public class RecipeBookActivity extends AppCompatActivity implements RecipeBookDetailsDialog.RecipeBookDetailsListener {
    /** Constants **/
    private static final String RECIPE_BOOK_DETAILS_DIALOG = "recipe_book_details_dialog";
    private static final String LOG_TAG = RecipeBookActivity.class.getSimpleName();

    /** Member Variables **/

    // Views bound by ButterKnife
    @BindView(R.id.recipe_book_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.recipe_book_fab) FloatingActionButton mFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_book);
        ButterKnife.bind(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @OnClick(R.id.recipe_book_fab)
    void onFabClick() {
        showRecipeBookDetailDialog();
    }

    /**
     * Shows the dialog for creating/editing the details of the recipe book
     */
    void showRecipeBookDetailDialog() {
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
        Log.d(LOG_TAG, "Positive Dialog Clicked!");
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

        // Start the ChapterActivity that lists all chapters of a recipe book of the recipe that was
        // just created
        Intent intent = new Intent(this, ChapterActivity.class);
        intent.setData(RecipeBookEntry.buildUriFromRecipeBookId(bookId));
        startActivity(intent);
    }
}
