package project.kitchen.ui;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;
import project.kitchen.dialog.AddToRecipeBookDialog;
import project.kitchen.dialog.ShoppingListDialog;
import project.kitchen.sync.RecipeImporter;
import project.kitchen.ui.adapter.AdapterDirection;
import project.kitchen.ui.adapter.AdapterIngredient;

import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_IMAGE_URL;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_URL;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_GENERIC;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentRecipeDetails extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private static final String LOG_TAG = FragmentRecipeDetails.class.getSimpleName();
    private static final int DETAILS_LOADER = 1;
    private static final String RECIPE_DETAILS_URI = "recipe_and_ingredients_uri";
//    public static final String RECIPE_DETAILS_URL = "recipe_url";
//    public static final String RECIPE_DETAILS_IMAGE_URL = "image_url";
    private static final String DIALOG_SHOPPING_LIST = "shopping_list_dialog";

    /** Member Variables **/
    private Uri mRecipeUri;
    private String mRecipeUrl;
    private String mImageUrl;
    private long mRecipeId;
    private Context mContext;
    private Cursor mCursor;
    private ContentResolver mContentResolver;
    private AdapterIngredient mIngredientAdapter;
    private AdapterDirection mDirectionAdapter;
    private CursorLoaderListener listener;
    private ConnectivityListener mConnectivityListener;
    private Snackbar mSnackbar;

    private boolean mSyncing = false;
    private boolean isConnected = true;
    private boolean connectivityRegistered = false;
    private boolean genericRecipe = false;

    // Views bound by ButterKnife
    @BindView(R.id.details_ingredient_recycler_view) RecyclerView mIngredientsRecyclerView;
    @BindView(R.id.details_direction_recycler_view) RecyclerView mDirectionsRecyclerView;
    @Nullable @BindView(R.id.details_recipe_image) ImageView mRecipeImageView;
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
    @BindView(R.id.details_recipe_progressbar) ProgressBar mProgressBar;
    @BindView(R.id.details_shopping_list_button) ImageView mShoppingListButton;

    public FragmentRecipeDetails() {
    }

    @OnClick(R.id.details_shopping_list_button)
    void onClick(View view) {
        // Show a dialog that allows the user to add ingredients to a shopping list
        ShoppingListDialog dialog = new ShoppingListDialog();

        // Pass the Cursor with ingredient information
        dialog.setIngredientCursor(mCursor);
        dialog.show(getActivity().getFragmentManager(), DIALOG_SHOPPING_LIST);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_recipe_details, container, false);
        ButterKnife.bind(this, rootView);
        setHasOptionsMenu(true);

        // Retrieve Bundled arguments
        Bundle args = getArguments();
        if (args != null) {
            // In master-detail flow, the imageUrl is passed as part of the Bundle
            mImageUrl = args.getString(RECIPE_DETAILS_IMAGE_URL);

            // Generic is always passed from either ActivityRecipeDetails or as part of the Bundle
            // in master-detail flow
            genericRecipe = args.getBoolean(RECIPE_DETAILS_GENERIC);
        }

        // Initialize member variables
        mContext = getActivity();
        mContentResolver = mContext.getContentResolver();

        // Retrieve member variable values from Bundle
        if (!initRecipeVars()) {
            Log.d(LOG_TAG, "No bundle found!");
        }

        // Set up RecyclerViews
        initRecyclerViews();

        // mImageUrl is only passed when in two-pane mode. If the variable is not null, then the
        // ImageView is located in the Fragment and needs to be populated
        if (mImageUrl != null) {
            loadImageView();
        }

        return rootView;
    }

    /**
     * Initializes member variables by retrieving their values from arguments and the database
     * @return true if Bundle was passed from calling Activity, false if no Bundle was passed
     */
    private boolean initRecipeVars() {
        // Retrieve the Bundle passed to the Fragment
        Bundle args = getArguments();

        if (args == null) {
            // If there is nothing passed to the Activity to load, then there is nothing to do and
            // the Fragment will be stuck
            if (getActivity() instanceof ActivityRecipeDetails) {
                getActivity().finish();
            }

            return false;
        } else {
            // Retrieve the recipe's info from the Bundle
            mRecipeUrl = args.getParcelable(RECIPE_DETAILS_URL).toString();
            mImageUrl = args.getString(RECIPE_DETAILS_IMAGE_URL);
            genericRecipe = args.getBoolean(RECIPE_DETAILS_GENERIC);

            // Retrieve the recipe's corresponding ID from database. This prevents duplicate recipes
            // from being added
            mRecipeId = Utilities.getRecipeIdFromUrl(mContext, mRecipeUrl);

            if (mRecipeId == -1) {
                // If the URL passed doesn't have a corresponding ID, generate the ID that will be
                // used by the recipe once it has been imported
                mRecipeId = Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);
            }

            // Generate the recipe's URI for its database entry
            mRecipeUri = LinkIngredientEntry.buildIngredientUriFromRecipe(mRecipeId);

            return true;
        }
    }

    /**
     * Sets up the RecyclerViews by initialized and setting proper Adapters and LayoutManagers
     */
    private void initRecyclerViews() {
        // Initialize the AdapterIngredient and AdapterDirection
        mIngredientAdapter = new AdapterIngredient(getActivity());
        mDirectionAdapter = new AdapterDirection(getActivity());

        // Initialize and set the LayoutManagers
        LinearLayoutManager llm = new LinearLayoutManager(getActivity()) {
//            @Override
//            public boolean canScrollVertically() {
//                return false;
//            }
//
//            @Override
//            public boolean canScrollHorizontally() {
//                return false;
//            }
        };
        LinearLayoutManager llm2 = new LinearLayoutManager(getActivity()) {
//            @Override
//            public boolean canScrollVertically() {
//                return false;
//            }
//
//            @Override
//            public boolean canScrollHorizontally() {
//                return false;
//            }
        };

        mIngredientsRecyclerView.setLayoutManager(llm);
        mDirectionsRecyclerView.setLayoutManager(llm2);

        // Disable nested scrolling to allow for proper scrolling physics
        mIngredientsRecyclerView.setNestedScrollingEnabled(false);
        mDirectionsRecyclerView.setNestedScrollingEnabled(false);

        // Set the AdapterIngredient for the ingredient's RecyclerView
        mIngredientsRecyclerView.setAdapter(mIngredientAdapter);
        mDirectionsRecyclerView.setAdapter(mDirectionAdapter);
    }

    private void loadImageView() {
        // Init the default parameters for loading the image with Glide
        boolean skipMemCache = false;
        DiskCacheStrategy strategy = DiskCacheStrategy.SOURCE;

        // Check that an image URL was passed with the Intent
        if (mImageUrl != null) {
            // Determine the scheme of the URL
            String scheme = Uri.parse(mImageUrl).getScheme();

            if (scheme.contains("file")) {
                // If the image is a local image, then Glide needs to skip loading from cache
                // otherwise it will load the wrong image on occasion.
                skipMemCache = true;
                strategy = DiskCacheStrategy.NONE;
            }
        } else {
            // If no image URL was passed, immediately start the transition animation
            scheduleStartPostponedTransition(mRecipeImageView);
        }

        // Use Glide to load the image into the ImageView
        Glide.with(this)
                .load(mImageUrl)
                .diskCacheStrategy(strategy)
                .skipMemoryCache(skipMemCache)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        // When image has finished loading, load image into target
                        target.onResourceReady(resource, null);

                        // Once the resource has been loaded, then start the transition animation
                        scheduleStartPostponedTransition(mRecipeImageView);

                        return false;

                    }
                })
                .into(mRecipeImageView);
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
            // Hide the views
            setInvisible();

            // Check if connected to a network
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (!mSyncing && isConnected) {
                // Set mSyncing to true so that the AsyncTask is only started once
                mSyncing = true;

                // If recipe is missing information, then load details from web
                RecipeImporter.importRecipeFromUrl(mContext, new RecipeImporter.UtilitySyncer() {
                    @Override
                    public void onFinishLoad() {
                        if (getActivity() != null)
                        getLoaderManager().restartLoader(DETAILS_LOADER, null, FragmentRecipeDetails.this);
                        mSyncing = false;

                        if (genericRecipe) {
                            // If recipe is a generic recipe, parsed from a web page, give tbe user
                            // option to edit the recipe and delete the original with mistakes

                            // Show a Snackbar informing the user of the ability to make changes
                            Snackbar editSnackbar = Snackbar.make(
                                    getActivity().getWindow().getDecorView().findViewById(android.R.id.content),
                                    getString(R.string.snackbar_check_recipe_details),
                                    Snackbar.LENGTH_INDEFINITE
                            );

                            // Set a click to open ActivityCreateRecipe with the boolean
                            // deleteOriginal set to true
                            editSnackbar.getView().setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Init Intent
                                    Intent intent = new Intent(getActivity(), ActivityCreateRecipe.class);

                                    // Set recipeUri as data to pass
                                    intent.setData(RecipeEntry.buildRecipeUriFromId(mRecipeId));
                                    intent.putExtra(ActivityCreateRecipe.DELETE_GENERIC_EXTRA, true);

                                    // Start ActivityCreateRecipe
                                    startActivity(intent);

                                    // Close ActivityRecipeDetails
                                    getActivity().finish();
                                }
                            });

                            // Show the Snackbar
                            editSnackbar.show();
                        }
                    }

                    @Override
                    public void onError() {
                        // If there is an error parsing the webpage for recipe information, show a
                        // Toast to alert the user of the failure
                        Toast.makeText(mContext, getString(R.string.toast_unable_to_parse_recipe), Toast.LENGTH_LONG).show();

                        // Return to ActivityRecipeList
                        getActivity().finish();
                    }
                }, mRecipeUrl);
            } else if (!mSyncing && !connectivityRegistered) {
                // Register a ConnectivityListener
                registerConnectivityListener();

                // Show a Snackbar informing the user of network status
                mSnackbar = Snackbar.make(
                        getActivity().getWindow().getDecorView().findViewById(android.R.id.content),
                        "Not connected to a network. Unable to download recipe details.",
                        Snackbar.LENGTH_INDEFINITE
                );

                // Set the Snackbar to be dismissed on click
                mSnackbar.getView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mSnackbar.dismiss();
                    }
                });

                mSnackbar.show();
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

    @Override
    public void onPause() {
        if (connectivityRegistered) {
            unregisterConnectivityListener();
        }
        super.onPause();
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
        Animation fadeOutAnim = AnimationUtils.loadAnimation(mContext, R.anim.fade_out);
        mProgressBar.startAnimation(fadeOutAnim);
        mProgressBar.setVisibility(View.INVISIBLE);

        Animation fadeInAnim = AnimationUtils.loadAnimation(mContext, R.anim.fade_in);

        mIngredientTitleText.setVisibility(View.VISIBLE);
        mDirectionTitleText.setVisibility(View.VISIBLE);
        mIngredientsRecyclerView.setVisibility(View.VISIBLE);
        mDirectionsRecyclerView.setVisibility(View.VISIBLE);
//        mRecipeImageView.setVisibility(View.VISIBLE);
        mShoppingListButton.setVisibility(View.VISIBLE);
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
        mShoppingListButton.startAnimation(fadeInAnim);
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
        mShoppingListButton.setVisibility(View.INVISIBLE);
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
        // Check whether the Fragment is attached to ActivityRecipesDetails
        if (getActivity() instanceof ActivityRecipeDetails) {
            // Query the database to see whether the image url exists
            long recipeId = Utilities.getRecipeIdFromUrl(getContext(), getActivity().getIntent().getData().toString());
            Uri linkUri = LinkIngredientEntry.buildIngredientUriFromRecipe(recipeId);

            Cursor cursor = getContext().getContentResolver().query(
                    linkUri,
                    LinkIngredientEntry.LINK_PROJECTION,
                    null,
                    null,
                    null
            );

            if (cursor != null && cursor.moveToFirst() && !cursor.getString(LinkIngredientEntry.IDX_IMG_URL).isEmpty()) {
                // Delay transition animation if an image can be loaded
                mProgressBar.setVisibility(View.INVISIBLE);
//                getActivity().supportPostponeEnterTransition();
                cursor.close();
            }

            if (cursor != null) cursor.close();
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
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
                getActivity().finish();
                return true;
            }

            case R.id.detail_menu_add_to_recipebook: {
                // Check whether there is already a recipe book and chapters created
                Cursor cursor = mContentResolver.query(
                        ChapterEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null
                );

                boolean bookExists = false;
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        bookExists = true;
                    }

                    cursor.close();
                }

                if (bookExists) {
                    AddToRecipeBookDialog dialog = new AddToRecipeBookDialog();
                    dialog.setRecipeId(mRecipeId);

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
                } else {
                    Toast.makeText(mContext,
                            getString(R.string.toast_no_recipe_books),
                            Toast.LENGTH_LONG
                    ).show();
                }

                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    public interface CursorLoaderListener {
        void onCursorLoaded(Cursor cursor, int recipeServings);
    }

    /**
     * Registers a ConnectivityListener to listen for a broadcast due to change in network state
     */
    private void registerConnectivityListener() {
        // Initialize a ConnectivityListener if it hasn't already been initialized
        if (mConnectivityListener == null) {
            mConnectivityListener = new ConnectivityListener();
        }

        // Create an IntentFilter for listening to a Broadcast for a change in network state
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

        // Register the receiver
        mContext.registerReceiver(mConnectivityListener, filter);

        // Set the boolean to indicate whether the ConnectivityListener is registered
        connectivityRegistered = true;
    }

    /**
     * Unregisters a registered ConnectivityListener
     */
    private void unregisterConnectivityListener() {
        // Unregister the ConnectivityListener
        mContext.unregisterReceiver(mConnectivityListener);

        // Set the boolean to indicate that no ConnectivityListener has been registered
        connectivityRegistered = false;
    }

    private class ConnectivityListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check if connected to a network
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            Log.d("Test", "test");
            if (!mSyncing && isConnected) {
                // Set mSyncing to true so that the AsyncTask is only started once
                mSyncing = true;

                // Hide the Snackbar
                mSnackbar.dismiss();

                // If recipe is missing information, then load details from web
                RecipeImporter.importRecipeFromUrl(mContext, new RecipeImporter.UtilitySyncer() {
                    @Override
                    public void onFinishLoad() {
                        if (getActivity() != null)
                            getLoaderManager().restartLoader(DETAILS_LOADER, null, FragmentRecipeDetails.this);
                        mSyncing = false;
                    }

                    @Override
                    public void onError() {
                        Toast.makeText(mContext, "Unable to identify the recipe on the website. \n Please use our \"Create recipe\" function to import the recipe.", Toast.LENGTH_LONG).show();
                        getActivity().finish();
                    }
                }, mRecipeUrl);
            }
        }
    }

    public interface BundleKeys {
        public static final String RECIPE_DETAILS_URL = "recipe_url";
        public static final String RECIPE_DETAILS_IMAGE_URL = "image_url";
        public static final String RECIPE_DETAILS_GENERIC = "generic_recipe";
    }
}
