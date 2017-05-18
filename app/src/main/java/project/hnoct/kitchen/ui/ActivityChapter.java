package project.hnoct.kitchen.ui;

import android.app.DialogFragment;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.dialog.AddRecipeDialog;
import project.hnoct.kitchen.dialog.ChapterDetailsDialog;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityChapter extends AppCompatActivity implements ChapterDetailsDialog.ChapterDetailsListener, FragmentChapter.RecipeCallBack {
    /** Constants**/
    private static final String LOG_TAG = ActivityChapter.class.getSimpleName();
    private static final String CHAPTER_DETAILS_DIALOG = "chapter_details_dialog";
    private static final String ADD_RECIPE_DIALOG = "add_recipe_dialog";
    private static final String CHAPTER_FRAGMENT_TAG = "chapter_fragment";

    /** Member Variables **/
    private long mBookId;
    private ChapterListener mChapterListener;

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

        // Set the member variable bookId
        mBookId = RecipeBookEntry.getRecipeBookIdFromUri(bookUri);

        // Pass the URI to the FragmentChapter as part of a Bundle
        FragmentChapter fragment = new FragmentChapter();
        Bundle args = new Bundle();
        args.putParcelable(FragmentChapter.RECIPE_BOOK_URI, bookUri);
        fragment.setArguments(args);

        // Inflate FragmentChapter into the container
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.chapter_container, fragment, CHAPTER_FRAGMENT_TAG)
                    .commit();
        }
    }

    /**
     * Override the back-button so that when in edit-mode, edit mode is exited instead
     */
    @Override
    public void onBackPressed() {
        FragmentChapter fragment = (FragmentChapter) getSupportFragmentManager().findFragmentByTag(CHAPTER_FRAGMENT_TAG);

        if (fragment.mChapterAdapter.isInEditMode()) {
            fragment.mChapterAdapter.exitEditMode();
            return;
        }

        super.onBackPressed();
    }

    interface ChapterListener {
        void onNewChapter();
    }

    public void setChapterListener(ChapterListener listener) {
        mChapterListener = listener;
    }

    @OnClick (R.id.chapter_fab)
    void onFabClick() {
        showAddChapterDialog();
    }

    /**
     * Show the dialog for adding chapters
     */
    private void showAddChapterDialog() {
        // Instantiate the dialog
        ChapterDetailsDialog dialog = new ChapterDetailsDialog();
        dialog.show(getFragmentManager(), CHAPTER_DETAILS_DIALOG);

        // Set the listener for the positive click
        dialog.setPositiveClickListener(this);
    }

    @Override
    public void onPositiveDialogClick(DialogFragment dialog, String titleText, String descriptionText) {
        Toast.makeText(this, "Positive clicked!", Toast.LENGTH_SHORT).show();

        // Initialize parameters for inserting chapter into database
        Uri chapterUri = ChapterEntry.CONTENT_URI;
        String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
        String[] selectionArgs = new String[] {Long.toString(mBookId)};

        // Determine the chapter order
        Cursor cursor = getContentResolver().query(
                chapterUri,
                new String[] {ChapterEntry.COLUMN_CHAPTER_ORDER},
                selection,
                selectionArgs,
                ChapterEntry.COLUMN_CHAPTER_ORDER + " DESC"
        );

        int chapterOrder;
        if (cursor != null && cursor.moveToFirst()) {
            chapterOrder = cursor.getInt(0) + 1;
        } else {
            chapterOrder = 0;
        }

        // Create ContentValues to insert
        ContentValues values = new ContentValues();
        values.put(ChapterEntry.COLUMN_CHAPTER_NAME, titleText);
        values.put(ChapterEntry.COLUMN_CHAPTER_DESCRIPTION, descriptionText);
        values.put(ChapterEntry.COLUMN_CHAPTER_ORDER, chapterOrder);
        values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_ID, mBookId);

        // Insert chapter into database
        getContentResolver().insert(
                chapterUri,
                values
        );

        if (mChapterListener != null) {
            mChapterListener.onNewChapter();
        }
    }

    @Override
    public void onRecipeSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        // If in single-view mode, then start the ActivityRecipeDetails
        View statusBar = findViewById(android.R.id.statusBarBackground);
        View navigationBar = findViewById(android.R.id.navigationBarBackground);

        List<Pair<View, String>> pairs = new ArrayList<>();
        pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
        if (navigationBar != null) {
            pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
        }
        pairs.add(Pair.create((View) viewHolder.recipeImage, getString(R.string.transition_recipe_image)));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                this,
                pairs.toArray(new Pair[pairs.size()])
        );
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        intent.putExtra(getString(R.string.extra_image), imageUrl);
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

    @Override
    public void onAddRecipeClicked(final long chapterId) {
        // Initialize the Dialog that will show recipes
        AddRecipeDialog dialog = new AddRecipeDialog();

        // Pass the chapterId to the Dialog
        dialog.setChapterId((int) chapterId);

        // Set the listener for when a recipe is selected
        dialog.setSelectionListener(new AddRecipeDialog.SelectionListener() {

            @Override
            public void onRecipeSelected(String recipeUrl) {
                // Initialize parameters for querying the database for recipe order
                Uri linkRecipeBookUri = LinkRecipeBookEntry.CONTENT_URI;
                String[] projection = LinkRecipeBookEntry.PROJECTION;
                String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(chapterId)};
                String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " DESC";

                // Query the database to determine the new recipe's order in the chapter
                Cursor cursor = getContentResolver().query(
                        linkRecipeBookUri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                );

                int recipeOrder;
                if (cursor !=  null && cursor.moveToFirst()) {
                    recipeOrder = cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER) + 1;

                } else {
                    recipeOrder = 0;
                }

                // Close the Cursor
                if (cursor != null) {
                    cursor.close();
                }

                long recipeId = Utilities.getRecipeIdFromUrl(ActivityChapter.this, recipeUrl);

                // Create the ContentValues to insert into the linked recipe book table
                ContentValues values = new ContentValues();
                values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_ID, mBookId);
                values.put(ChapterEntry.COLUMN_CHAPTER_ID, chapterId);
                values.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
                values.put(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, recipeOrder);

                // Insert values into database
                getContentResolver().insert(
                        linkRecipeBookUri,
                        values
                );
            }
        });

        // Show the Dialog
        dialog.show(getFragmentManager(), ADD_RECIPE_DIALOG);
    }
}
