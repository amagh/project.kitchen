package project.kitchen.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.ui.ActivityRecipeList;
import project.kitchen.ui.adapter.AdapterChapter;
import project.kitchen.ui.adapter.AdapterRecipeBook;

/**
 * Created by hnoct on 4/20/2017.
 */

public class AddToRecipeBookDialog extends DialogFragment {
    /** Constants **/

    /** Member Variables **/
    private AdapterRecipeBook mRecipeBookAdapter;
    private AdapterChapter mChapterAdapter;
    private ChapterSelectedListener mListener;
    private Cursor mCursor;
    private long mBookId;
    private long recipeId;
    Context mContext;

    // Views Bound by ButterKnife
    @BindView(R.id.dialog_add_recipe_recyclerview)
    RecyclerView mRecyclerView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Instantiate the member variable
        mContext = getActivity();

        // Build the dialog to display to the user for selecing the recipe book/chapter
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        // Inflate the view used for the dialog
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_recyclerview, null);
        ButterKnife.bind(this, view);

        // Initialize the AdapterRecipeBook
        mRecipeBookAdapter = new AdapterRecipeBook(mContext, new AdapterRecipeBook.RecipeBookAdapterOnClickHandler() {
            @Override
            public void onClick(AdapterRecipeBook.RecipeBookViewHolder viewHolder, final long bookId) {
                if (isRecipeInBook(bookId)) {
                    final AlertDialog dialog = new AlertDialog.Builder(mContext).create();
                    dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            swapCursorChapter(bookId);
                            dialog.dismiss();
                        }
                    });

                    dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.button_deny), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialog.dismiss();
                        }
                    });

                    dialog.setMessage(getString(R.string.dialog_confirm_add_recipe_to_recipe_book));

                    dialog.show();
                } else {
                    swapCursorChapter(bookId);
                }


            }
        });

        // Initialize the AdapterChapter used for selecting the chapter the recipe is to be added to
        mChapterAdapter = new AdapterChapter(mContext);
        mChapterAdapter.useInDialog();
        mChapterAdapter.setChapterClickListener(new AdapterChapter.ChapterClickListener() {
            @Override
            public void onChapterClicked(long chapterId) {
                // Check if the recipe already exists in the chapter
                if (isRecipeInChapter(chapterId)) {
                    // Recipe exists, show a toast informing the user
                    Toast.makeText(mContext, R.string.toast_recipe_in_chapter, Toast.LENGTH_LONG).show();
                } else {
                    // Does not exist, add the recipe to the chapter
                    if (mListener != null) {
                        // Pass recipe book and chapter ID to the Activity requesting the dialog
                        mListener.onChapterSelected(mBookId, chapterId);

                        // Dismiss the dialog
                        dismiss();
                    }
                }
            }
        });

        // Get the number of columns to be used from
        int columnCount = ActivityRecipeList.mTwoPane ?
                getResources().getInteger(R.integer.recipe_twopane_columns) :
                getResources().getInteger(R.integer.recipe_columns);

        // Reduce the column count by one due to the dialog not filling the entire screen width
        if (columnCount > 1) columnCount--;

        // Initialize the StaggeredLayoutManager to be used for mRecyclerView
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        // Set the AdapterRecipeBook to mRecyclerView to start
        mRecyclerView.setAdapter(mRecipeBookAdapter);

        // Initialize the parameters used to query the database for the recipe book information
        Uri uri = RecipeBookEntry.CONTENT_URI;
        String[] projection = RecipeBookEntry.PROJECTION;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME + " ASC";

        // Query the database
        mCursor = mContext.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the Cursor inot mRecipeBookAdapter
        mRecipeBookAdapter.swapCursor(mCursor);

        // Set the inflated view to be used by the dialog
        builder.setView(view);

        return builder.create();
    }

    /**
     * Generates a Cursor for all chapters of a recipe book
     */
    private void swapCursorChapter(long bookId) {
        // Initialize member variable
        mBookId = bookId;

        // Set the AdapterRecipeBook to null so the AdapterChapter can be set to mRecyclerView
        mRecipeBookAdapter.swapCursor(null);
        mRecyclerView.setAdapter(mChapterAdapter);

        // Initialize parameters for querying database for chapters of a specific recipe book
        Uri uri = ChapterEntry.CONTENT_URI;
        String[] projection = ChapterEntry.PROJECTION;
        String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
        String[] selectionArgs = new String[] {Long.toString(bookId)};
        String sortOrder = ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

        // Query the database
        mCursor = mContext.getContentResolver().query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );

        // Swap the new Cursor into the AdapterChapter
        mChapterAdapter.swapCursor(mCursor);
    }

    /**
     * Method of passing the recipeId of the recipe to be added to a recipe book to check whether
     * the recipe book already contains the recipe
     * @param recipeId
     */
    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    /**
     * Listener for when a chapter has been selected for which the recipe is to be added to
     */
    public interface ChapterSelectedListener {
        void onChapterSelected(long bookId, long chapterId);
    }

    /**
     * Set the observer for when a chapter has been selected
     * @param listener ChapterSelectedListener to set as the member listener
     */
    public void setChapterSelectedListener(ChapterSelectedListener listener) {
        mListener = listener;
    }

    /**
     * Checks whether the recipe is in a given recipe book
     * @param bookId ID of the recipe book to be checked
     * @return Boolean value for whether the recipe book contains the recipe
     */
    private boolean isRecipeInBook(long bookId) {
        // Parameters for Cursor
        String selection = RecipeBookEntry.TABLE_NAME + "." +
                RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ? AND " +
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ?";

        String[] selectionArgs = new String[] {Long.toString(bookId), Long.toString(recipeId)};

        // Query the database to see if recipe book contains recipe
        Cursor cursor = getActivity().getContentResolver().query(
                LinkRecipeBookEntry.CONTENT_URI,
                LinkRecipeBookEntry.PROJECTION,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Recipe book contains recipe, close the Cursor
                cursor.close();
                return true;
            } else {
                // Does not contain recipe, close the Cursor
                cursor.close();
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks whether a recipe is in a given chapter
     * @param chapterId ID of the chapter to be checked
     * @return Boolean value for whether the chapter contains the recipe
     */
    private boolean isRecipeInChapter(long chapterId) {
        // Parameters for Cursor
        String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ? AND " +
                RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ?";

        String[] selectionArgs = new String[] {Long.toString(chapterId), Long.toString(recipeId)};

        // Query the database to see if chapter contains recipe
        Cursor cursor = getActivity().getContentResolver().query(
                LinkRecipeBookEntry.CONTENT_URI,
                LinkRecipeBookEntry.PROJECTION,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Recipe book contains recipe, close the Cursor
                cursor.close();
                return true;
            } else {
                // Does not contain recipe, close the Cursor
                cursor.close();
                return false;
            }
        } else {
            return false;
        }
    }
}
