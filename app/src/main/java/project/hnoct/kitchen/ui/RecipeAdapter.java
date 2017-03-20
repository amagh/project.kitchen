package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Optional;
import project.hnoct.kitchen.R;

import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

import static android.view.View.GONE;

/**
 * Created by hnoct on 2/20/2017.
 */

class RecipeAdapter extends android.support.v7.widget.RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {
    /** Constants **/
    public static final String LOG_TAG = RecipeAdapter.class.getSimpleName();
    private static final int RECIPE_VIEW_NORMAL = 0;
    private static final int RECIPE_VIEW_LAST = 1;
    private static final int RECIPE_VIEW_DETAIL = 2;
    private static final int RECIPE_VIEW_DETAIL_LAST = 3;

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private RecipeAdapterOnClickHandler mClickHandler;
    private int mDetailCardPosition;

    private FragmentManager mFragmentManager;       // Used for inflating the RecipeDetailsFragment into the ViewHolder
    private RecyclerView mRecyclerView;

    private boolean useDetailView = false;


    /**
     * Public constructor for RecipeAdapter. Used to inflate view used for Recycle View in the
     * {@link RecipeListFragment}
     * @param context interface for global context
     * @param clickHandler interface for passing information to the UI from the Adapter
     */
    RecipeAdapter(Context context, FragmentManager fragmentManager, RecipeAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
        mFragmentManager = fragmentManager;
        mDetailCardPosition = -1;
    }

    Cursor swapCursor(Cursor newCursor) {
        if (mCursor != newCursor) {
            mCursor = newCursor;
            notifyDataSetChanged();
        }
        return mCursor;
    }

    void setUseDetailView(boolean useDetailView) {
        this.useDetailView = useDetailView;
    }

    @Override
    public RecipeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            if (mRecyclerView == null) {
                mRecyclerView = (RecyclerView) parent;
                hideDetails();
            }

            // Set up the view to be added to the ViewHolder
            View view;

            // Inflate the list item layout
            switch (viewType) {
                case RECIPE_VIEW_NORMAL: {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
                    view.setFocusable(true);
                    break;
                }

                case RECIPE_VIEW_LAST: {
                    // Same as normal, but with an added margin at the bottom
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe, parent, false);
                    view.setFocusable(true);
                    StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
                    int margin = (int) Utilities.convertDpToPixels(16);
                    params.setMargins(margin, margin, margin, margin);
                    view.setLayoutParams(params);
                    break;
                }

