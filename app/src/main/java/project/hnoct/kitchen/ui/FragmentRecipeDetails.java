package project.hnoct.kitchen.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.dialog.AddToRecipeBookDialog;
import project.hnoct.kitchen.sync.RecipeImporter;
import project.hnoct.kitchen.ui.adapter.AdapterDirection;
import project.hnoct.kitchen.ui.adapter.AdapterIngredient;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentRecipeDetails extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FragmentRecipeDetails.class.getSimpleName();
    private static final int DETAILS_LOADER = 1;
    private static final String RECIPE_DETAILS_URI = "recipe_and_ingredients_uri";
    public static final String RECIPE_DETAILS_URL = "recipe_url";

    /** Member Variables **/
    private Uri mRecipeUri;
    private String mRecipeUrl;
    private long mRecipeId;
    private Context mContext;
    private Cursor mCursor;
    private ContentResolver mContentResolver;
    private AdapterIngredient mIngredientAdapter;
    private AdapterDirection mDirectionAdapter;
    private CursorLoaderListener listener;
    private long time;

    private boolean mSyncing = false;
    private boolean loaded = false;

    // Views bound by ButterKnife
    @BindView(R.id.details_ingredient_recycler_view) RecyclerView mIngredientsRecyclerView;
    @BindView(R.id.details_direction_recycler_view) RecyclerView mDirectionsRecyclerView;
    @BindView(R.id.details_recipe_image) ImageView mRecipeImageView;
    @BindView(R.id.details_recipe_title_text) TextView mRecipeTitleText;
    @BindView(R.id.details_recipe_author_text) TextView mRecipeAuthorText;
    @BindView(R.id.details_recipe_attribution_text) TextView mRecipeAttributionText;
    @BindView(R.id.details_recipe_reviews_text) TextView mRecipeReviewsText;
    @BindView(R.id.details_ratings_text) TextView mRecipeRatingText;
    @BindView(R.id.details_recipe_short_description_text) TextView mRecipeShortDescriptionText;
    @BindView(R.id.details_ingredient_title_text) TextView mIngredientTitleText;
    @BindView(R.id.details_direction_title_text) TextView mDirectionTitleText;
    @BindView(R.id.details_line_separator_top) View mLineSeparatorTop;
    @BindView(R.id.details_line_separator_bottom) View mLineSeparatorBottom;


    public FragmentRecipeDetails() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        ButterKnife.bind(this, rootView);
        setHasOptionsMenu(true);
        time = Utilities.getCurrentTime();

        // Initialize member variables
        mContext = getActivity();
        mContentResolver = mContext.getContentResolver();

        if (getArguments() != null) {
            // Get the URL passed from the ActivityRecipeList/ActivityRecipeDetails
            mRecipeUri = getArguments().getParcelable(RECIPE_DETAILS_URI);
            if(getArguments().getParcelable(RECIPE_DETAILS_URL) != null) {
                mRecipeUrl = getArguments().getParcelable(RECIPE_DETAILS_URL).toString();

                // Get the recipeId and generate recipeUri for database
                mRecipeId = Utilities.getRecipeIdFromUrl(mContext, mRecipeUrl);

                if (mRecipeId == -1) {
                    mRecipeId = Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);
                }

                mRecipeUri = LinkIngredientEntry.buildIngredientUriFromRecipe(mRecipeId);
            }
        } else {
            Log.d(LOG_TAG, "No bundle found!");
            return rootView;
        }

        // Initialize the AdapterIngredient and AdapterDirection
        mIngredientAdapter = new AdapterIngredient(getActivity());
        mDirectionAdapter = new AdapterDirection(getActivity());


        // Initialize and set the LayoutManagers
        LinearLayoutManager llm = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        LinearLayoutManager llm2 = new LinearLayoutManager(getActivity()) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        mIngredientsRecyclerView.setLayoutManager(llm);
        mDirectionsRecyclerView.setLayoutManager(llm2);


        // Set the AdapterIngredient for the ingredient's RecyclerView
        mIngredientsRecyclerView.setAdapter(mIngredientAdapter);
        mDirectionsRecyclerView.setAdapter(mDirectionAdapter);

        loadImageView();

        return rootView;
    }

    private void loadImageView() {
        Cursor cursor = mContentResolver.query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(mRecipeId)},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String recipeImageUrl = cursor.getString(RecipeEntry.IDX_IMG_URL);
            if (!recipeImageUrl.isEmpty()) {
                loaded = true;
            }

            Glide.with(mContext)
                    .load(recipeImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                            // When image has finished loading, load image into target
                            target.onResourceReady(resource, null);

                            if (getActivity() instanceof ActivityRecipeDetails && loaded) {
                                scheduleStartPostponedTransition(mRecipeImageView);
                            }

                            Log.d(LOG_TAG, "Time elapsed: " + (Utilities.getCurrentTime() - time   + "ms"));
                            return false;

                            }
                    })
                    .into(mRecipeImageView);

            cursor.close();
        }


    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Sort the columns by order that ingredient was added to link table
        String sortOrder = LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " ASC";

        if (mRecipeUri == null) {
            return null;
        }

        // Initialize and return CursorLoader
        return new CursorLoader(
                mContext,
                mRecipeUri,
                LinkIngredientEntry.LINK_PROJECTION,
                null,
                null,
                sortOrder
        );
    }

    public void setCursorLoaderListener(CursorLoaderListener listener) {
        this.listener = listener;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            // Ensure a Cursor was returned
            mCursor = cursor;
        } else {
            return;
        }

        // Move Cursor to first row or end
        if (!mCursor.moveToFirst()) {
            if (!mSyncing) {
                mSyncing = true;
                setInvisible();

                // If recipe is missing information, then load details from web
                RecipeImporter.importRecipeFromUrl(mContext, new RecipeImporter.UtilitySyncer() {
                    @Override
                    public void onFinishLoad() {
                        if (getActivity() != null)
                        getLoaderManager().restartLoader(DETAILS_LOADER, null, FragmentRecipeDetails.this);
                        mSyncing = false;
                    }
                }, mRecipeUrl);
            }
            return;
        }

        // Retrieve recipe information from database
        long recipeId = mCursor.getLong(LinkIngredientEntry.IDX_RECIPE_ID);
        String recipeTitle = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_NAME);
        String recipeAuthor = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_AUTHOR);
        String recipeImageUrl = mCursor.getString(LinkIngredientEntry.IDX_IMG_URL);
        String recipeUrl = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_URL);
        String recipeDescription = mCursor.getString(LinkIngredientEntry.IDX_SHORT_DESC);
        double recipeRating = mCursor.getDouble(LinkIngredientEntry.IDX_RECIPE_RATING);
        long recipeReviews = mCursor.getLong(LinkIngredientEntry.IDX_RECIPE_REVIEWS);
        String recipeDirections = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_DIRECTIONS);
        boolean recipeFavorite = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_FAVORITE) == 1;
        String recipeSource = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_SOURCE);
        int recipeServings = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_SERVINGS);

        // Populate the views with the data
        if (!loaded) {
            loadImageView();
        }
