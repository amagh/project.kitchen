package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;

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
    private RecipeClickListener mListener;
    private RecipeAdapter[] mRecipeAdapterArray;

    public ChapterAdapter(Context context, FragmentManager fragmentManager, CursorManager cursorManager) {
        mContext = context;
        mFragmentManager = fragmentManager;
        // Get the CursorManager passed from the activity so all the Cursors being managed can be
        // properly closed in onStop of the Activity
        mCursorManager = cursorManager;

        // Set the listener to manage the RecipeAdapters and their Cursors
        cursorManager.setCursorChangeListener(new CursorManager.CursorChangeListener() {
            @Override
            public void onCursorChanged(int position) {
                Log.d(LOG_TAG, "Cursor changed!");
                mRecipeAdapterArray[position].swapCursor(mCursorManager.getCursor(position));
                mRecipeAdapterArray[position].notifyDataSetChanged();
            }
        });
    }

    /**
     * Changes the Cursor being used to display data
     * @param newCursor New Cursor to be used for data
     * @return Cursor currently used after the change
     */
    Cursor swapCursor(Cursor newCursor) {
        // Check if the new Cursor is different than the old one
        if (mCursor != newCursor) {
            // Set the member Cursor to the new Cursor
            mCursor = newCursor;

            // Reset the Array holding the RecipeAdapters being used
            mRecipeAdapterArray = new RecipeAdapter[mCursor.getCount()];

            // Notify the Adapter of the change in data
            notifyDataSetChanged();
        }
        return mCursor;
    }

    @Override
    public ChapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            // Inflate the layout to be used for the chapter list item
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_chapter, parent, false);
            return new ChapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection!");
        }
    }

    @Override
    public void onBindViewHolder(ChapterViewHolder holder, int position) {
        // Move the Cursor to the correct entry in the database
        mCursor.moveToPosition(position);

        // Retrieve the information to be used to populate the views within the view holder
        String chapterTitleText = mCursor.getString(ChapterEntry.IDX_CHAPTER_NAME);
        String chapterDescriptionText = mCursor.getString(ChapterEntry.IDX_CHAPTER_DESCRIPTION);
        final long chapterId = mCursor.getLong(ChapterEntry.IDX_CHAPTER_ID);
        final long recipeBookId = mCursor.getLong(ChapterEntry.IDX_BOOK_ID);

        // Attempt to retrieve the Cursor from the CursorManager
        Cursor cursor = mCursorManager.getCursor(position);

        if (cursor == null) {
            // Instantiate the Cursor by querying for the recipes of the chapter
            Uri recipesOfChapterUri = LinkRecipeBookTable.buildRecipeUriFromBookAndChapterId(
                    recipeBookId,
                    chapterId
            );

            // Initialize parameters for querying the database
            String[] projection = RecipeEntry.RECIPE_PROJECTION;
            String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
            String[] selectionArgs = new String[] {Long.toString(chapterId)};
            String sortOrder = LinkRecipeBookTable.COLUMN_RECIPE_ORDER + " ASC";

            // Query the database
            cursor = mContext.getContentResolver().query(
                    recipesOfChapterUri,
                    RecipeEntry.RECIPE_PROJECTION,
                    selection,
                    selectionArgs,
                    sortOrder
            );

            // Add the new Cursor to the member CursorManager
            mCursorManager.addCursor(position, cursor, recipesOfChapterUri, projection, sortOrder);
        }

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
                    mListener.onRecipeClicked(recipeId, viewHolder);
                }
            });

            // Set a reference to this RecipeAdapter in mRecipeAdapterArray
            mRecipeAdapterArray[position] = recipeAdapter;

            // Set whether the RecipeAdapter should use the DetailedView layout or master-flow/single
            recipeAdapter.setUseDetailView(useDetailView, mFragmentManager);
        }

        // Swap the Cursor in the recipeAdapter with the new one
        recipeAdapter.swapCursor(cursor);

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

    /**
     * Registers an observer to be notified of user interaction with the Adapter
     * @param listener
     */
    void setRecipeClickListener(RecipeClickListener listener) {
        mListener = listener;
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        // ItemId will be the recipeId since it is a UNIQUE primary key
        // Allows for smoother scrolling with StaggeredGridLayout and less shuffling
        mCursor.moveToPosition(position);
        return mCursor.getLong(LinkRecipeBookTable.IDX_CHAPTER_ID);
    }

    class ChapterViewHolder extends RecyclerView.ViewHolder {
        // Views bound by ButterKnife
        @BindView(R.id.list_chapter_title) TextView titleText;
        @BindView(R.id.list_chapter_description) TextView descriptionText;
        @BindView(R.id.list_chapter_recipe_recyclerview) NonScrollingRecyclerView recipeRecyclerView;
        @BindView(R.id.list_chapter_add_recipe) CardView addRecipeButton;

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
            if (mListener != null) mListener.onAddRecipeClicked(chapterId);
        }

        public ChapterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
