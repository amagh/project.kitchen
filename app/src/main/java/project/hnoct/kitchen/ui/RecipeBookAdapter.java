package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 3/25/2017.
 */

public class RecipeBookAdapter extends RecyclerView.Adapter<RecipeBookAdapter.RecipeBookViewHolder> {
    /** Constants **/

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private CursorManager mCursorManager;
    private RecipeBookAdapterOnClickHandler mClickHandler;

    RecipeBookAdapter(Context context, RecipeBookAdapterOnClickHandler clickHandler, CursorManager cursorManager) {
        // Initialize member variables
        mContext = context;
        mClickHandler = clickHandler;
        mCursorManager = cursorManager;

        // Register a listener to mCursorManager to listen for changes in its Cursors
        mCursorManager.setCursorChangeListener(new CursorManager.CursorChangeListener() {
            @Override
            public void onCursorChanged(int position) {
                // Notify the correct ViewHolder of a change in its data
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public RecipeBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for the recipe book list items
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_recipebook, parent, false);
        view.setFocusable(true);

        // Set the inflated view to the view holder
        return new RecipeBookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecipeBookViewHolder holder, int position) {
        // Move the cursor to the correct entry in the database
        mCursor.moveToPosition(position);

        // Retrieve the information from database to populate the view holder
        long bookId = mCursor.getLong(RecipeBookEntry.IDX_BOOK_ID);
        String bookTitle = mCursor.getString(RecipeBookEntry.IDX_BOOK_NAME);
        String bookDescription = mCursor.getString(RecipeBookEntry.IDX_BOOK_DESCRIPTION);

        // Populate the views of the view holder
        holder.recipeBookTitleText.setText(bookTitle);
        holder.recipeBookDescriptionText.setText(bookDescription);

        // Get the URI for the recipe book within the table so its chapters can be retrieved
        Uri recipeBookUri = LinkRecipeBookTable.buildRecipeUriFromRecipeBookId(bookId);

        // Retrieve the Cursor from mCursorManager
        Cursor cursor = mCursorManager.getCursor(position);

        if (cursor != null && cursor.moveToFirst()) {
            // Set the first recipe image of each chapter to the correct thumbnail in the recipe
            // book layout
            String image0Url = cursor.getString(LinkRecipeBookTable.IDX_IMG_URL);
            String image1Url, image2Url, image3Url;
            Glide.with(mContext)
                    .load(image0Url)
                    .into(holder.image0);
            if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookTable.IDX_RECIPE_ORDER) <= 0) {
                image1Url = cursor.getString(LinkRecipeBookTable.IDX_IMG_URL);
                Glide.with(mContext)
                        .load(image1Url)
                        .into(holder.image1);
                if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookTable.IDX_RECIPE_ORDER) <= 0) {
                    image2Url = cursor.getString(LinkRecipeBookTable.IDX_IMG_URL);
                    Glide.with(mContext)
                            .load(image2Url)
                            .into(holder.image2);
                    if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookTable.IDX_RECIPE_ORDER) <= 0) {
                        image3Url = cursor.getString(LinkRecipeBookTable.IDX_IMG_URL);
                        Glide.with(mContext)
                                .load(image3Url)
                                .into(holder.image3);
                    } else {
                        // Set any unused ImageViews to GONE
                        holder.image3.setVisibility(View.GONE);
                    }
                } else {
                    holder.image2.setVisibility(View.GONE);
                    holder.image3.setVisibility(View.GONE);
                }
            } else {
                holder.image1.setVisibility(View.GONE);
                holder.image2.setVisibility(View.GONE);
                holder.image3.setVisibility(View.GONE);
            }
        } else {
            holder.image0.setVisibility(View.GONE);
            holder.image1.setVisibility(View.GONE);
            holder.image2.setVisibility(View.GONE);
            holder.image3.setVisibility(View.GONE);
            holder.gradient.setVisibility(View.GONE);
        }
    }

    /**
     * Callback interface sending information about which recipe book as been selected so the
     * ChapterActivity can be correctly called
     */
    interface RecipeBookAdapterOnClickHandler {
        void onClick(RecipeBookViewHolder viewHolder, long bookId);
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    class RecipeBookViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.list_recipebook_image_0) ImageView image0;
        @BindView(R.id.list_recipebook_image_1) ImageView image1;
        @BindView(R.id.list_recipebook_image_2) ImageView image2;
        @BindView(R.id.list_recipebook_image_3) ImageView image3;
        @BindView(R.id.list_recipebook_gradient) ImageView gradient;
        @BindView(R.id.list_recipebook_title) TextView recipeBookTitleText;
        @BindView(R.id.list_recipebook_description_text) TextView recipeBookDescriptionText;

        public RecipeBookViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Pass the recipe book ID of the clicked item to the click handler
            int position = getAdapterPosition();
            mCursor.moveToPosition(position);
            long bookId = mCursor.getLong(RecipeBookEntry.IDX_BOOK_ID);
            mClickHandler.onClick(this, bookId);
        }
    }
}
