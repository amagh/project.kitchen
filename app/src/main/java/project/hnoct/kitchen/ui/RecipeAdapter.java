package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 2/20/2017.
 */

public class RecipeAdapter extends android.support.v7.widget.RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    /** Constants **/
    public static final String LOG_TAG = RecipeAdapter.class.getSimpleName();

    /** Member Variables **/
    Context mContext;
    Cursor mCursor;
    RecipeAdapterOnClickHandler mClickHandler;

    /**
     * Public constructor for RecipeAdapter. Used to inflate view used for Recycle View in the
     * {@link RecipeListFragment}
     * @param context interface for global context
     * @param clickHandler interface for passing information to the UI from the Adapter
     */
    public RecipeAdapter(Context context, RecipeAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
    }

    public Cursor swapCursor(Cursor newCursor) {
        if (mCursor != newCursor) {
            mCursor = newCursor;
            notifyDataSetChanged();
        }
        return mCursor;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            // Inflate the list item layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
            view.setFocusable(true);

            // Set as the view in ViewHolder and return
            try {
                return new RecipeViewHolder(view);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    @Override
    public void onBindViewHolder(RecipeViewHolder holder, int position) {
        // Return the data located at the correct row (position of the view)
        mCursor.moveToPosition(position);
        String recipeTitle = mCursor.getString(RecipeEntry.IDX_RECIPE_NAME);
        String recipeDescription = mCursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
        String recipeThumbnailUrl = mCursor.getString(RecipeEntry.IDX_THUMBNAIL_URL);
        Long recipeReviews = mCursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);
        Double recipeRating = mCursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);

        if (recipeThumbnailUrl != null) {
            // Use Glide to load image into view
            Glide.with(mContext)
                    .load(recipeThumbnailUrl)
                    .into(holder.recipe_thumb);
        }

        // Populate the rest of the views
        holder.recipe_title.setText(recipeTitle);
        holder.recipe_description.setText(recipeDescription);
        holder.recipe_review_count.setText(Utilities.formatReviews(mContext, recipeReviews));
        holder.recipe_rating.setText(Utilities.formatRating(recipeRating));
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * Callback interface for passing information to the UI Activity
     */
    public interface RecipeAdapterOnClickHandler {
        void onClick(long recipeId, RecipeViewHolder viewHolder);
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        @BindView(R.id.list_title) TextView recipe_title;
        @BindView(R.id.list_description) TextView recipe_description;
        @BindView(R.id.list_thumbnail) ImageView recipe_thumb;
        @BindView(R.id.list_reviews_count) TextView recipe_review_count;
        @BindView(R.id.list_avg_rating) TextView recipe_rating;

        public RecipeViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            // Set the onClickListener to the ViewHolder
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Get the recipeId of the RecipeViewHolder just clicked
            mCursor.moveToPosition(getAdapterPosition());
            long recipeId = mCursor.getLong(RecipeEntry.IDX_RECIPE_ID);

            // Utilize the interface to pass information to the UI thread
            mClickHandler.onClick(recipeId, this);
        }
    }
}
