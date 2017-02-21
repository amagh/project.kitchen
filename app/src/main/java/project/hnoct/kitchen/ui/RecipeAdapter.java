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

    /**
     * Public constructor for RecipeAdapter. Used to inflate view used for Recycle View in the
     * {@link RecipeListFragment}
     * @param context interface for global context
     */
    public RecipeAdapter(Context context) {
        mContext = context;
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
            return new RecipeViewHolder(view);
        } else {
            throw new RuntimeException("Not bound to RecyclerViewSelection");
        }
    }

    @Override
    public void onBindViewHolder(RecipeViewHolder holder, int position) {
        // Return the data located at the correct row (position) of the view
        mCursor.moveToPosition(position);
        String recipeTitle = mCursor.getString(RecipeEntry.IDX_RECIPE_NAME);
        String recipeDescription = mCursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
        String recipeThumbnailUrl = mCursor.getString(RecipeEntry.IDX_THUMBNAIL_URL);
        Long recipeReviews = mCursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);
        Double recipeRating = mCursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);

        if (recipeThumbnailUrl != null) {
            Glide.with(mContext)
                    .load(recipeThumbnailUrl)
                    .into(holder.recipe_thumb);
        }

        holder.recipe_title.setText(recipeTitle);
        holder.recipe_description.setText(recipeDescription);
        holder.recipe_review_count.setText(Utilities.formatReviews(recipeReviews));
        holder.recipe_rating.setText(Utilities.formatRating(recipeRating));
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_title) TextView recipe_title;
        @BindView(R.id.list_description) TextView recipe_description;
        @BindView(R.id.list_thumbnail) ImageView recipe_thumb;
        @BindView(R.id.list_reviews_count) TextView recipe_review_count;
        @BindView(R.id.list_avg_rating) TextView recipe_rating;

        public RecipeViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
