package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

import static android.view.View.GONE;

/**
 * Created by hnoct on 2/20/2017.
 */

public class RecipeAdapter extends android.support.v7.widget.RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    /** Constants **/
    public static final String LOG_TAG = RecipeAdapter.class.getSimpleName();
    static final int RECIPE_VIEW_NORMAL = 0;
    static final int RECIPE_VIEW_LAST = 1;

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
            int layoutId = -1;

            // Inflate the list item layout
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
            view.setFocusable(true);

            if (viewType == RECIPE_VIEW_LAST) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = (int) Utilities.convertDpToPixels(16);
                params.setMargins(margin, margin, margin, margin);
                view.setLayoutParams(params);
            }

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
        String recipeAuthor = mCursor.getString(RecipeEntry.IDX_RECIPE_AUTHOR);
        String recipeAttribution = mCursor.getString(RecipeEntry.IDX_RECIPE_SOURCE);
        String recipeDescription = mCursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
        String recipeThumbnailUrl = mCursor.getString(RecipeEntry.IDX_THUMBNAIL_URL);
        String recipeImgUrl = mCursor.getString(RecipeEntry.IDX_IMG_URL);
        Long recipeReviews = mCursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);
        Double recipeRating = mCursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);
        boolean favorite = mCursor.getInt(RecipeEntry.IDX_FAVORITE) == 1;

        if (recipeImgUrl != null) {
            // Use Glide to load image into view
            Glide.with(mContext)
                    .load(recipeImgUrl)
                    .into(holder.recipeImage);
        }

        if (recipeAttribution.equals(mContext.getString(R.string.attribution_custom)) ||
                mCursor.getLong(RecipeEntry.IDX_RECIPE_ID) < 0) {
            holder.recipeAttribution.setVisibility(View.INVISIBLE);
            holder.recipeReviews.setVisibility(View.INVISIBLE);
            holder.recipeRating.setVisibility(View.INVISIBLE);
        } else {
            holder.recipeAttribution.setVisibility(View.VISIBLE);
            holder.recipeReviews.setVisibility(View.VISIBLE);
            holder.recipeRating.setVisibility(View.VISIBLE);
        }

        // Populate the rest of the views
        holder.recipeTitle.setText(recipeTitle);
        holder.recipeAuthor.setText(Utilities.formatAuthor(mContext, recipeAuthor));
        holder.recipeAttribution.setText(recipeAttribution);
        holder.recipeDescription.setText(recipeDescription);
        holder.recipeReviews.setText(Utilities.formatReviews(mContext, recipeReviews));
        holder.recipeRating.setText(Utilities.formatRating(recipeRating));

        // Set the icon of the favorite button depending on its favorite status
        if (favorite) {
            holder.favoriteButton.setColorFilter(mContext.getResources().getColor(R.color.favorite_enabled));
        } else {
            holder.favoriteButton.setColorFilter(mContext.getResources().getColor(R.color.favorite_disabled));
        }
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mCursor.getCount() - 1) {
            return RECIPE_VIEW_LAST;
        }
        return RECIPE_VIEW_NORMAL;
    }

    /**
     * Callback interface for passing information to the UI Activity
     */
    public interface RecipeAdapterOnClickHandler {
        void onClick(long recipeId, RecipeViewHolder viewHolder);
    }

    public class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        @BindView(R.id.list_title) TextView recipeTitle;
        @BindView(R.id.list_recipe_author_text) TextView recipeAuthor;
        @BindView(R.id.list_recipe_attribution_text) TextView recipeAttribution;
        @BindView(R.id.list_recipe_description_text) TextView recipeDescription;
        @BindView(R.id.list_recipe_reviews_text) TextView recipeReviews;
        @BindView(R.id.list_recipe_rating_text) TextView recipeRating;
        @BindView(R.id.list_thumbnail) ImageView recipeImage;
        @BindView(R.id.list_recipe_favorite_button) ImageView favoriteButton;

        @OnClick(R.id.list_recipe_favorite_button)
        public void favorite(ImageView view) {
            if (mCursor.getCount() == 0) {
                return;
            }

            mCursor.moveToPosition(getAdapterPosition());
            long recipeId = mCursor.getLong(RecipeEntry.IDX_RECIPE_ID);
            boolean favorite = Utilities.setRecipeFavorite(mContext, recipeId);
            if (favorite) {
                view.setColorFilter(mContext.getResources().getColor(R.color.favorite_enabled));
            } else {
                view.setColorFilter(mContext.getResources().getColor(R.color.favorite_disabled));
            }
            notifyItemChanged(getAdapterPosition());
        }

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