//        Glide.with(mContext)
//                .load(recipeImageUrl)
////                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
//                .listener(new RequestListener<String, GlideDrawable>() {
//                    @Override
//                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
//                        return false;
//                    }
//
//                    @Override
//                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
//                        // When image has finished loading, load image into target
////                        target.onResourceReady(resource, null);
//
//                        if (getActivity() instanceof ActivityRecipeDetails) {
//                            scheduleStartPostponedTransition(mRecipeImageView);
//                        }
//                        Log.d(LOG_TAG, "Time elapsed: " + (Utilities.getCurrentTime() - time   + "ms"));
//                        return false;
//                    }
//                })
//                .into(mRecipeImageView);

        mRecipeTitleText.setText(recipeTitle);
        mRecipeAuthorText.setText(Utilities.formatAuthor(mContext, recipeAuthor));
        mRecipeAttributionText.setText(recipeSource);

        if (recipeId < 0) {
            // Recipes with a negative recipe ID are user-created or user-modified recipes and
            // therefore have no ratings or reviews
            mRecipeRatingText.setVisibility(View.GONE);
            mRecipeReviewsText.setVisibility(View.GONE);
        } else {
            mRecipeRatingText.setText(Utilities.formatRating(recipeRating));
            mRecipeReviewsText.setText(Utilities.formatReviews(mContext, recipeReviews));
        }

        mRecipeShortDescriptionText.setText(recipeDescription);

        // Set the visibility of the ingredient and direction section titles to VISIBLE

