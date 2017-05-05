package project.hnoct.kitchen.ui;

import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.Optional;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeDbHelper;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.dialog.ImportRecipeDialog;
import project.hnoct.kitchen.prefs.SettingsActivity;
import project.hnoct.kitchen.sync.AllRecipesService;
import project.hnoct.kitchen.sync.EpicuriousService;
import project.hnoct.kitchen.sync.FoodDotComService;
import project.hnoct.kitchen.sync.RecipeImporter;
import project.hnoct.kitchen.sync.RecipeGcmService;
import project.hnoct.kitchen.sync.SeriousEatsService;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;

public class ActivityRecipeList extends AppCompatActivity implements FragmentRecipeList.RecipeCallBack, ImportRecipeDialog.ImportRecipeDialogListener {
    /** Constants **/
    private static final String LOG_TAG = ActivityRecipeList.class.getSimpleName();
    private final String DETAILS_FRAGMENT = "DFTAG";
    public static final String TIME_IN_MILLIS = "timeInMillis";
    private static final boolean DEVELOPER_MODE = true;
    private int SIX_HOURS_IN_SECONDS = 60 * 60 * 6;
    private int FLEX_TWO_HOURS = 60 * 60 * 2;
    private final long DAY_IN_SECONDS = 86400;
    private final long HOUR_IN_SECONDS = 3600;

    /** Member Variables **/
    private static boolean mFabMenuOpen;
    public static boolean mTwoPane = false;
    public static boolean mDetailsVisible = false;
    private static int mPosition;
    private SearchListener mSearchListener;
    private ConnectivityListener mConnectivityListener;
    private boolean isConnected;
    private boolean connectivityRegistered = false;
    private Snackbar mSnackBar;

    // Bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.main_menu_fab) FloatingActionButton mFab;
    @BindView(R.id.main_add_recipe_fab) FloatingActionButton mAddRecipeFab;
    @BindView(R.id.main_import_recipe_fab) FloatingActionButton mImportRecipeFab;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_menu_text) TextView mMainFabText;
    @BindView(R.id.main_add_recipe_text) TextView mAddRecipeText;
    @BindView(R.id.main_import_recipe_text) TextView mImportRecipeText;
    @BindView(R.id.main_drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.recipe_search_more) CardView mSearchMore;
    @Nullable @BindView(R.id.recipe_detail_container) FrameLayout mDetailsContainer;
    @Nullable @BindView(R.id.detail_fragment_container) FrameLayout mContainer;
    @Nullable @BindView(R.id.temp_button) ImageView mTempButton;
    @Nullable @BindView(R.id.searchview) EditText mSearchView;
    @Nullable @BindView(R.id.search_icon) ImageView mSearchIcon;
    @Nullable @BindView(R.id.app_title) TextView mTitle;

    @Optional
    @OnClick(R.id.temp_button)
    public void closePreview() {
        mDetailsVisible = false;
        ViewGroup.LayoutParams params = mContainer.getLayoutParams();
        params.width = 0;
        mContainer.setLayoutParams(params);
        FragmentRecipeList fragment = (FragmentRecipeList) getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setLayoutColumns();
//        fragment.mRecipeRecyclerView.scrollToPosition(mPosition);
    }

    @Optional
    @OnEditorAction(R.id.searchview)
    boolean onEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            // Re-query the database with the search term when user presses the search button on the
            // soft keyboard
            // Retrieve the search term from mSearchView
            String searchTerm = mSearchView != null ? mSearchView.getText().toString() : null;
            if (mSearchListener != null && searchTerm != null && !searchTerm.trim().isEmpty()) {
                // Inform FragmentRecipeList of the change in query parameters
                mSearchListener.onSearch(searchTerm);

                // Show the CardView allowing the user to import additional recipes from online if
                // they cannot find a recipe
                mSearchMore.setVisibility(View.VISIBLE);
            }

            // Hide the soft keyboard
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            return true;
        }
        return false;
    }

    @Optional
    @OnClick(R.id.recipe_search_more)
    void searchMore() {
        // Start ActivitySearch and pass the searchTerm from mSearchView in a Bundle
        Intent intent = new Intent(this, ActivitySearch.class);

        // Retrieve the user-added search term
        String searchTerm = mSearchView != null ? mSearchView.getText().toString() : null;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Pass the search term as an extra within the Intent
            intent.putExtra(ActivitySearch.SEARCH_TERM, searchTerm);
            // Start ActivitySearch with the Bundle
            startActivity(intent);
        }
    }

    @Optional
    @OnClick(R.id.search_icon)
    void onClick() {
        if (mSearchView.getVisibility() == View.GONE) {
            showSearch();
        } else {
            hideSearch();
        }
    }

    /**
     * Shows the SearchView, allowing the user to search recipes
     */
    void showSearch() {
        // Set the visibility of the SearchView
        mSearchView.setVisibility(View.VISIBLE);

        // Request focus
        mSearchView.requestFocus();

        // Change the search icon to a cancel icon
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_close_clear_cancel));

        // Hide the app title
        mTitle.setVisibility(View.GONE);

        // Show the soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this.getCurrentFocus(), 0);
    }

    /**
     * Hides the SearchView and resets the filter
     */
    void hideSearch() {
        // Hide the SearchView and show the search icon
        mSearchView.setVisibility(View.GONE);
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_search));

        // Hide the Card allowing the user to search additional recipes online
        mSearchMore.setVisibility(View.GONE);

        // Show the app title
        mTitle.setVisibility(View.VISIBLE);

        if (mSearchListener != null) {
            // Reset the search filter
            mSearchView.setText("");
            mSearchListener.onSearch("");
        }

        // Hide the soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
    }

    @OnClick(R.id.main_menu_fab)
    public void onClickFabMenu() {
        if (!mFabMenuOpen) {
            showFabMenu();
        } else {
            closeFabMenu();
        }
    }

    @OnClick(R.id.main_import_recipe_fab)
    public void onClickFabImport() {
        closeFabMenu();
        showImportDialog();
    }

    @OnClick(R.id.main_add_recipe_fab)
    public void createRecipe() {
        closeFabMenu();
        startActivity(new Intent(this, ActivityCreateRecipe.class));
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                    .detectDiskReads()
//                    .detectDiskWrites()
//                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });

        if (findViewById(R.id.recipe_detail_container) == null) {
            // If container is not found, then utilizing phone layout without two panes
            mTwoPane = false;
        } else {
            // Set the boolean indicating that there are two panes in view
            mTwoPane = true;
            if (savedInstanceState == null && !mDetailsVisible) {
                // If no recipe has been selected yet, then set boolean indicating that the
                // details fragment is not visible
                mDetailsVisible = false;

                // Set the visibility of the container to GONE to allow the FragmentRecipeList
                // to take the full width of the view
                ViewGroup.LayoutParams params = mContainer.getLayoutParams();
                params.width = 0;
                mContainer.setLayoutParams(params);
//                mContainer.setVisibility(View.GONE);

                // Create a new FragmentRecipeDetails and load it into the container
                FragmentRecipeDetails fragment = new FragmentRecipeDetails();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                        .commit();
            }
        }

        // Query the database to check if any recipes exist in case the user has wiped data somehow
        Cursor cursor = getContentResolver().query(
                RecipeContract.RecipeEntry.CONTENT_URI,
                RecipeContract.RecipeEntry.RECIPE_PROJECTION,
                null,
                null,
                null
        );

        // Check for network connectivity
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // Check when the recipes were last synced
        long lastSync = Utilities.getLastSyncTime(this);
        long currentTime = Utilities.getCurrentTime();

        if (currentTime - lastSync > SIX_HOURS_IN_SECONDS * 1000 ||
                (cursor != null && !cursor.moveToFirst())) {

            if (isConnected) {
                // If the database was last synced more than six hours ago (e.g. on first start), then
                // the recipes are immediately synced
                syncImmediately();

                // Check to see if the device has GooglePlayServices
                if (checkGooglePlayServices()) {
                    // If so, schedule a periodic task to sync the recipes in the background every six hours
                    GcmNetworkManager networkManager = GcmNetworkManager.getInstance(this);

                    PeriodicTask task = new PeriodicTask.Builder()
                            .setService(RecipeGcmService.class)
                            .setPeriod(SIX_HOURS_IN_SECONDS)
                            .setFlex(FLEX_TWO_HOURS)
                            .setRequiredNetwork(PeriodicTask.NETWORK_STATE_CONNECTED)
                            .setTag("periodic")
                            .setPersisted(true)
                            .setUpdateCurrent(true)
                            .build();

                    networkManager.schedule(task);
                }
            } else {
                // Convert the interval from milliseconds to seconds
                long syncInterval = (currentTime - lastSync) / 1000;

                if (!connectivityRegistered) {
                    // Register a ConnectivityListener
                    registerConnectivityListener();
                }

                // Show a Snackbar displaying the error to the user
                showErrorSnackbar(syncInterval);
            }

        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Show a Snackbar informing the user of a lack of network connectivity
     * @param syncInterval Time in seconds since the last successful sync
     */
    void showErrorSnackbar(long syncInterval) {
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

        // Create the String message to display to the user to notify them of how long
        // since the last sync
        String timeString = getString(R.string.error_last_sync, timePassed + " " + timeUnit);

        // Create a Snackbar to hold the message
        mSnackBar = Snackbar.make(mDrawerLayout,
                timeString,
                Snackbar.LENGTH_INDEFINITE
        );

        // Set the Snackbar to be dismissed on click
        mSnackBar.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSnackBar.dismiss();
            }
        });

        // Show the Snackbar
        mSnackBar.show();
    }

    /**
     * Initialize and start all RecipeSyncServices to begin importing recipes from the web
     */
    void syncImmediately() {
        // Utilize the current time as the seed time for each of the RecipeSyncServices
        long currentTime = Utilities.getCurrentTime();

        // Initialize and start the Services
        Intent allRecipesIntent = new Intent(this, AllRecipesService.class);
        allRecipesIntent.putExtra(getString(R.string.extra_time), currentTime);
        startService(allRecipesIntent);

        Intent epicuriousIntent = new Intent(this, EpicuriousService.class);
        epicuriousIntent.putExtra(getString(R.string.extra_time), currentTime);
        startService(epicuriousIntent);

        Intent foodIntent = new Intent(this, FoodDotComService.class);
        foodIntent.putExtra(getString(R.string.extra_time), currentTime);
        startService(foodIntent);

        Intent seriousIntent = new Intent(this, SeriousEatsService.class);
        seriousIntent.putExtra(getString(R.string.extra_time), currentTime);
        startService(seriousIntent);
    }

    /**
     * Checks whether the user has GooglePlayServices
     * @return boolean value for whether the device includes GooglePlayServices
     */
    public boolean checkGooglePlayServices() {
        // Retrieve an instance of the GoogleApiAvailability object
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();

        // Check whether the device includes GooglePlayServices
        int resultCode = api.isGooglePlayServicesAvailable(this);
        if (resultCode == ConnectionResult.SUCCESS) {
            // If it does, return true
            return true;
        } else if (api.isUserResolvableError(resultCode)) {
            // If Google Play Services are available to download to the user, show the error Dialog
            api.showErrorDialogFragment(this, resultCode, 9000);
            return false;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        if (mSearchView.getVisibility() == View.VISIBLE) {
            hideSearch();
            return;
        }
        super.onBackPressed();
    }

    private void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorites: {
                startActivity(new Intent(this, ActivityFavorites.class));
                hideNavigationDrawer();
                mDetailsVisible = false;
                break;
            }
            case R.id.action_settings: {
                startActivity(new Intent(this, SettingsActivity.class));
                hideNavigationDrawer();
                mDetailsVisible = false;
                break;
            }
            case R.id.action_my_recipes: {
                startActivity(new Intent(this, ActivityMyRecipes.class));
                hideNavigationDrawer();
                mDetailsVisible = false;
                break;
            }
            case R.id.action_my_recipe_books: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, ActivityRecipeBook.class);
                startActivity(intent);
                mDetailsVisible = false;
                break;
            }
            case R.id.action_copy_db: {
                File sd = Environment.getExternalStorageDirectory();
                File database = getApplicationContext().getDatabasePath(RecipeDbHelper.DATABASE_NAME + ".db");
                Log.d(LOG_TAG, sd.toString());
                if (sd.canWrite()) {
                    Log.d(LOG_TAG, "Able to write to SD.");
                    File dbCopy = new File(sd, RecipeDbHelper.DATABASE_NAME + ".db");
                    if (database.exists()) {
                        Log.d(LOG_TAG, "Database exists.");
                        try {
                            FileChannel src = new FileInputStream(database).getChannel();
                            FileChannel dst = new FileInputStream(dbCopy).getChannel();
                            dst.transferFrom(src, 0, src.size());

                            src.close();
                            dst.close();
                            Toast.makeText(this, "Database copied to external storage!", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
                break;
            }
            case R.id.action_clear_data: {
                // Delete the database and restart the application to rebuild it
                boolean deleted = deleteDatabase(RecipeDbHelper.DATABASE_NAME);
                Log.d(LOG_TAG, "Database deleted " + deleted);

                // Set an Alarm to re-open the Application right after it is closed
                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                PendingIntent restartIntent = PendingIntent.getActivity(
                        getBaseContext(), 0, new Intent(getIntent()),
                        PendingIntent.FLAG_ONE_SHOT);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, restartIntent);

                // Exit the application
                System.exit(2);
                break;
            }
        }
    }

    /**
     * Hides the Navigation Drawer
     */
    private void hideNavigationDrawer() {
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    /**
     * Opens the FAB Menu
     */
    private void showFabMenu() {
        // Set the boolean to true
        mFabMenuOpen = true;

        // Make the menu options VISIBLE
        mAddRecipeFab.setVisibility(View.VISIBLE);
        mImportRecipeFab.setVisibility(View.VISIBLE);
        mMainFabText.setVisibility(View.VISIBLE);
        mAddRecipeText.setVisibility(View.VISIBLE);
        mImportRecipeText.setVisibility(View.VISIBLE);

        // Set the icon for the FAB to the cancel icon
        mFab.setImageResource(R.drawable.ic_menu_close_clear_cancel);
    }

    /**
     * Closes the FAB Menu
     */
    private void closeFabMenu() {
        // Set the boolean to false
        mFabMenuOpen = false;

        // Make the menu options GONE
        mAddRecipeFab.setVisibility(View.GONE);
        mImportRecipeFab.setVisibility(View.GONE);
        mMainFabText.setVisibility(View.GONE);
        mAddRecipeText.setVisibility(View.GONE);
        mImportRecipeText.setVisibility(View.GONE);

        // Set the FAB icon to the add icon
        mFab.setImageResource(R.drawable.ic_menu_add_custom);
    }

    /**
     * Shows a Dialog with an EditText to allow copy-pasting of a recipe URL so it can be imported
     */
    private void showImportDialog() {
        ImportRecipeDialog dialog = new ImportRecipeDialog();
        String IMPORT_DIALOG = "ImportRecipeDialog";
        dialog.show(getFragmentManager(), IMPORT_DIALOG);
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        if (!mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            View statusBar = findViewById(android.R.id.statusBarBackground);
            View navigationBar = findViewById(android.R.id.navigationBarBackground);
            View appbar = findViewById(R.id.appbar);

            List<Pair<View, String>> pairs = new ArrayList<>();
            pairs.add(Pair.create(statusBar, Window.STATUS_BAR_BACKGROUND_TRANSITION_NAME));
            if (navigationBar != null) {
                pairs.add(Pair.create(navigationBar, Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME));
            }
//            pairs.add(Pair.create(appbar, "test"));
            pairs.add(Pair.create((View) viewHolder.recipeImage, getString(R.string.transition_recipe_image)));
            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    pairs.toArray(new Pair[pairs.size()])
            );
            Intent intent = new Intent(this, ActivityRecipeDetails.class);
            intent.setData(Uri.parse(recipeUrl));
            intent.putExtra(getString(R.string.extra_image), imageUrl);
            ActivityCompat.startActivity(this, intent, options.toBundle());
        } else {
            mDetailsVisible = true;

            // Create a new FragmentRecipeDetails
            FragmentRecipeDetails fragment = new FragmentRecipeDetails();


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Set a fade animation to occur during transition between recipes in two-pane
                fragment.setEnterTransition(new Fade());
            }

            // Create the Bundle and add the recipe's URL to it and set it as the argument for the
            // fragment
            Bundle args = new Bundle();
            args.putParcelable(FragmentRecipeDetails.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            fragment.setArguments(args);

            // Replace the existing FragmentRecipeDetails with the newly created one
            getSupportFragmentManager().beginTransaction()
                    .addSharedElement(viewHolder.recipeImage, getString(R.string.transition_recipe_image))
                    .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                    .commit();

            // Show the FragmentRecipeDetails in the master-flow view
            ViewGroup.LayoutParams params = mContainer.getLayoutParams();
            params.width = (int) Utilities.convertDpToPixels(600);
            mContainer.setLayoutParams(params);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String recipeUrl) {
        if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {

            RecipeImporter.importRecipeFromUrl(this, new RecipeImporter.UtilitySyncer() {
                @Override
                public void onFinishLoad() {
                    FragmentRecipeList recipeListFragment =
                            (FragmentRecipeList) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    recipeListFragment.mRecipeAdapter.notifyDataSetChanged();
                    recipeListFragment.mRecipeAdapter.setDetailCardPosition(0);
                }
            }, recipeUrl);

        } else if (!mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            Intent intent = new Intent(this, ActivityRecipeDetails.class);
            intent.setData(Uri.parse(recipeUrl));
            startActivity(intent);
        } else {
            mDetailsVisible = true;

            // Create a new FragmentRecipeDetails
            FragmentRecipeDetails fragment = new FragmentRecipeDetails();

            // Create the Bundle and add the recipe's URL to it and set it as the argument for the
            // fragment
            Bundle args = new Bundle();
            args.putParcelable(FragmentRecipeDetails.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            fragment.setArguments(args);

            // Replace the existing FragmentRecipeDetails with the newly created one
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                    .commit();

            // Show the FragmentRecipeDetails in the master-flow view
            mContainer.setVisibility(View.VISIBLE);

            FragmentRecipeList recipeListFragment = (FragmentRecipeList) getSupportFragmentManager().findFragmentById(R.id.fragment);
            recipeListFragment.setLayoutColumns();
        }
    }

    /**
     * Interface for informing a registered observer of the search term that the user has input
     */
    interface SearchListener {
        void onSearch(String searchTerm);
    }

    /**
     * Setter for the SearchListener
     * @param listener
     */
    public void setSearchListener(SearchListener listener) {
        mSearchListener = listener;
    }

    @Override
    protected void onResume() {
        // Check when the last sync occurred
        long lastSync = Utilities.getLastSyncTime(this);
        long currentTime = Utilities.getCurrentTime();

        long syncInterval = currentTime - lastSync;
        if (syncInterval > SIX_HOURS_IN_SECONDS * 1000 && !connectivityRegistered && !isConnected) {
            // If the database was last synced over six hours ago and the user is not connected to a
            // network, register a ConnectivityListener to listen for changes in network state
            registerConnectivityListener();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Check if a ConnectivityListener has been registered
        if (connectivityRegistered) {
            // If it has, unregister the listener
            unregisterConnectivityListener();
        }
        super.onPause();
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
        registerReceiver(mConnectivityListener, filter);

        // Set the boolean to indicate whether the ConnectivityListener is registered
        connectivityRegistered = true;
    }

    /**
     * Unregisters a registered ConnectivityListener
     */
    private void unregisterConnectivityListener() {
        // Unregister the ConnectivityListener
        unregisterReceiver(mConnectivityListener);

        // Set the boolean to indicate that no ConnectivityListener has been registered
        connectivityRegistered = false;
    }

    /**
     * A BroadcastListener for observing changes in network state and syncing recipes if the network
     * is connected
     */
    private class ConnectivityListener extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Check whether the device is connected to an active network
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                // Dismiss the Snackbar
                mSnackBar.dismiss();

                // Immediately sync all recipe sources
                syncImmediately();

                // Unregister the ConnectivityListener
                unregisterConnectivityListener();
            }
        }
    }
}