                case RECIPE_VIEW_DETAIL: {
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe_details, parent, false);
                    view.setFocusable(true);
                    break;
                }

                case RECIPE_VIEW_DETAIL_LAST: {
                    // Same as normal, but with an added margin at the bottom
                    view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_recipe_details, parent, false);
                    view.setFocusable(true);
                    StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams();
                    int margin = (int) Utilities.convertDpToPixels(16);
                    params.setMargins(margin, margin, margin, margin);
                    view.setLayoutParams(params);
                    break;
                }

                default: throw new UnsupportedOperationException("Unknown ViewType");
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

    /**
     * Hides the detailed layout if user scrolls it out of position
     */
    void hideDetails() {
        // Get the layout manager
        final StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager) mRecyclerView.getLayoutManager();

        // Add an OnScrollListener
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Get the position of the first and last visible views
                int[] firstVisible = sglm.findFirstVisibleItemPositions(null);
                int[] lastVisible = sglm.findLastVisibleItemPositions(null);

                if (mDetailCardPosition != -1 &&
                        (firstVisible[0] != -1 &&
                                lastVisible[lastVisible.length - 1] != -1) &&
                        (mDetailCardPosition < firstVisible[0] ||
                                mDetailCardPosition > lastVisible[lastVisible.length - 1])) {
                    // If detailed layout is scrolled out of view, then set no recipes to use the
                    // detailed layout
                    mDetailCardPosition = -1;
                }
            }
        });
    }

    @Override
    public void onBindViewHolder(final RecipeViewHolder holder, int position) {
        // Return the data located at the correct row (position of the view)
        mCursor.moveToPosition(position);
        String recipeTitle = mCursor.getString(RecipeEntry.IDX_RECIPE_NAME);
        String recipeAuthor = mCursor.getString(RecipeEntry.IDX_RECIPE_AUTHOR);
        String recipeAttribution = mCursor.getString(RecipeEntry.IDX_RECIPE_SOURCE);
        String recipeDescription = mCursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
        String recipeImgUrl = mCursor.getString(RecipeEntry.IDX_IMG_URL);
        Long recipeReviews = mCursor.getLong(RecipeEntry.IDX_RECIPE_REVIEWS);
        Double recipeRating = mCursor.getDouble(RecipeEntry.IDX_RECIPE_RATING);
        boolean favorite = mCursor.getInt(RecipeEntry.IDX_FAVORITE) == 1;

        String recipeUrl = mCursor.getString(RecipeEntry.IDX_RECIPE_URL);

        if (useDetailView && position == mDetailCardPosition) {
            // Instantiate a new RecipeDetailsFragmeent
            RecipeDetailsFragment fragment = new RecipeDetailsFragment();
            fragment.setCursorLoaderListener(new RecipeDetailsFragment.CursorLoaderListener() {
                @Override
                public void onCursorLoaded(Cursor cursor, int recipeServings) {
                    // When finished loading the fragment, scroll to the recipe
                    StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager)mRecyclerView.getLayoutManager();
                    sglm.scrollToPositionWithOffset(mDetailCardPosition, 0);
                }
            });

            // Add the recipe's URL as a Bundle to the fragment
            Bundle args = new Bundle();
            args.putParcelable(RecipeDetailsFragment.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            fragment.setArguments(args);

            // Swap the fragment into the container
            mFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();


            // Set the layout to span the entire width
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
            holder.itemView.setLayoutParams(layoutParams);

        } else {
            // Reset the layout parameters if it has been set differently for the detailed layout
            StaggeredGridLayoutManager.LayoutParams layoutParams =
                    (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
            holder.itemView.setLayoutParams(layoutParams);

            if (recipeImgUrl != null) {
                // Use Glide to load image into view
                Glide.with(mContext)
                        .load(recipeImgUrl)
                        .into(holder.recipeImage);
            }

            if (recipeAttribution.equals(mContext.getString(R.string.attribution_custom)) ||
                    mCursor.getLong(RecipeEntry.IDX_RECIPE_ID) < 0) {
                // Hide non-utilized views if recipe was made or edited by user
                holder.recipeAttribution.setVisibility(View.INVISIBLE);
                holder.recipeReviews.setVisibility(View.INVISIBLE);
                holder.recipeRating.setVisibility(View.INVISIBLE);
            } else {
                // Show the views if recipe was not custom made
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
        if (useDetailView && position == mDetailCardPosition && position == mCursor.getCount() - 1) {
            return RECIPE_VIEW_DETAIL_LAST;
        } else if (useDetailView && position == mDetailCardPosition) {
            return RECIPE_VIEW_DETAIL;
        } else if (position == mCursor.getCount() - 1) {
             return RECIPE_VIEW_LAST;
        } else {
            return RECIPE_VIEW_NORMAL;
        }
    }

    @Override
    public long getItemId(int position) {
        // ItemId will be the recipeId since it is a UNIQUE primary key
        // Allows for smoother scrolling with StaggeredGridLayout and less shuffling
        mCursor.moveToPosition(position);
        return mCursor.getLong(RecipeEntry.IDX_RECIPE_ID);
    }

    /**
     * Callback interface for passing information to the UI Activity
     */
    interface RecipeAdapterOnClickHandler {
        void onClick(long recipeId, RecipeViewHolder viewHolder);
    }

    class RecipeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        @Nullable @BindView(R.id.list_title) TextView recipeTitle;
        @Nullable @BindView(R.id.list_recipe_author_text) TextView recipeAuthor;
        @Nullable @BindView(R.id.list_recipe_attribution_text) TextView recipeAttribution;
        @Nullable @BindView(R.id.list_recipe_description_text) TextView recipeDescription;
        @Nullable @BindView(R.id.list_recipe_reviews_text) TextView recipeReviews;
        @Nullable @BindView(R.id.list_recipe_rating_text) TextView recipeRating;
        @Nullable @BindView(R.id.list_thumbnail) ImageView recipeImage;
        @Nullable @BindView(R.id.list_recipe_favorite_button) ImageView favoriteButton;
        @Nullable @BindView(R.id.fragment_container) FrameLayout container;
        @Nullable @BindView(R.id.list_recipe_image_container) RelativeLayout imageContainer;
        @Nullable @BindView(R.id.list_recipe_text_container) android.support.v7.widget.GridLayout textContainer;

        @Optional
        @OnClick(R.id.list_recipe_favorite_button)
        void favorite(ImageView view) {
            if (mCursor.getCount() == 0) {
                // If cursor hasn't loaded anything yet, then do nothing
                return;
            }

            // Toggle the favorite status of the recipe
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

        RecipeViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            // Set the onClickListener to the ViewHolder
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Get the recipeId of the RecipeViewHolder just clicked
            int position = getAdapterPosition();
            mCursor.moveToPosition(position);
            long recipeId = mCursor.getLong(RecipeEntry.IDX_RECIPE_ID);

            // Reload the view that used to be using the detailed layout
            if (mDetailCardPosition != -1) notifyItemChanged(mDetailCardPosition);

            // Set the clicked recipe to utilize detailed layout
            mDetailCardPosition = position;

            // Reload the view
            notifyItemChanged(mDetailCardPosition);

            // Scroll to the layout just clicked
            StaggeredGridLayoutManager sglm = (StaggeredGridLayoutManager)mRecyclerView.getLayoutManager();
            sglm.scrollToPositionWithOffset(mDetailCardPosition, 0);

            // Utilize the interface to pass information to the UI thread if detailed view is not
            // being used
            if (!useDetailView) mClickHandler.onClick(recipeId, this);
        }
    }
}
