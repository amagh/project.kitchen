package project.kitchen.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;

import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;
import project.kitchen.sync.RecipeSyncService;
import project.kitchen.ui.adapter.AdapterRecipe;
import project.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

import static project.kitchen.sync.RecipeSyncService.SYNC_INVALID;
import static project.kitchen.sync.RecipeSyncService.SYNC_SERVER_DOWN;
import static project.kitchen.sync.RecipeSyncService.SYNC_SUCCESS;

/**
 * Fragment for the main view displaying all recipes loaded from web
 */
public class FragmentRecipeList extends FragmentModel implements LoaderManager.LoaderCallbacks<Cursor> {
    /** Constants **/
    private final String LOG_TAG = FragmentRecipeList.class.getSimpleName();
    private static final int RECIPE_LOADER = 0;
    private final long DAY_IN_SECONDS = 86400;
    private final long HOUR_IN_SECONDS = 3600;

    /** Member Variables **/
    private Context mContext;                   // Interface for global context
    Cursor mCursor;
    AdapterRecipe mRecipeAdapter;
    private int mPosition;                      // Position of mCursor
    StaggeredGridLayoutManagerWithSmoothScroll mStaggeredLayoutManager;
    private SyncListener mSyncListener;
    private LocalBroadcastManager mBroadcastManager;

    // Views bound by ButterKnife
    @BindView(R.id.fragment_recyclerview) RecyclerView mRecipeRecyclerView;
    @BindView(R.id.recipe_error_textview) TextView mErrorTextView;
    @BindView(R.id.recipe_progressbar) ProgressBar mProgressBar;
    @BindView(R.id.recipe_error_card) CardView mErrorCard;

    public FragmentRecipeList() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        super.onCreateView(inflater, container, savedInstanceState);

        // Bind views using ButterKnife
        ButterKnife.bind(this, rootView);

        // Instantiate member variables
        mContext = getActivity();

        // Initialize AdapterRecipe and obtain a reference to it
        initRecipeAdapter();
        mRecipeAdapter = getRecipeAdapter();

        // Set the LayoutManager and the number of columns to be used
        setLayoutColumns();

        // Init the RecyclerView and obtain a reference to it
        initRecyclerView();
        mRecipeRecyclerView = getRecyclerView();

        mRecipeRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                // Hide the FAB when scrolling down and show it again when scrolling up
                if (dy > 0) {
                    // Check whether the FAB menu is open
                    if (ActivityRecipeList.hideFab) {
                        return;
                    }
                    if (ActivityRecipeList.mFabMenuOpen) {
                        // Set the boolean to true so that the FAB is closed at the end of the
                        // menu closing animation
                        ActivityRecipeList.hideFab = true;

                        // Start the animation to close the FAB menu
                        ((ActivityRecipeList)getActivity()).closeFabMenu();
                    } else {
                        ((ActivityRecipeList)getActivity()).mFab.hide();
                    }

                } else {
                    // Set the boolean so that the FAB can be hidden again if the menu is opened
                    ActivityRecipeList.hideFab = false;

                    // Close the FAB menu
                    if (ActivityRecipeList.mFabMenuOpen) {
                        ((ActivityRecipeList)getActivity()).closeFabMenu();
                    }

                    // Show the FAB
                    ((ActivityRecipeList)getActivity()).mFab.show();
                }
            }
        });

        // Initialize the listener for when the user attempts to search for a recipe
        ((ActivityRecipeList) getActivity()).setSearchListener(new ActivityRecipeList.SearchListener() {
            @Override
            public void onSearch(String searchTerm) {
                search(searchTerm);
            }
        });

        return rootView;
    }

    /**
     * Reloads the CursorLoader to only show results that include the user's search term
     * @param searchTerm The user-input search term for the recipes they wish to find
     */
    private void search(String searchTerm) {
        // Initialize the selection argument, passing in the searchTerm
        String[] selectionArgs = new String[] {"%" + searchTerm + "%"};

        // Create a Bundle and add the search argument to it
        Bundle args = new Bundle();
        args.putStringArray(getString(R.string.selection_args_key), selectionArgs);

        // Restart the CursorLoader and pass the Bundle as an argument
        getLoaderManager().restartLoader(RECIPE_LOADER, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Get the URI for the recipe table
        Uri recipeUri = RecipeEntry.CONTENT_URI;

        // Set the column projection (All columns for now)
        String[] projection = RecipeEntry.RECIPE_PROJECTION;

        // Set the sort order to newest recipes first
        String sortOrder = RecipeEntry.COLUMN_DATE_ADDED + " DESC";

        String selection = null;
        String[] selectionArgs = null;

        if (args != null) {
            // If arguments have been passed, set the new selection and selection arguments
            selection = RecipeEntry.COLUMN_RECIPE_NAME + " LIKE ?";
            selectionArgs = args.getStringArray(getString(R.string.selection_args_key));
        }

        // Return CursorLoader set to recipe table
        return new CursorLoader(mContext,
                recipeUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Set member variable mCursor to the loaded Cursor
        mCursor = cursor;

        // Swap in the loaded Cursor into the Adapter
        mRecipeAdapter.swapCursor(mCursor);

        // Hide the ProgressBar if mRecipeAdapter is populated with recipes
        if (mRecipeAdapter.getItemCount() > 0) {
            if (mProgressBar.getVisibility() == View.VISIBLE) {
                mProgressBar.setVisibility(View.GONE);
            }

            if (mErrorCard.getVisibility() == View.VISIBLE) {
                mErrorCard.setVisibility(View.GONE);
            }

        } else {
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap null in for cursor adapter to clear view
        mRecipeAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = getActivity();

        getLoaderManager().initLoader(RECIPE_LOADER, null, this);
    }

    interface RecipeCallBack {
        void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder);
    }


    /**
     * Register a BroadcastListener to listen when a sync has been completed by the
     * RecipeSyncServices
     */
    private void registerSyncListener() {
        // Initialize a SyncListener if it hasn't been initialized yet
        if (mSyncListener == null) {
            mSyncListener = new SyncListener();
        }

        // Initialize a LocalBroadcastManager if it hasn't been initialized yet
        if (mBroadcastManager == null) {
            mBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        }

        // Set the IntentFilter to listen for the local broadcast when a sync has been completed
        IntentFilter filter = new IntentFilter(getString(R.string.intent_filter_sync_finished));

        // Register the SyncListener to the LocalBroadcastManager
        mBroadcastManager.registerReceiver(mSyncListener, filter);
    }

    /**
     * Unregisters a registered SyncListener
     */
    private void unregisterSyncListener() {
        // Unregister the SyncListener
        mBroadcastManager.unregisterReceiver(mSyncListener);
    }

    @Override
    public void onResume() {
        // Register the SyncListener
        registerSyncListener();
        super.onResume();
    }

    @Override
    public void onPause() {
        // Unregister the SyncListener
        unregisterSyncListener();
        super.onPause();
    }

    private class SyncListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Retrieve the tag passed by the Intent for the sync status
            @RecipeSyncService.SyncStatus int flag = intent.getFlags();

            switch (flag) {
                case SYNC_SUCCESS: {
                    // Hide mErrorCard
                    Log.i(LOG_TAG, "Sync Successful!");
                    mErrorCard.setVisibility(View.GONE);
                    break;
                }

                case SYNC_SERVER_DOWN: {
                    Log.d(LOG_TAG, "Unable to connect!");
                    // Check how long since the last sync occurred
                    long currentTime = Utilities.getCurrentTime();
                    long lastSync = Utilities.getLastSyncTime(context);

                    // Convert the interval from milliseconds to seconds
                    long syncInterval = (currentTime - lastSync) / 1000;

                    // Initialize the the parameters that will be used to create the String message
                    int timePassed;
                    String timeUnit;
                    if (syncInterval < DAY_IN_SECONDS) {
                        // If less than a day has passed, convert the number of seconds to hours
                        timePassed = (int) (syncInterval / HOUR_IN_SECONDS);

                        // Set the time unit to singular or multiple
                        if (timePassed == 1) {
                            timeUnit = getString(R.string.hour_singular);
                        } else {
                            timeUnit = getString(R.string.hour_multiple);
                        }

                    } else {
                        // Convert the time passed to days
                        timePassed = (int) (syncInterval / DAY_IN_SECONDS);

                        // Set the time unit to singular or multiple
                        if (timePassed == 1) {
                            timeUnit = getString(R.string.day_singular);
                        } else {
                            timeUnit = getString(R.string.day_multiple);
                        }
                    }

                    if (mRecipeAdapter.getItemCount() == 0) {
                        // If the sync fails and there are no items in mRecipeAdapter, show
                        // mErrorCard
                        mErrorCard.setVisibility(View.VISIBLE);
                        mErrorTextView.setText(getString(R.string.error_network_down));
                    }

                    // Create the String message to display to the user to notify them of how long
                    // since the last sync
                    String timeString = getString(R.string.error_last_sync, timePassed + " " + timeUnit);

                    // Create a Snackbar to hold the message
                    final Snackbar snackBar = Snackbar.make(((ActivityRecipeList)getActivity()).mDrawerLayout,
                            timeString,
                            Snackbar.LENGTH_INDEFINITE
                    );

                    // Set the Snackbar to be dismissed on click
                    snackBar.getView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            snackBar.dismiss();
                        }
                    });

                    // Show the Snackbar
                    snackBar.show();

                    break;
                }

                case SYNC_INVALID: {
                    Log.d(LOG_TAG, "Error in retrieved document!");
                    if (mRecipeAdapter.getItemCount() == 0) {
                        // If the sync retrieves an invalid document and there are no items in
                        // mRecipeAdapter, show mErrorCard
                        mErrorCard.setVisibility(View.VISIBLE);
                        mErrorTextView.setText(getString(R.string.error_unknown));
                    }
                    break;
                }
            }

            // Hide the ProgressBar if it is visible
            if (mProgressBar.getVisibility() == View.VISIBLE) {
                mProgressBar.setVisibility(View.GONE);
            }
        }
    }
}
