package project.hnoct.kitchen.ui;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.view.SlidingAlphabeticalIndex;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentChapter extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FragmentChapter.class.getSimpleName();
    private static final int CHAPTER_LOADER = 4;
    static final String RECIPE_BOOK_URI = "recipe_book_uri";
    private static final int POSITION_MODIFIER = 10000;

    /** Member Variables **/
    AdapterChapter mChapterAdapter;

    private Context mContext;
    private Cursor mCursor;
    private CursorManager mCursorManager;
    private Uri mRecipeBookUri;
    private int mPosition;
    private ItemTouchHelper mHelper;
    private Map<String, Integer> mIndex;
    private StaggeredGridLayoutManager mLayoutManager;
    private String alphabet;

    @BindView(R.id.chapter_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.chapter_alphabetical_index) SlidingAlphabeticalIndex mSlidingIndex;

    public FragmentChapter() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_chapter, container, false);
        ButterKnife.bind(this, view);
        setHasOptionsMenu(true);

        // Instantiate member variables
        mContext = getActivity();

        // Get the Uri of the recipe book to be accessed
        Bundle bundle = getArguments();
        if (bundle != null) {
            mRecipeBookUri = bundle.getParcelable(RECIPE_BOOK_URI);
        }

        mChapterAdapter = new AdapterChapter(
                mContext,
                getChildFragmentManager(),
                mCursorManager = new CursorManager(mContext)
        );

        // Set the layout manager and the number of columns to use
        setLayoutColumns();

        mRecyclerView.setAdapter(mChapterAdapter);
        mRecyclerView.setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        mChapterAdapter.setRecipeClickListener(new AdapterChapter.RecipeClickListener() {
            @Override
            public void onRecipeClicked(long recipeId, AdapterRecipe.RecipeViewHolder viewHolder) {
                String recipeUrl = Utilities.getRecipeUrlFromRecipeId(mContext, recipeId);
                ((ActivityChapter) getActivity()).onRecipeSelected(recipeUrl, viewHolder);
            }

            @Override
            public void onAddRecipeClicked(long chapterId) {
                ((ActivityChapter) getActivity()).onAddRecipeClicked(chapterId);
            }
        });

        // Set a listener to restart the CursorLoader when a new chapter has been added
        ((ActivityChapter) getActivity()).setChapterListener(new ActivityChapter.ChapterListener() {
            @Override
            public void onNewChapter() {
                restartLoader();
            }
        });

        // Initialize the Listener to observe when the drag handle has been touched
        mChapterAdapter.setOnStartDragListener(new AdapterChapter.OnStartDragListener() {
            @Override
            public void onStartDrag(AdapterChapter.ChapterViewHolder viewHolder) {
                mHelper.startDrag(viewHolder);
            }
        });

        // Initialize the Listener to observe when a chapter has been deleted
        mChapterAdapter.setDeleteChapterListener(new AdapterChapter.DeleteChapterListener() {
            @Override
            public void onDelete(final int position) {
                // Show a dialog confirming the user's desire to delete the chapter
                final AlertDialog dialog = new AlertDialog.Builder(mContext).create();

                // Set the message to alert the user
                dialog.setMessage("Are you sure you want to remove this chapter and all its contents?");
                dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // If user confirms, delete the chapter
                        removeChapter(position);
                        dialog.dismiss();
                    }
                });

                dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Dismiss the dialog and do nothing
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        // Initialize the ItemTouchHelper for interacting with mRecyclerView
        mHelper = new ItemTouchHelper(ithCallback);

        // Attach the ItemTouchHelper to mRecyclerView
        mHelper.attachToRecyclerView(null);

        // Override the back key so that when in edit-mode it returns to regular mode
        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (mChapterAdapter.isInEditMode() && keyCode == KeyEvent.KEYCODE_BACK) {
                    mChapterAdapter.exitEditMode();
                    return false;
                }
                return false;
            };
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_chapter_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit_mode: {
                if (mChapterAdapter.isInEditMode()) {
                    mChapterAdapter.exitEditMode();

                    // Attach the ItemTouchHelper to a null RecyclerView
                    mHelper.attachToRecyclerView(null);

                    // Calculate the number of pixels for the 4dp margin
                    int px4 = (int) Utilities.convertDpToPixels(4);

                    // Set the margin for the right side of mRecyclerView to prevent interference
                    // from the SlidingAlphabeticalIndex
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams();
                    params.setMargins(0, 0, px4, 0);

                    // Set the LayoutParams to mRecyclerView
                    mRecyclerView.setLayoutParams(params);

                    // Hide the soft keyboard
                    InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
                } else {
                    mChapterAdapter.enterEditMode();

                    // Attach the ItemTouchHelper to mRecyclerView
                    mHelper.attachToRecyclerView(mRecyclerView);

                    // Calculate the number of pixels for the 20dp margin
                    int px20 = (int) Utilities.convertDpToPixels(20);

                    // Set margin and LayoutParams to mRecyclerView
                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams();
                    params.setMargins(0, 0, px20, 0);

                    mRecyclerView.setLayoutParams(params);
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void restartLoader() {
        getLoaderManager().restartLoader(CHAPTER_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (mRecipeBookUri != null && id == CHAPTER_LOADER) {
            // Get the recipe book ID that is being queried from the Uri passed
            long recipeBookId = RecipeBookEntry.getRecipeBookIdFromUri(mRecipeBookUri);

            // Instantiate the parameters used to query the database
            Uri uri = ChapterEntry.CONTENT_URI;
            String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
            String[] selectionArgs = new String[] {Long.toString(recipeBookId)};
            String sortOrder = ChapterEntry.COLUMN_CHAPTER_ORDER + " ASC";

            // Build and return the CursorLoader
            return new CursorLoader(
                    mContext,
                    uri,
                    ChapterEntry.PROJECTION,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        } else if (args != null) {
            // The Loader is for Cursors used by RecyclerViews within AdapterChapter
            // Retrieve the arguments from the passed Bundle
            Uri uri = args.getParcelable(Utilities.URI);
            String[] projection = args.getStringArray(Utilities.PROJECTION);
            String selection = args.getString(Utilities.SELECTION);
            String[] selectionArgs = args.getStringArray(Utilities.SELECTION_ARGS);
            String sortOrder = args.getString(Utilities.SORT_ORDER);

            // Return a new CursorLoader with the given parameters
            return new CursorLoader(
                    mContext,
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
            );
        } else {
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == CHAPTER_LOADER) {
            if (cursor != null && cursor.moveToFirst()) {
                mIndex = mSlidingIndex.getIndex();
                mCursor = cursor;

                // After all the other required Cursors have been initialized, swap the Cursor into
                // AdapterChapter
                mChapterAdapter.swapCursor(mCursor);

                // Build a String containing an entry for each chapter
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < mCursor.getCount(); i++) {
                    builder.append(i);
                }

                // Set the alphabet to be used for fast-scrolling to the built String
                alphabet = builder.toString();

                // Set the alphabet to be used by the SlidingAlphabeticalIndex
                mSlidingIndex.setAlphabet(alphabet);

                // Set the Listener for when the user interacts with mSlidingIndex
                mSlidingIndex.setOnValueChangedListener(new SlidingAlphabeticalIndex.OnValueChangedListener() {
                    @Override
                    public void onValueChanged(int value) {
                        scrollToIndex(Character.toString(alphabet.charAt(value)));
                    }
                });

                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    // Retrieve the parameters for generating the URI for querying the database
                    long chapterId = cursor.getLong(ChapterEntry.IDX_CHAPTER_ID);
                    long recipeBookId = cursor.getLong(ChapterEntry.IDX_BOOK_ID);

                    // Instantiate the Cursor by querying for the recipes of the chapter
                    Uri recipesOfChapterUri = LinkRecipeBookEntry.buildRecipeUriFromBookAndChapterId(
                            recipeBookId,
                            chapterId
                    );

                    // Initialize parameters for querying the database
                    String[] projection = RecipeEntry.RECIPE_PROJECTION;
                    String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                    String[] selectionArgs = new String[] {Long.toString(chapterId)};
                    String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " ASC";

                    // Pass the parameters to a Bundle to be passed to the new CursorLoaders
                    Bundle args = new Bundle();
                    args.putParcelable(getString(R.string.uri_key), recipesOfChapterUri);
                    args.putStringArray(getString(R.string.projection_key), projection);
                    args.putString(getString(R.string.selection_key), selection);
                    args.putStringArray(getString(R.string.selection_args_key), selectionArgs);
                    args.putString(getString(R.string.sort_order_key), sortOrder);

                    // Add the position to mIndex used for scrolling
                    mIndex.put(Integer.toString(i), i);

                    // Initialize a new Loader
                    generateHelperLoader(i, args);
                }
            }

        } else {
            mCursorManager.addManagedCursor(loader.getId() - POSITION_MODIFIER, cursor);
        }
    }

    /**
     * Fast scrolls to the chapter selected by mSlidingIndex
     * @param value The number of the chapter to scroll to
     */
    void scrollToIndex(String value) {
        // Get the position within the index of the chapter
        int position = mIndex.get(value);

        // Request focus to prevent the EditText from stopping the scrolling
        mRecyclerView.requestFocus();

        // Scroll the view
        mLayoutManager.scrollToPositionWithOffset(position, 0);
    }

    /**
     * Generates a CursorLoader for the embedded RecyclerViews
     * @param position Position of the ViewHolder requesting the Cursor
     * @param args Parameters used to generate the CursorLoader
     */
    private void generateHelperLoader(int position, Bundle args) {
        // Check whether the CursorLoader has already been started
        if (getLoaderManager().getLoader(position + POSITION_MODIFIER) != null) {
            // If it has already been started, restart it
            getLoaderManager().restartLoader(position + POSITION_MODIFIER, args, this);
        } else {
            // If it has not been started, initialize a new CursorLoader
            getLoaderManager().initLoader(position + POSITION_MODIFIER, args, this);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap a null Cursor into AdapterChapter
        mChapterAdapter.swapCursor(null);

//        // Close all previously opened Cursors used by the AdapterChapter
//        mCursorManager.closeAllCursors();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initialize the Cursor Loader
        getLoaderManager().initLoader(CHAPTER_LOADER, null, this);
    }

    /**
     * Callback Interface to notify ActivityChapter of user interactions
     */
    interface RecipeCallBack {
        /**
         * Adds a recipe to a chapter
         * @param recipeUrl URL of the recipe to be added
         * @param viewHolder ViewHolder holding the recipe's information
         */
        void onRecipeSelected(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder);

        /**
         * For opening a dialog allowing the user to select a recipe to add
         * @param chapterId ID of the chapter that is adding a new recipe
         */
        void onAddRecipeClicked(long chapterId);
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    private void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns = 1;

        // Instantiate the LayoutManager
        mLayoutManager = new StaggeredGridLayoutManager(
                columns,
                StaggeredGridLayoutManager.VERTICAL
        );

        // Set the LayoutManager for the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);

        AdapterRecipe adapter = ((AdapterRecipe) mRecyclerView.getAdapter());
        if (adapter != null) {
            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        mLayoutManager.scrollToPositionWithOffset(mPosition, 0);
    }

    void removeChapter(int position) {
        // Remove the entry from mChapterAdapter's list
        mChapterAdapter.getList().remove(position);

        // Notify the Adapter of the change
        mChapterAdapter.notifyItemRemoved(position);

        // Remove the entry from the database in the background
        ModifyDatabase asyncTask = new ModifyDatabase(position);
        asyncTask.execute();
    }

    ItemTouchHelper.SimpleCallback ithCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // Retrieve the List being used to by the AdapterChapter
            List<Map<String, Object>> chapterList = mChapterAdapter.getList();

            // Get the start and end positions for the swap
            int start = viewHolder.getAdapterPosition();
            int end = target.getAdapterPosition();

            // Swap the items in the position from start to end
            if (start < end) {
                for (int i = start; i < end; i++) {
                    Collections.swap(chapterList, i, i + 1);
                }
            } else {
                for (int i = start; i > end; i--) {
                    Collections.swap(chapterList, i, i - 1);
                }
            }

            // Set the database operations within mChapterAdapter
            if (mChapterAdapter.editOperations[0] == -1) {
                // Start operation is only set if it hasn't already been set
                mChapterAdapter.editOperations[0] = start;
            }
            mChapterAdapter.editOperations[1] = end;

            // Notify mChapterAdapter of the chagne
            mChapterAdapter.notifyItemMoved(start, end);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Get the position of the ViewHolder swiped to remove
            int position = viewHolder.getAdapterPosition();

        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);

            // Retrieve the edit operation initiated by the user
            int start = mChapterAdapter.editOperations[0];
            int end = mChapterAdapter.editOperations[1];
            int remove = mChapterAdapter.editOperations[2];

            // Initialize the AsyncTask for editing the database
            ModifyDatabase asyncTask;

            // Check whether the user has initiated as a swap or removal operation
            if (start != -1 && end != -1) {
                if (start == end) {
                    // If start and end positions are the same, do nothing
                    return;
                }
                // Swap procedure
                asyncTask = new ModifyDatabase(start, end);
            } else {
                // No edit operations initiated by the user, do nothing
                return;
            }

            // Execute the AsyncTask for modifying the database
            asyncTask.execute();

            // Reset the edit operations
            mChapterAdapter.editOperations = new int[] {-1, -1, -1};
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Disable long-press drag. Utilizing drag handle
            return false;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            // Disable swipe to delete
            return false;
        }
    };

    class ModifyDatabase extends AsyncTask<Void, Void, Void> {
        // Member variables
        int start = -1;
        int end = -1;
        int remove = -1;

        /**
         * Constructor for a swap procedure
         * @param start Position of the entry to be moved
         * @param end Position that the entry should end in
         */
        ModifyDatabase(int start, int end) {
            this.start = start;
            this.end = end;
        }

        /**
         * Constructor for a removal procedure
         * @param remove Position of the entry to be removed
         */
        ModifyDatabase(int remove) {
            this.remove = remove;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Initialize the ArrayList that will hold all operations that need to be applied in
            // batch
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            // Selection for update
            String selection = ChapterEntry.COLUMN_CHAPTER_ID + " = ?";

            // Determine whether an entry needs to be moved or removed from the database
            if (start != -1 && end != -1) {
                // Move the cursor to the position of the item that needs to be moved
                mCursor.moveToPosition(start);

                // Create variable to hold the chapterId of the entry that needs to be moved
                long mChapterId = -1;

                for (int i = 0; i < Math.abs(end - start) + 1; i++) {
                    // Initialize variables
                    int newPosition;
                    int oldPosition = mCursor.getInt(ChapterEntry.IDX_CHAPTER_ORDER);

                    if (oldPosition == start) {
                        // Move the selected entry to the end of the database
                        newPosition = mCursor.getCount() + 10;

                        mChapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);
                    } else if (start < end) {
                        // Moving an entry to a higher sort order
                        if (oldPosition > start && oldPosition <= end) {
                            newPosition = oldPosition - 1;
                        } else {
                            continue;
                        }
                    } else {
                        // Moving an entry to a lower sort order
                        if (oldPosition < start && oldPosition >= end) {
                            newPosition = oldPosition + 1;
                        } else {
                            continue;
                        }
                    }

                    // Retrieve the ChapterId of the entry being modified
                    long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

                    // Update arguments
                    String[] selectionArgs = new String[] {Long.toString(chapterId)};

                    // Create the update operation
                    ContentProviderOperation operation = ContentProviderOperation
                            .newUpdate(ChapterEntry.CONTENT_URI)
                            .withSelection(selection, selectionArgs)
                            .withValue(ChapterEntry.COLUMN_CHAPTER_ORDER, newPosition)
                            .build();

                    // Add the update operation to the list of operations to be performed
                    operations.add(operation);

                    // Depending on whether items need to be shifted up or down, move the Cursor
                    if (start < end ) {
                        mCursor.moveToNext();
                    } else {
                        mCursor.moveToPrevious();
                    }
                }
                // Update arguments
                String[] selectionArgs = new String[] {Long.toString(mChapterId)};

                // Add the final update operation that will place the selected entry at the
                // 'end' position
                ContentProviderOperation operation = ContentProviderOperation
                        .newUpdate(ChapterEntry.CONTENT_URI)
                        .withSelection(selection, selectionArgs)
                        .withValue(ChapterEntry.COLUMN_CHAPTER_ORDER, end)
                        .build();

                operations.add(operation);
            } else if (remove != -1) {
                // Move the Cursor into position
                mCursor.moveToPosition(remove);
                do {
                    // Initialize variables
                    int newPosition;
                    int oldPosition = mCursor.getInt(ChapterEntry.IDX_CHAPTER_ORDER);
                    long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

                    // Populate the selection argument for the update/removal operation
                    String[] selectionArgs = new String[] {Long.toString(chapterId)};

                    if (oldPosition == remove) {
                        // Remove the entry selected by the user
                        ContentProviderOperation operation = ContentProviderOperation
                                .newDelete(ChapterEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .build();

                        // Add the operation to the list
                        operations.add(operation);
                    } else {
                        // Move all following entries up one
                        newPosition = oldPosition - 1;
                        ContentProviderOperation operation = ContentProviderOperation
                                .newUpdate(ChapterEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .withValue(ChapterEntry.COLUMN_CHAPTER_ORDER, newPosition)
                                .build();

                        // Add the operation to the list
                        operations.add(operation);
                    }
                } while (mCursor.moveToNext());
            } else {
                return null;
            }

            try {
                // Perform the update/removal operations
                mContext.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, operations);
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