//        mIngredientTitleText.setVisibility(View.VISIBLE);
//        mDirectionTitleText.setVisibility(View.VISIBLE);
//        mIngredientTitleText.startAnimation(fadeInAnim);
//        mDirectionTitleText.startAnimation(fadeInAnim);
//        mIngredientsRecyclerView.startAnimation(fadeInAnim);
//        mDirectionsRecyclerView.startAnimation(fadeInAnim);

        // Set visibility of line separators to VISIBLE
        mLineSeparatorTop.setVisibility(View.VISIBLE);
        mLineSeparatorBottom.setVisibility(View.VISIBLE);

        // Swap the Cursor into the Adapter so that data can be displayed in the ingredient list
        mIngredientAdapter.swapCursor(cursor);

        // Set the direction list for the AdapterDirection so steps can be displayed
        if (recipeDirections != null) {
            mDirectionAdapter.setDirectionList(Utilities.getDirectionList(recipeDirections));
        }

        if (mRecipeTitleText.getVisibility() == View.INVISIBLE) fadeIn();

        if (listener != null) {
            listener.onCursorLoaded(cursor, recipeServings);
        }
    }

    void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        getActivity().supportStartPostponedEnterTransition();
                        return true;
                    }
                });
    }

    void fadeIn() {
        Animation fadeInAnim = AnimationUtils.loadAnimation(mContext, R.anim.fade);

        mIngredientTitleText.setVisibility(View.VISIBLE);
        mDirectionTitleText.setVisibility(View.VISIBLE);
        mIngredientsRecyclerView.setVisibility(View.VISIBLE);
        mDirectionsRecyclerView.setVisibility(View.VISIBLE);
//        mRecipeImageView.setVisibility(View.VISIBLE);
        mRecipeTitleText.setVisibility(View.VISIBLE);
        mRecipeAuthorText.setVisibility(View.VISIBLE);
        mRecipeAttributionText.setVisibility(View.VISIBLE);
        mRecipeReviewsText.setVisibility(View.VISIBLE);
        mRecipeRatingText.setVisibility(View.VISIBLE);
        mRecipeShortDescriptionText.setVisibility(View.VISIBLE);

        mLineSeparatorTop.setVisibility(View.VISIBLE);
        mLineSeparatorBottom.setVisibility(View.VISIBLE);

        mIngredientTitleText.startAnimation(fadeInAnim);
        mDirectionTitleText.startAnimation(fadeInAnim);
        mIngredientsRecyclerView.startAnimation(fadeInAnim);
        mDirectionsRecyclerView.startAnimation(fadeInAnim);
//        mRecipeImageView.startAnimation(fadeInAnim);
        mRecipeTitleText.startAnimation(fadeInAnim);
        mRecipeAuthorText.startAnimation(fadeInAnim);
        mRecipeAttributionText.startAnimation(fadeInAnim);
        mRecipeReviewsText.startAnimation(fadeInAnim);
        mRecipeRatingText.startAnimation(fadeInAnim);
        mRecipeShortDescriptionText.startAnimation(fadeInAnim);

        mLineSeparatorTop.startAnimation(fadeInAnim);
        mLineSeparatorBottom.startAnimation(fadeInAnim);
    }

    void setInvisible() {
        mIngredientTitleText.setVisibility(View.INVISIBLE);
        mDirectionTitleText.setVisibility(View.INVISIBLE);
        mIngredientsRecyclerView.setVisibility(View.INVISIBLE);
        mDirectionsRecyclerView.setVisibility(View.INVISIBLE);
//        mRecipeImageView.setVisibility(View.INVISIBLE);
        mRecipeTitleText.setVisibility(View.INVISIBLE);
        mRecipeAuthorText.setVisibility(View.INVISIBLE);
        mRecipeAttributionText.setVisibility(View.INVISIBLE);
        mRecipeReviewsText.setVisibility(View.INVISIBLE);
        mRecipeRatingText.setVisibility(View.INVISIBLE);
        mRecipeShortDescriptionText.setVisibility(View.INVISIBLE);

        mLineSeparatorTop.setVisibility(View.INVISIBLE);
        mLineSeparatorBottom.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mIngredientAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long recipeId = Utilities.getRecipeIdFromUrl(getContext(), getActivity().getIntent().getData().toString());
        Uri linkUri = LinkIngredientEntry.buildIngredientUriFromRecipe(recipeId);

        if (getActivity() instanceof ActivityRecipeDetails) {
            Cursor cursor = getContext().getContentResolver().query(
                    linkUri,
                    LinkIngredientEntry.LINK_PROJECTION,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst() && !cursor.getString(LinkIngredientEntry.IDX_IMG_URL).isEmpty()) {
                // Delay transition animation
                getActivity().supportPostponeEnterTransition();
                cursor.close();
            }
        }


        // Initialize CursorLoader
        getLoaderManager().initLoader(DETAILS_LOADER, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_details_fragment, menu);

        // Query the db to check whether the recipe is a favorite
        Cursor cursor = mContentResolver.query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(mRecipeId)},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // Instantiate the menu-item associated with favorites
            MenuItem menuFavorite = menu.findItem(R.id.detail_favorites);

            // Set the icon for the favorites action depending on the favorite status of the recipe
            menuFavorite.setIcon(cursor.getInt(RecipeEntry.IDX_FAVORITE) == 1 ?
                    R.drawable.btn_rating_star_on_normal_holo_light : R.drawable.btn_rating_star_off_normal_holo_light);
        }

        // Close the Cursor
        if (cursor != null) cursor.close();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.detail_favorites: {
                // Update the favorite status of the recipe when selected and change the icon accordingly
                boolean favorite = Utilities.setRecipeFavorite(mContext, mRecipeId);
                if (favorite) {
                    item.setIcon(R.drawable.btn_rating_star_on_normal_holo_light);
                } else {
                    item.setIcon(R.drawable.btn_rating_star_off_normal_holo_light);
                }
                return true;
            }

            case R.id.detail_menu_edit: {
                // Start ActivityCreateRecipe to edit recipe
                Intent intent = new Intent(mContext, ActivityCreateRecipe.class);
                intent.setData(RecipeEntry.buildRecipeUriFromId(mRecipeId));
                startActivity(intent);
                return true;
            }

            case R.id.detail_menu_add_to_recipebook: {
                AddToRecipeBookDialog dialog = new AddToRecipeBookDialog();
                dialog.show(getActivity().getFragmentManager(), "dialog");
                dialog.setChapterSelectedListener(new AddToRecipeBookDialog.ChapterSelectedListener() {
                    @Override
                    public void onChapterSelected(long bookId, long chapterId) {
                        // Initialize parameters for querying the database for recipe order
                        Uri linkRecipeBookUri = LinkRecipeBookEntry.CONTENT_URI;
                        String[] projection = LinkRecipeBookEntry.PROJECTION;
                        String selection = ChapterEntry.TABLE_NAME + "." + ChapterEntry.COLUMN_CHAPTER_ID + " = ?";
                        String[] selectionArgs = new String[] {Long.toString(chapterId)};
                        String sortOrder = LinkRecipeBookEntry.COLUMN_RECIPE_ORDER + " DESC";

                        // Query the database to determine the new recipe's order in the chapter
                        Cursor cursor = mContentResolver.query(
                                linkRecipeBookUri,
                                projection,
                                selection,
                                selectionArgs,
                                sortOrder
                        );

                        int recipeOrder;
                        if (cursor !=  null && cursor.moveToFirst()) {
                            recipeOrder = cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER) + 1;
                            // Close the Cursor
                            cursor.close();
                        } else {
                            recipeOrder = 0;
                        }

                        // Generate the ContentValues to insert the entry in the database
                        ContentValues values = new ContentValues();
                        values.put(RecipeEntry.COLUMN_RECIPE_ID, mRecipeId);
                        values.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_ID, bookId);
                        values.put(ChapterEntry.COLUMN_CHAPTER_ID, chapterId);
                        values.put(LinkRecipeBookEntry.COLUMN_RECIPE_ORDER, recipeOrder);

                        // Insert into database
                        mContentResolver.insert(
                                LinkRecipeBookEntry.CONTENT_URI,
                                values
                        );
                    }
                });
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public interface CursorLoaderListener {
        void onCursorLoaded(Cursor cursor, int recipeServings);
    }
}
