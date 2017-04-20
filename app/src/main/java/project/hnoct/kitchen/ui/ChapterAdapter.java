package project.hnoct.kitchen.ui;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import butterknife.Optional;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.view.NonScrollingRecyclerView;

/**
 * Created by hnoct on 3/22/2017.
 */

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = ChapterAdapter.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private CursorManager mCursorManager;
    private FragmentManager mFragmentManager;
    private RecipeClickListener mRecipeClickListener;
    private ChapterClickListener mChapterClickListener;
    private DeleteChapterListener mDeleteListener;
    private OnStartDragListener mDragListener;
    private RecipeAdapter[] mRecipeAdapterArray;
    private ItemTouchHelper[] mItemTouchHelperArray;
    private List<Map<String, Object>> mList;
    private boolean editMode = false;
    public int[] editOperations = {-1, -1, -1};
    private List<Integer> editChapters;

    // ViewTypes
    private static final int CHAPTER_VIEW_NORMAL = 0;
    private static final int CHAPTER_VIEW_EDIT = 1;
    private static final int CHAPTER_VIEW_EDIT_LAST = 2;

    // Map key values
    private String CHAPTER_ID = "chapterId";
    private String BOOK_ID = "bookId";
    private String CHAPTER_NAME = "chapterName";
    private String CHAPTER_DESCRIPTION = "chapterDescription";
    private String CHAPTER_ORDER = "chapterOrder";

    public ChapterAdapter(Context context, FragmentManager fragmentManager, CursorManager cursorManager) {
        mContext = context;
        mFragmentManager = fragmentManager;
        editChapters = new ArrayList<>();
        // Get the CursorManager passed from the activity so all the Cursors being managed can be
        // properly closed in onStop of the Activity
        mCursorManager = cursorManager;

        // Set the listener to manage the RecipeAdapters and their Cursors
        cursorManager.setCursorChangeListener(new CursorManager.CursorChangeListener() {
            @Override
            public void onCursorChanged(int position) {
                if (mRecipeAdapterArray != null && mRecipeAdapterArray[position] != null) {
                    mRecipeAdapterArray[position].swapCursor(mCursorManager.getCursor(position));
                    mRecipeAdapterArray[position].notifyDataSetChanged();
                }
            }
        });
    }

    public ChapterAdapter(Context context) {
        mContext = context;
    }

    /**
     * Changes the Cursor being used to display data
     * @param newCursor New Cursor to be used for data
     * @return Cursor currently used after the change
     */
    public Cursor swapCursor(Cursor newCursor) {
        // Check if the new Cursor is different than the old one
        if (newCursor != null && mCursor != newCursor) {
            // Set the member Cursor to the new Cursor
            mCursor = newCursor;

            if (mCursor.moveToFirst()) {
                // Initialize a new List to hold all values used to populate ViewHolder
                mList = new ArrayList<>(mCursor.getCount());

                do {
                    // Retrieve the chapter information from the Cursor
                    long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);
                    long bookId = mCursor.getLong(ChapterEntry.IDX_BOOK_ID);
                    String chapterName = mCursor.getString(ChapterEntry.IDX_CHAPTER_NAME);
                    String chapterDescription = mCursor.getString(ChapterEntry.IDX_CHAPTER_DESCRIPTION);
                    int chapterOrder = mCursor.getInt(ChapterEntry.IDX_CHAPTER_ORDER);

                    // Initialize HashMap to hold all the values
                    Map<String, Object> map = new HashMap<>();

                    // Insert all values into HashMap
                    map.put(CHAPTER_ID, chapterId);
                    map.put(BOOK_ID, bookId);
                    map.put(CHAPTER_NAME, chapterName);
                    map.put(CHAPTER_DESCRIPTION, chapterDescription);
                    map.put(CHAPTER_ORDER, chapterOrder);

                    // Add the HashMap to mList
                    mList.add(map);
                } while (mCursor.moveToNext());
            }

            // Reset the Array holding the RecipeAdapters being used
            mRecipeAdapterArray = new RecipeAdapter[mCursor.getCount()];
            mItemTouchHelperArray = new ItemTouchHelper[mCursor.getCount()];

            // Notify the Adapter of the change in data
            notifyDataSetChanged();
        }
        return mCursor;
    }

    void enterEditMode() {
        editMode = true;
        editChapters = new ArrayList<>();
        notifyDataSetChanged();
    }

    void exitEditMode() {
        editMode = false;

        if (editChapters.size() > 0) {
            EditChaptersAsyncTask asyncTask = new EditChaptersAsyncTask();
            asyncTask.execute();
        }

        notifyDataSetChanged();
    }

    boolean isInEditMode() {
        return editMode;
    }

    public List<Map<String, Object>> getList() {
        return mList;
    }

    @Override
    public ChapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            View view;
            switch (viewType) {
                // Inflate the layout to be used for the chapter list item
                case CHAPTER_VIEW_NORMAL: {
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_chapter, parent, false);
                    break;
                }
                case CHAPTER_VIEW_EDIT: {
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_chapter_edit, parent, false);
                    break;
                }
                default:
                    return null;
            }
            return new ChapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection!");
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (editMode) {
            return CHAPTER_VIEW_EDIT;
        } else {
            return CHAPTER_VIEW_NORMAL;
        }
    }

    @Override
    public void onBindViewHolder(ChapterViewHolder holder, final int position) {
        // Retrieve the HashMap holding all the values
        Map<String, Object> map = mList.get(position);

        // Retrieve the information to be used to populate the views within the view holder
        String chapterTitleText = (String) map.get(CHAPTER_NAME);
        String chapterDescriptionText = (String) map.get(CHAPTER_DESCRIPTION);

        // Populate the views of the view holder
        holder.titleText.setText(chapterTitleText);
        holder.descriptionText.setText(chapterDescriptionText);

        // Retrieve the column information from pre-defined resources
        int columnCount;

        if (RecipeListActivity.mTwoPane && RecipeListActivity.mDetailsVisible) {
            columnCount = mContext.getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columnCount = mContext.getResources().getInteger(R.integer.recipe_columns);
        }

        // Instantiate the layout manager to use the correct number of columns
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        // Check whether the orientation and screen size calls for utilization of the detailed view
        // within the RecipeAdapter
        final boolean useDetailView = mContext.getResources().getBoolean(R.bool.recipeAdapterUseDetailView);

        // Set the layout manager for the recycler view and set the adapter
        holder.recipeRecyclerView.setLayoutManager(sglm);

        // Attempt to get a reference to the RecipeAdapter in memory if it exists
        RecipeAdapter recipeAdapter;
        if ((recipeAdapter = mRecipeAdapterArray[position]) == null)  {
            // If there is no reference to a corresponding RecipeAdapter, initialize a new
            // RecipeAdapter
            recipeAdapter = new RecipeAdapter(mContext, new RecipeAdapter.RecipeAdapterOnClickHandler() {
                @Override
                public void onClick(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder) {
                    // Relay the click event and its data to the registered Observer
                    mRecipeClickListener.onRecipeClicked(recipeId, viewHolder);
                }
            });
            recipeAdapter.setPosition(position);

            // Set a reference to this RecipeAdapter in mRecipeAdapterArray
            mRecipeAdapterArray[position] = recipeAdapter;

            // Set whether the RecipeAdapter should use the DetailedView layout or master-flow/single
            if (mFragmentManager != null) recipeAdapter.setUseDetailView(useDetailView, mFragmentManager);
        }

        // Check if the Adapter is in edit mode
        if (editMode) {
            // If in edit mode, attempt to retrieve the ItemTouchHelper from mItemTouchHelperArray
            // or instantiate a new ItemTouchHelper if not found
            mItemTouchHelperArray[position] = mItemTouchHelperArray[position]!= null ?
                    mItemTouchHelperArray[position] :
                    new ItemTouchHelper(ithCallback);

            // Attach the ItemTouchHelper to the ViewHolder's RecyclerView
            mItemTouchHelperArray[position].attachToRecyclerView(holder.recipeRecyclerView);

            if (position == getItemCount() - 1) {
                // If the item is the last one in the Adapter, modify LayoutParams to increase the
                // bottom margin for uniformity
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.addRecipeButton.getLayoutParams();
                int px4 = (int) Utilities.convertDpToPixels(4);
                int px16 = (int) Utilities.convertDpToPixels(16);
                params.setMargins(px16, px4, px16, px16);

                // Set the new LayoutParams
                holder.addRecipeButton.setLayoutParams(params);
            }

        } else {
            // If not in edit mode, check to see if an ItemTouchHelper exists for the ViewHolder's
            // position.
            ItemTouchHelper helper = mItemTouchHelperArray[position];

            if (helper != null) {
                // If found, attach it to null RecyclerView
                helper.attachToRecyclerView(null);
            }
        }

        if (!editMode && recipeAdapter.isInEditMode()) {
            recipeAdapter.exitEditMode();
        } else if (editMode && !recipeAdapter.isInEditMode()) {
            recipeAdapter.enterEditMode();
        }

        recipeAdapter.setOnStartDragListener(new RecipeAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(RecipeAdapter.RecipeViewHolder viewHolder) {
                mItemTouchHelperArray[(int) viewHolder.itemView.getTag()].startDrag(viewHolder);
            }
        });

        // Swap the Cursor in the recipeAdapter with the new one
        if (mCursorManager != null) recipeAdapter.swapCursor(mCursorManager.getCursor(position));

        // Set the Adapter to the ViewHolder's RecyclerView
        holder.recipeRecyclerView.setAdapter(recipeAdapter);
    }

    /**
     * Interface for a listener to notify a registered observer of whether a specific recipe was
     * selected or if user clicked to add a new recipe to the Chapter
     */
    interface RecipeClickListener {
        void onRecipeClicked(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder);
        void onAddRecipeClicked(long chapterId);
    }

    interface OnStartDragListener {
        void onStartDrag(ChapterViewHolder viewHolder);
    }

    public interface ChapterClickListener {
        void onChapterClicked(long chapterId);
    }

    public void setChapterClickListener(ChapterClickListener listener) {
        mChapterClickListener = listener;
    }

    void setOnStartDragListener(OnStartDragListener listener) {
        mDragListener = listener;
    }

    /**
     * Registers an observer to be notified of user interaction with the Adapter
     * @param listener
     */
    void setRecipeClickListener(RecipeClickListener listener) {
        mRecipeClickListener = listener;
    }

    interface DeleteChapterListener {
        void onDelete(int position);
    }

    void setDeleteChapterListener(DeleteChapterListener listener) {
        mDeleteListener = listener;
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mList.size();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        // ItemId will be the recipeId since it is a UNIQUE primary key
        // Allows for smoother scrolling with StaggeredGridLayout and less shuffling
        return (long) mList.get(position).get(CHAPTER_ID);
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // Views bound by ButterKnife
        @BindView(R.id.list_chapter_title) TextView titleText;
        @BindView(R.id.list_chapter_description) TextView descriptionText;
        @BindView(R.id.list_chapter_recipe_recyclerview) NonScrollingRecyclerView recipeRecyclerView;
        @Nullable @BindView(R.id.list_chapter_add_recipe) CardView addRecipeButton;
        @Nullable @BindView(R.id.list_chapter_touchpad) ImageView touchpad;
        @Nullable @BindView(R.id.list_chapter_delete) ImageView deleteButton;
//        @BindView(R.id.list_chapter_title) EditText titleText;
//        @BindView(R.id.list_chapter_description) EditText descriptionText;

        @Optional
        @OnClick(R.id.list_chapter_add_recipe)
        /**
         * Notifies the registered observer that the user has selected to add a recipe to the chapter
         */
        void addRecipe() {
            // Get the position of the Chapter the recipe is to be added to
            int position = getAdapterPosition();

            // Get the chapterId
            mCursor.moveToPosition(position);
            long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

            // Notify the observer to allow the user to select a new recipe to be added
            if (mRecipeClickListener != null) mRecipeClickListener.onAddRecipeClicked(chapterId);
        }

        @Optional
        @OnTouch(R.id.list_chapter_touchpad)
        boolean onTouch(View view, MotionEvent event) {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                // Set the drag handle to handle the drag event for moving chapters
                if (mDragListener != null) {
                    mDragListener.onStartDrag(this);
                    return true;
                }
            }
            return false;
        }

        @Optional
        @OnClick(R.id.list_chapter_delete)
        void onClickDelete(View view) {
            int position = getAdapterPosition();

            if (mDeleteListener != null) mDeleteListener.onDelete(position);
        }

        @Optional
        @OnTextChanged(R.id.list_chapter_title)
        void onTitleChanged(CharSequence text) {
            int position = getAdapterPosition();

            // Add the new title to mList
            mList.get(position).put(CHAPTER_NAME, text.toString());

            // Add the entry to list of items to be updated
            if (editChapters != null && !editChapters.contains(position)) editChapters.add(position);
        }

        @Optional
        @OnTextChanged(R.id.list_chapter_description)
        void onDescriptionChanged(CharSequence text) {
            int position = getAdapterPosition();

            // Add the new description to mList
            mList.get(position).put(CHAPTER_DESCRIPTION, text.toString());

            // Add the entry to list of items to be updated
            if (editChapters != null && !editChapters.contains(position)) editChapters.add(position);
        }


        public ChapterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mChapterClickListener != null) {
                // Get the Adapter's position
                int position = getAdapterPosition();

                // Move the Cursor into position
                mCursor.moveToPosition(position);

                // Get the chapter ID
                long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

                // Send the chapter ID to mChapterClickListener
                mChapterClickListener.onChapterClicked(chapterId);
            }
        }
    }

    private ItemTouchHelper.Callback ithCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // Get the positions of the ViewHolder's current and target position
            int start = viewHolder.getAdapterPosition();
            int end = target.getAdapterPosition();

            // Retrieve the RecipeAdapter being modified
            RecipeAdapter adapter = (RecipeAdapter) recyclerView.getAdapter();

            // Add instructions for how the database should be re-arranged
            // Check to see if the initial position of the edit action has already been set
            if (adapter.editInstructions[0] == -1) {
                // If the initial position has not been set, set it to the start position
                adapter.editInstructions[0] = start;
            }

            // Set the end instruction as the target's position
            adapter.editInstructions[1] = end;

            // Change the order of the item within the list being utilized by RecipeAdapter
            if (start < end) {
                for (int i = start; i < end; i++) {
                    Collections.swap(adapter.getList(), i, i + 1);
                }
            } else {
                for (int i = start; i > end; i--) {
                    Collections.swap(adapter.getList(), i, i - 1);
                }
            }

            // Notify the adapter of the change
            adapter.notifyItemMoved(start, end);

            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            RecipeAdapter adapter = mRecipeAdapterArray[(int) viewHolder.itemView.getTag()];
            if (editMode) {
                // Get the position of the ViewHolder being removed
                int position = viewHolder.getAdapterPosition();
                adapter.getList().remove(position);
                adapter.notifyItemRemoved(position);

                // Add an instruction for which position should be deleted from the database
                adapter.editInstructions[2] = position;
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            // Modify the database when the user-releases the item being dragged so that update
            // operations in the database are kept to a minimum
            RecipeAdapter adapter = (RecipeAdapter) recyclerView.getAdapter();

            // Check to see if both the start and end position of the edit instructions have been
            // correctly set
            if (adapter.editInstructions[0] == -1 && adapter.editInstructions[1] == -1 && adapter.editInstructions[2] == -1) {
                // If no instructions exist, do nothing
                return;
            } else  {
                // Initialize parameters for creating a new Cursor
                mCursor.moveToPosition((int) viewHolder.itemView.getTag());
                long bookId = mCursor.getLong(ChapterEntry.IDX_BOOK_ID);
                long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

                Uri uri = LinkRecipeBookEntry.buildRecipeUriFromBookAndChapterId(bookId, chapterId);
                String[] projection = LinkRecipeBookEntry.PROJECTION;
                String selection =
                        RecipeBookEntry.TABLE_NAME + "." + RecipeBookEntry.IDX_BOOK_ID + " = ? AND " +
                                ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(bookId), Long.toString(chapterId)};
                String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " ASC";

                // Initialize Cursor to pass the ModifyDatabaseAsyncTask
                Cursor cursor = mContext.getContentResolver().query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        sortOrder
                );


                if (adapter.editInstructions[2] != -1) {
                    // Get the removal position
                    int remove = adapter.editInstructions[2];

                    // Execute the AsyncTask with the removal parameter
                    ModifyDatabaseAsyncTask asyncTask = new ModifyDatabaseAsyncTask(cursor, remove);
                    asyncTask.execute();
                } else if (adapter.editInstructions[0] != -1 && adapter.editInstructions[1] != -1) {
                    // Get the start and end positions
                    int start = adapter.editInstructions[0];
                    int end = adapter.editInstructions[1];

                    if (start == end) {
                        // If both the start position and the end position are the same, do nothing
                        return;
                    }

                    // Execute the AsyncTask with the parameters
                    ModifyDatabaseAsyncTask asyncTask = new ModifyDatabaseAsyncTask(cursor, start, end);
                    asyncTask.execute();

                }
            }

            // Reset the editInstructions for the next operation
            adapter.editInstructions = new int[] {-1, -1, -1};
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Disable long press drag to move items. Drag is initiated from the ViewHolder's handle
            return false;
        }

        class ModifyDatabaseAsyncTask extends AsyncTask<Object, Void, Void> {
            private Cursor cursor;      // Used to iterate through all values in the database that need to be modified
            private int start = -1;     // Recipe order of the entry that needs to be modified
            private int end = -1;       // Recipe order that the entry will end in
            private int remove = -1;    // Recipe order of the entry to be removed

            ModifyDatabaseAsyncTask(Cursor cursor, int start, int end) {
                // Initialize member variables
                this.cursor = cursor;
                this.start = start;
                this.end = end;
            }

            ModifyDatabaseAsyncTask(Cursor cursor, int remove) {
                // Initialize member variables
                this.cursor = cursor;
                this.remove = remove;
            }

            @Override
            protected Void doInBackground(Object... args) {
                // Initialize an ArrayList to hold all the update operations to be performed
                ArrayList<ContentProviderOperation> operations = new ArrayList<>();

                // Selection for update instructions
                String updateSelection =
                        RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ? AND " +
                                ChapterEntry.COLUMN_CHAPTER_ID + " = ? AND " +
                                RecipeEntry.COLUMN_RECIPE_ID + " = ?";

                // Check whether the recipe order needs to be modified or if an entry needs to be removed
                if (start != -1 && end != -1) {
                    // Check to see ensure Cursor is valid
                    if (cursor != null && cursor.moveToFirst()) {
                        long mBookId = -1, mChapterId = -1, mRecipeId = 0;

                        if (end > start) {
                            do {
                                int oldPosition = cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER);
                                int newPosition;

                                if (oldPosition < start) {
                                    continue;
                                } else if (oldPosition == start) {
                                    // The item being moved first needs to move to the back of the database
                                    // so the other items can be shifted without violating the UNIQUE constraint
                                    newPosition = cursor.getCount() + 10;

                                    // Initialize parameters for the final operation of the recipe into the
                                    // final position
                                    mBookId = cursor.getLong(LinkRecipeBookEntry.IDX_BOOK_ID);
                                    mChapterId = cursor.getLong(LinkRecipeBookEntry.IDX_CHAPTER_ID);
                                    mRecipeId = cursor.getLong(LinkRecipeBookEntry.IDX_RECIPE_ID);
                                } else if (oldPosition <= end) {
                                    // Move each recipe down one in ordering
                                    newPosition = oldPosition - 1;
                                } else {
                                    // Skip recipes that do not need to be moved
                                    continue;
                                }

                                // Retrieve parameters for updating the order of the recipes
                                long bookId = cursor.getLong(LinkRecipeBookEntry.IDX_BOOK_ID);
                                long chapterId = cursor.getLong(LinkRecipeBookEntry.IDX_CHAPTER_ID);
                                long recipeId = cursor.getLong(LinkRecipeBookEntry.IDX_RECIPE_ID);

                                // Initialize the selection arguments
                                String[] updateArgs = new String[]{Long.toString(bookId), Long.toString(chapterId), Long.toString(recipeId)};

                                // Generate the update operation
                                ContentProviderOperation operation = ContentProviderOperation
                                        .newUpdate(LinkRecipeBookEntry.CONTENT_URI)
                                        .withSelection(updateSelection, updateArgs)
                                        .withValue(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, newPosition)
                                        .build();

                                // Add the operation to the list of operations to perform in batch
                                operations.add(operation);

                            } while (cursor.moveToNext());
                        } else {
                            cursor.moveToLast();
                            do {
                                int oldPosition = cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER);
                                int newPosition;

                                if (oldPosition > start) {
                                    continue;
                                } else if (oldPosition == start) {
                                    // The item being moved first needs to move to the back of the database
                                    // so the other items can be shifted without violating the UNIQUE constraint
                                    newPosition = cursor.getCount() + 10;

                                    // Initialize parameters for the final operation of the recipe into the
                                    // final position
                                    mBookId = cursor.getLong(LinkRecipeBookEntry.IDX_BOOK_ID);
                                    mChapterId = cursor.getLong(LinkRecipeBookEntry.IDX_CHAPTER_ID);
                                    mRecipeId = cursor.getLong(LinkRecipeBookEntry.IDX_RECIPE_ID);
                                } else if (oldPosition >= end) {
                                    newPosition = oldPosition + 1;
                                } else {
                                    continue;
                                }

                                // Retrieve parameters for updating the order of the recipes
                                long bookId = cursor.getLong(LinkRecipeBookEntry.IDX_BOOK_ID);
                                long chapterId = cursor.getLong(LinkRecipeBookEntry.IDX_CHAPTER_ID);
                                long recipeId = cursor.getLong(LinkRecipeBookEntry.IDX_RECIPE_ID);

                                // Initialize the selection arguments
                                String[] updateArgs = new String[]{Long.toString(bookId), Long.toString(chapterId), Long.toString(recipeId)};

                                // Generate the update operation
                                ContentProviderOperation operation = ContentProviderOperation
                                        .newUpdate(LinkRecipeBookEntry.CONTENT_URI)
                                        .withSelection(updateSelection, updateArgs)
                                        .withValue(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, newPosition)
                                        .build();

                                // Add the operation to the list of operations to perform in batch
                                operations.add(operation);

                            } while (cursor.moveToPrevious());

                        }

                        // Initialize the selection arguments for the final operation -- move the
                        // entry to its final recipe order
                        String[] updateArgs = new String[]{Long.toString(mBookId), Long.toString(mChapterId), Long.toString(mRecipeId)};

                        // Generate the operation to perform
                        ContentProviderOperation operation = ContentProviderOperation
                                .newUpdate(LinkRecipeBookEntry.CONTENT_URI)
                                .withSelection(updateSelection, updateArgs)
                                .withValue(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, end)
                                .build();

                        // Add it to the list
                        operations.add(operation);
                    }
                } else if (remove != -1) {
                    if (cursor != null && cursor.moveToPosition(remove)) {
                        // Remove the entry requested by the user and then shift all other entries
                        // up one position
                        do {
                            // Get the values in the Cursor
                            long bookId = cursor.getLong(LinkRecipeBookEntry.IDX_BOOK_ID);
                            long chapterId = cursor.getLong(LinkRecipeBookEntry.IDX_CHAPTER_ID);
                            long recipeId = cursor.getLong(LinkRecipeBookEntry.IDX_RECIPE_ID);
                            int recipeOrder = cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER);

                            // Generate the selection arguments used for removal/update
                            String[] updateArgs = new String[]{Long.toString(bookId), Long.toString(chapterId), Long.toString(recipeId)};

                            if (recipeOrder == remove) {
                                // If the recipeOrder is the same as the one to remove, set up an
                                // operation to remove the entry
                                ContentProviderOperation operation = ContentProviderOperation
                                        .newDelete(LinkRecipeBookEntry.CONTENT_URI)
                                        .withSelection(updateSelection, updateArgs)
                                        .build();

                                // Add it to the list
                                operations.add(operation);
                            } else {
                                // Generate a update operation
                                ContentProviderOperation operation = ContentProviderOperation
                                        .newUpdate(LinkRecipeBookEntry.CONTENT_URI)
                                        .withSelection(updateSelection, updateArgs)
                                        .withValue(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, recipeOrder - 1)
                                        .build();

                                // Add it to the list
                                operations.add(operation);
                            }
                        } while (cursor.moveToNext());
                    }
                } else {
                    // If there is no start, end, or remove parameter, do nothing
                    return null;
                }

                try {
                    // Apply all update operations for entries affected by the change
                    mContext.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, operations);
                } catch (RemoteException | OperationApplicationException e) {
                    e.printStackTrace();
                }
                // Close the passed Cursor
                cursor.close();

                return null;
            }
        }
    };

    /**
     * AsyncTask for updating the database with new chapter title and descriptions
     */
    class EditChaptersAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            // Check to see if the chapters have been edited
            if (editChapters.size() > 0) {
                // Initialize an ArrayList to hold the operations to perform
                ArrayList<ContentProviderOperation> operations = new ArrayList<>(editChapters.size());

                // Selection for update
                String selection = ChapterEntry.COLUMN_CHAPTER_ID + " = ?";

                // Iterate through each entry
                for (int i : editChapters) {
                    // If Cursor is null or invalid, then do nothing
                    if (mCursor == null || mCursor.isClosed() || !mCursor.moveToFirst()) return null;

                    // Move the Cursor to position being edited
                    mCursor.moveToPosition(i);

                    // Retrieve the chapterId of the entry to be edited
                    long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);

                    // Selection argument for the update selection
                    String[] selectionArgs = new String[] {Long.toString(chapterId)};

                    // Get the new title and description to update the database
                    String newName = (String) mList.get(i).get(CHAPTER_NAME);
                    String newDescription = (String) mList.get(i).get(CHAPTER_DESCRIPTION);

                    // Create the update operation
                    ContentProviderOperation operation = ContentProviderOperation
                            .newUpdate(ChapterEntry.CONTENT_URI)
                            .withSelection(selection, selectionArgs)
                            .withValue(ChapterEntry.COLUMN_CHAPTER_NAME, newName)
                            .withValue(ChapterEntry.COLUMN_CHAPTER_DESCRIPTION, newDescription)
                            .build();

                    // Add the operation to the list
                    operations.add(operation);
                }

                try {
                    // Batch apply all update operations
                    mContext.getContentResolver().applyBatch(RecipeContract.CONTENT_AUTHORITY, operations);
                } catch (RemoteException | OperationApplicationException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}
