package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 3/22/2017.
 */

public class ChapterAdapter extends RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder> {
    private Context mContext;
    private Cursor mCursor;
    private CursorManager mCursorManager;
    private FragmentManager mFragmentManager;

    public ChapterAdapter(Context context, FragmentManager fragmentManager, CursorManager cursorManager) {
        mContext = context;
        // Get the CursorManager passed from the activity so all the Cursors being managed can be
        // properly closed in onStop of the Activity
        mCursorManager = cursorManager;
    }

    Cursor swapCursor(Cursor newCursor) {
        if (mCursor != newCursor) {
            mCursor = newCursor;
            notifyDataSetChanged();
        }
        return mCursor;
    }

    @Override
    public ChapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_chapter, parent, false);
            return new ChapterViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection!");
        }
    }

    @Override
    public void onBindViewHolder(ChapterViewHolder holder, int position) {
        mCursor.moveToPosition(position);
        String chapterTitleText = mCursor.getString(LinkRecipeBookTable.IDX_CHAPTER_NAME);
        String chapterDescriptionText = mCursor.getString(LinkRecipeBookTable.IDX_CHAPTER_DESCRIPTION);
        long chapterId = mCursor.getLong(LinkRecipeBookTable.IDX_CHAPTER_ID);
        long recipeBookId = mCursor.getLong(LinkRecipeBookTable.IDX_BOOK_ID);

        Cursor cursor = mCursorManager.getCursor(position);

        if (cursor == null) {
            Uri recipesOfChapterUri = LinkRecipeBookTable.buildRecipeUriFromBookAndChapterId(
                    recipeBookId,
                    chapterId
            );
            String[] projection;
            String sortOrder = LinkRecipeBookTable.COLUMN_RECIPE_ORDER + " ASC";

            cursor = mContext.getContentResolver().query(
                    recipesOfChapterUri,
                    RecipeEntry.RECIPE_PROJECTION, // <-- TODO: Make sure this works out correctly
                    null,
                    null,
                    sortOrder
            );
        }

        holder.titleText.setText(chapterTitleText);
        holder.descriptionText.setText(chapterDescriptionText);

        int columnCount;

        if (RecipeListActivity.mTwoPane && RecipeListActivity.mDetailsVisible) {
            columnCount = mContext.getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columnCount = mContext.getResources().getInteger(R.integer.recipe_columns);
        }

        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        final boolean useDetailView = mContext.getResources().getBoolean(R.bool.recipeAdapterUseDetailView);

        holder.recipeRecyclerView.setLayoutManager(sglm);
        RecipeAdapter recipeAdapter = new RecipeAdapter(mContext, mFragmentManager, new RecipeAdapter.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, RecipeAdapter.RecipeViewHolder viewHolder) {
                if (useDetailView);
            }
        });

        recipeAdapter.setUseDetailView(useDetailView);
        recipeAdapter.swapCursor(cursor);
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
        @BindView(R.id.list_chapter_title) TextView titleText;
        @BindView(R.id.list_chapter_description) TextView descriptionText;
        @BindView(R.id.list_chapter_recipe_recyclerview) NonScrollingRecyclerView recipeRecyclerView;

        public ChapterViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
