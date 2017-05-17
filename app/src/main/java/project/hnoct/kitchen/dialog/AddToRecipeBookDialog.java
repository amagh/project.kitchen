package project.hnoct.kitchen.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.ui.ActivityRecipeList;
import project.hnoct.kitchen.ui.adapter.AdapterChapter;
import project.hnoct.kitchen.ui.adapter.AdapterRecipeBook;

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
            public void onClick(AdapterRecipeBook.RecipeBookViewHolder viewHolder, long bookId) {
                // Initialize member variable
                mBookId = bookId;

                // Set the AdapterRecipeBook to null so the AdapterChapter can be set to mRecyclerView
                mRecipeBookAdapter.swapCursor(null);
                mRecyclerView.setAdapter(mChapterAdapter);

                // Initialize parameters for querying database for chapters of a specific recipe book
                Uri uri = RecipeContract.ChapterEntry.CONTENT_URI;
                String[] projection = RecipeContract.ChapterEntry.PROJECTION;
                String selection = RecipeContract.RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(bookId)};
                String sortOrder = RecipeContract.ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

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
        });

        // Initialize the AdapterChapter used for selecting the chapter the recipe is to be added to
        mChapterAdapter = new AdapterChapter(mContext);
        mChapterAdapter.useInDialog();
        mChapterAdapter.setChapterClickListener(new AdapterChapter.ChapterClickListener() {
            @Override
            public void onChapterClicked(long chapterId) {
                if (mListener != null) {
                    // Pass recipe book and chapter ID to the Activity requesting the dialog
                    mListener.onChapterSelected(mBookId, chapterId);

                    // Dismiss the dialog
                    dismiss();
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
        Uri uri = RecipeContract.RecipeBookEntry.CONTENT_URI;
        String[] projection = RecipeContract.RecipeBookEntry.PROJECTION;
        String selection = null;
        String[] selectionArgs = null;
        String sortOrder = RecipeContract.RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME + " ASC";

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
}
