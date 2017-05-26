package project.kitchen.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
import com.google.android.gms.tasks.RuntimeExecutionException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.Optional;
import project.kitchen.R;
import project.kitchen.data.RecipeContract;
import project.kitchen.data.Utilities;
import project.kitchen.dialog.ImportRecipeDialog;
import project.kitchen.sync.AllRecipesService;
import project.kitchen.sync.EpicuriousService;
import project.kitchen.sync.FoodDotComService;
import project.kitchen.sync.RecipeImporter;
import project.kitchen.sync.RecipeGcmService;
import project.kitchen.sync.SeriousEatsService;
import project.kitchen.ui.adapter.AdapterRecipe;

public class ActivityRecipeList extends ActivityModel implements FragmentModel.RecipeCallback {
    /** Constants **/
    private static final String LOG_TAG = ActivityRecipeList.class.getSimpleName();
    private final String DETAILS_FRAGMENT = "DFTAG";
    private static final boolean DEVELOPER_MODE = true;
    private int SIX_HOURS_IN_SECONDS = 60 * 60 * 6;
    private int FLEX_TWO_HOURS = 60 * 60 * 2;
    private final long DAY_IN_SECONDS = 86400;
    private final long HOUR_IN_SECONDS = 3600;


    /** Member Variables **/
    public static boolean mFabMenuOpen;
    public static boolean hideFab = false;
    public static boolean mTwoPane = false;
    public static boolean mDetailsVisible = false;
    private SearchListener mSearchListener;
    private ConnectivityListener mConnectivityListener;
    private boolean isConnected;
    private boolean connectivityRegistered = false;
    private Snackbar mSnackBar;

    public static List<AnimatorSet> mAnimQueue = new ArrayList<>();
    private boolean animQueueLock = false;


    // Bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.main_menu_fab) FloatingActionButton mFab;
    @BindView(R.id.main_add_recipe_fab) FloatingActionButton mAddRecipeFab;
    @BindView(R.id.main_import_recipe_fab) FloatingActionButton mImportRecipeFab;
    @BindView(R.id.navigation_drawer) NavigationView mNavigationView;
    @BindView(R.id.main_menu_text) TextView mMainFabText;
    @BindView(R.id.main_add_recipe_text) TextView mAddRecipeText;
    @BindView(R.id.main_import_recipe_text) TextView mImportRecipeText;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.recipe_search_more) CardView mSearchMore;
    @Nullable @BindView(R.id.detail_fragment_container) FrameLayout mDetailsContainer;
    @Nullable @BindView(R.id.temp_button) ImageView mTempButton;
    @Nullable @BindView(R.id.searchview) EditText mSearchView;
    @Nullable @BindView(R.id.search_icon) ImageView mSearchIcon;
    @Nullable @BindView(R.id.app_title) TextView mTitle;

    @Optional
    @OnClick(R.id.temp_button)
    public void closePreview() {
        mDetailsVisible = false;

        hideDetailsContainer();

        mDetailsVisible = false;

        FragmentRecipeList fragment = (FragmentRecipeList) getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setLayoutColumns();
//        fragment.mRecipeRecyclerView.scrollToPosition(mPosition);
    }

    @Optional
    @OnEditorAction(R.id.searchview)
    boolean onEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
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
        if (mSearchView.getVisibility() == View.INVISIBLE) {
            showSearch();
        } else {
            hideSearch();
        }
    }

    /**
     * Shows the SearchView, allowing the user to search recipes
     */
    void showSearch() {
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
        // Setup the animation for mSearchView
        mSearchView.setPivotY(mSearchView.getHeight());
        ObjectAnimator searchAnim = ObjectAnimator.ofFloat(mSearchView, "scaleY", 0.1f, 1.0f);
        searchAnim.setDuration(100);
        searchAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Set the visibility of the SearchView
                mSearchView.setVisibility(View.VISIBLE);

                // Request focus
                mSearchView.requestFocus();
            }
        });

        // Setup the animation for mTitle
        mTitle.setPivotY(mTitle.getHeight());
        ObjectAnimator titleAnim = ObjectAnimator.ofFloat(mTitle, "scaleY", 1.0f, 0.0f);
        searchAnim.setDuration(50);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playSequentially(titleAnim, searchAnim);
        animSet.setInterpolator(interpolator);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Hide the app title
                mTitle.setVisibility(View.INVISIBLE);
            }
        });
        animSet.start();

        // Change the search icon to a cancel icon
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_close_clear_cancel));

        // Show the soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this.getCurrentFocus(), 0);
    }

    /**
     * Hides the SearchView and resets the filter
     */
    void hideSearch() {
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        // Setup the animation for mSearchView
        mSearchView.setPivotY(mSearchView.getHeight());
        ObjectAnimator searchAnim = ObjectAnimator.ofFloat(mSearchView, "scaleY", 1.0f, 0.0f);
        searchAnim.setDuration(100);
        searchAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Set the visibility of the SearchView
                mSearchView.setVisibility(View.INVISIBLE);
            }
        });

        // Setup the animation for mTitle
        mTitle.setPivotY(mTitle.getHeight());
        ObjectAnimator titleAnim = ObjectAnimator.ofFloat(mTitle, "scaleY", 0.1f, 1.0f);
        searchAnim.setDuration(100);

        AnimatorSet animSet = new AnimatorSet();
        animSet.playSequentially(searchAnim, titleAnim);
        animSet.setInterpolator(interpolator);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Hide the app title
                mTitle.setVisibility(View.VISIBLE);
            }
        });
        animSet.start();

        // Hide the SearchView and show the search icon
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_search));

        // Hide the Card allowing the user to search additional recipes online
        mSearchMore.setVisibility(View.GONE);

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
        setActivityId(R.id.action_browse);

        // Set up the hamburger menu used for opening mDrawerLayout
        initNavigationDrawer();

        // Set up whether to use tablet or phone layout
        initLayout(savedInstanceState);
        mTwoPane = getTwoPane();

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
                cursor == null || !cursor.moveToFirst()) {

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

    /**
     * Opens the FAB Menu with animation
     */
    public void showFabMenu() {
        // Initialize the Interpolator to be used
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        // Initialize the AnimatorSets for each animation
        final AnimatorSet addRecipeAnimSet = new AnimatorSet();
        final AnimatorSet importRecipeAnimSet = new AnimatorSet();
        final AnimatorSet textAnimSet = new AnimatorSet();

        // Scale animation for mAddRecipeFab
        ObjectAnimator addRecipeXAnim = ObjectAnimator.ofFloat(mAddRecipeFab, "scaleX", 0.1f, 1.0f);
        addRecipeXAnim.setDuration(150);
        ObjectAnimator addRecipeYAnim = ObjectAnimator.ofFloat(mAddRecipeFab, "scaleY", 0.1f, 1.0f);
        addRecipeYAnim.setDuration(150);
        addRecipeYAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAddRecipeFab.setVisibility(View.VISIBLE);
            }
        });

        // Scale animation for mImportRecipeFab
        ObjectAnimator importRecipeXAnim = ObjectAnimator.ofFloat(mImportRecipeFab, "scaleX", 0.1f, 1.0f);
        importRecipeXAnim.setDuration(150);
        ObjectAnimator importRecipeYAnim = ObjectAnimator.ofFloat(mImportRecipeFab, "scaleY", 0.1f, 1.0f);
        importRecipeYAnim.setDuration(150);
        importRecipeYAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mImportRecipeFab.setVisibility(View.VISIBLE);
            }
        });

        // Scale animation for mMainFabText
        mMainFabText.setPivotX(mMainFabText.getWidth());
        ObjectAnimator mainTextXAnim = ObjectAnimator.ofFloat(mMainFabText, "scaleX", 0.1f, 1.0f);
        mainTextXAnim.setDuration(100);
        mainTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mMainFabText.setVisibility(View.VISIBLE);
            }
        });

        // Scale animation for mAddRecipeText
        mAddRecipeText.setPivotX(mAddRecipeText.getWidth());
        ObjectAnimator addTextXAnim = ObjectAnimator.ofFloat(mAddRecipeText, "scaleX", 0.1f, 1.0f);
        addTextXAnim.setDuration(100);
        addTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAddRecipeText.setVisibility(View.VISIBLE);

            }
        });

        // Scale animation for mImportRecipeText
        mImportRecipeText.setPivotX(mImportRecipeText.getWidth());
        ObjectAnimator importTextXAnim = ObjectAnimator.ofFloat(mImportRecipeText, "scaleX", 0.1f, 1.0f);
        importTextXAnim.setDuration(100);
        importTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mImportRecipeText.setVisibility(View.VISIBLE);
            }
        });

        // Rotation animation for mFab
        ObjectAnimator fabRotationAnim = ObjectAnimator.ofFloat(mFab, "rotation", 0f, 360f);
        fabRotationAnim.setDuration(150);
        fabRotationAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mFab.setImageResource(R.drawable.ic_menu_close_clear_cancel);
            }
        });

        // Setup the AnimatorSets
        // Set up the AnimatorSets
        addRecipeAnimSet.playTogether(addRecipeXAnim, addRecipeYAnim);
        addRecipeAnimSet.setInterpolator(interpolator);
        addRecipeAnimSet.setStartDelay(100);
        addRecipeAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(addRecipeAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(addRecipeAnimSet);
                }
            }
        });
        importRecipeAnimSet.playTogether(importRecipeXAnim, importRecipeYAnim, fabRotationAnim);
        importRecipeAnimSet.setInterpolator(interpolator);

        importRecipeAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(importRecipeAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(importRecipeAnimSet);
                }
            }
        });
        textAnimSet.playSequentially(mainTextXAnim, importTextXAnim, addTextXAnim);
        textAnimSet.setInterpolator(interpolator);
        textAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(textAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(textAnimSet);
                }
            }
        });

        // Remove any animations that have yet to be played
        if (!animQueueLock) {
            animQueueLock = true;
            for (AnimatorSet anim : mAnimQueue) {
                if (anim.equals(addRecipeAnimSet) || anim.equals(importRecipeAnimSet) || anim.equals(textAnimSet)) {
                    continue;
                }
                anim.cancel();
            }
            animQueueLock = false;
        }

        // Add AnimatorSets to mAnimQueue
        if (!animQueueLock) {
            mAnimQueue.add(addRecipeAnimSet);
            mAnimQueue.add(importRecipeAnimSet);
            mAnimQueue.add(textAnimSet);
        }

        // Start the animations
        addRecipeAnimSet.start();
        importRecipeAnimSet.start();
        textAnimSet.start();
        fabRotationAnim.start();

        // Set the boolean to true
        mFabMenuOpen = true;
    }

    /**
     * Closes the FAB Menu with animation
     */
    public void closeFabMenu() {
        // Initialize the interpolater to be used
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        // Initialize the AnimatorSets for each animation
        final AnimatorSet addRecipeAnimSet = new AnimatorSet();
        final AnimatorSet importRecipeAnimSet = new AnimatorSet();
        final AnimatorSet textAnimSet = new AnimatorSet();

        // Scale animation for mAddRecipeFab
        ObjectAnimator addRecipeXAnim = ObjectAnimator.ofFloat(mAddRecipeFab, "scaleX", 1.0f, 0.1f);
        addRecipeXAnim.setDuration(150);
        ObjectAnimator addRecipeYAnim = ObjectAnimator.ofFloat(mAddRecipeFab, "scaleY", 1.0f, 0.1f);
        addRecipeYAnim.setDuration(150);
        addRecipeYAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAddRecipeFab.setVisibility(View.INVISIBLE);
            }
        });

        // Scale animation for mImportRecipeFab
        ObjectAnimator importRecipeXAnim = ObjectAnimator.ofFloat(mImportRecipeFab, "scaleX", 1.0f, 0.1f);
        importRecipeXAnim.setDuration(150);
        ObjectAnimator importRecipeYAnim = ObjectAnimator.ofFloat(mImportRecipeFab, "scaleY", 1.0f, 0.1f);
        importRecipeYAnim.setDuration(150);
        importRecipeYAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mImportRecipeFab.setVisibility(View.INVISIBLE);
            }
        });

        // Scale animation for mMainFabText
        mMainFabText.setPivotX(mMainFabText.getWidth());
        ObjectAnimator mainTextXAnim = ObjectAnimator.ofFloat(mMainFabText, "scaleX", 1.0f, 0.1f);
        mainTextXAnim.setDuration(100);
        mainTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mMainFabText.setVisibility(View.INVISIBLE);
            }
        });

        // Scale animation for mAddRecipeText
        mAddRecipeText.setPivotX(mAddRecipeText.getWidth());
        ObjectAnimator addTextXAnim = ObjectAnimator.ofFloat(mAddRecipeText, "scaleX", 1.0f, 0.1f);
        addTextXAnim.setDuration(100);
        addTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAddRecipeText.setVisibility(View.INVISIBLE);
            }
        });

        // Scale animation for mImportRecipeText
        mImportRecipeText.setPivotX(mImportRecipeText.getWidth());
        ObjectAnimator importTextXAnim = ObjectAnimator.ofFloat(mImportRecipeText, "scaleX", 1.0f, 0.1f);
        importTextXAnim.setDuration(100);
        importTextXAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mImportRecipeText.setVisibility(View.INVISIBLE);
            }
        });

        // Rotation animation for mFab
        ObjectAnimator fabRotationAnim = ObjectAnimator.ofFloat(mFab, "rotation", 0f, 360f);
        fabRotationAnim.setDuration(150);
        fabRotationAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFab.setImageResource(R.drawable.ic_menu_add_custom);
            }
        });

        // Set up the AnimatorSets
        addRecipeAnimSet.playTogether(addRecipeXAnim, addRecipeYAnim, fabRotationAnim);
        addRecipeAnimSet.setInterpolator(interpolator);
        addRecipeAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(addRecipeAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(addRecipeAnimSet);
                }
            }
        });
        importRecipeAnimSet.playTogether(importRecipeXAnim, importRecipeYAnim);
        importRecipeAnimSet.setInterpolator(interpolator);
        importRecipeAnimSet.setStartDelay(100);
        importRecipeAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(importRecipeAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(importRecipeAnimSet);
                }
            }
        });
        textAnimSet.playSequentially(addTextXAnim, importTextXAnim, mainTextXAnim);
        textAnimSet.setInterpolator(interpolator);
        textAnimSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mAnimQueue.contains(textAnimSet) && !animQueueLock) {
                    mAnimQueue.remove(textAnimSet);
                }

                if (hideFab) {
                    mFab.hide();
                }
            }
        });

        // Remove any animations that have yet to be played
        if (!animQueueLock) {
            animQueueLock = true;
            for (AnimatorSet anim : mAnimQueue) {
                if (anim.equals(addRecipeAnimSet) || anim.equals(importRecipeAnimSet) || anim.equals(textAnimSet)) {
                    continue;
                }
                anim.cancel();
            }
            animQueueLock = false;
        }

        // Add AnimatorSets to mAnimQueue
        if (!animQueueLock) {
            mAnimQueue.add(addRecipeAnimSet);
            mAnimQueue.add(importRecipeAnimSet);
            mAnimQueue.add(textAnimSet);
        }

        // Play the animations
        addRecipeAnimSet.start();
        importRecipeAnimSet.start();
        textAnimSet.start();
        fabRotationAnim.start();

        // Set the boolean to false
        mFabMenuOpen = false;
    }

    /**
     * Shows a Dialog with an EditText to allow copy-pasting of a recipe URL so it can be imported
     */
    private void showImportDialog() {
        ImportRecipeDialog dialog = new ImportRecipeDialog();
        dialog.addImportRecipeDialogListener(new ImportRecipeDialog.ImportRecipeDialogListener() {
            @Override
            public void onDialogPositiveClick(String recipeUrl) {
                ActivityRecipeList.this.onDialogPositiveClick(recipeUrl);
            }
        });
        String IMPORT_DIALOG = "ImportRecipeDialog";
        dialog.show(getFragmentManager(), IMPORT_DIALOG);
    }

    @Override
    public void selectDrawerItem(MenuItem item) {
        super.selectDrawerItem(item);

        if (item.getItemId() == R.id.action_copy_db) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();

            int oldDeleted = prefs.getInt(getString(R.string.recipes_deleted_key), 0);

            Cursor cursor = getContentResolver().query(
                    RecipeContract.RecipeEntry.CONTENT_URI,
                    RecipeContract.RecipeEntry.RECIPE_PROJECTION,
                    null,
                    null,
                    RecipeContract.RecipeEntry.COLUMN_RECIPE_ID + " DESC"
            );

            cursor.moveToFirst();
            int lastId = cursor.getInt(RecipeContract.RecipeEntry.IDX_RECIPE_ID);
            int count = cursor.getCount();

            int deleted = lastId - count;

            Log.d(LOG_TAG, "Old deleted count: " + oldDeleted);
            Log.d(LOG_TAG, "New deleted count; " + deleted);

            editor.putInt(getString(R.string.recipes_deleted_key), deleted);
            editor.apply();
        }
        mDetailsVisible = false;
    }

    @Override
    public void onItemSelected(String recipeUrl, String imageUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        if (!mTwoPane) {
            // For phone UI, launch ActivityRecipeDetails
            startDetailsActivity(recipeUrl, imageUrl, viewHolder);
        } else {
            // For tablet UI inflate FragmentRecipeDetails into the container
            inflateDetailsFragment(recipeUrl, imageUrl, viewHolder);

            // Show the FragmentRecipeDetails in the master-flow view
            showDetailsContainer();

            mDetailsVisible = true;
        }
    }

    public void onDialogPositiveClick(String recipeUrl) {
        if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {

            RecipeImporter.importRecipeFromUrl(this, new RecipeImporter.UtilitySyncer() {
                @Override
                public void onFinishLoad() {
                    FragmentRecipeList recipeListFragment =
                            (FragmentRecipeList) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    recipeListFragment.mRecipeAdapter.notifyDataSetChanged();
                    recipeListFragment.mRecipeAdapter.setDetailCardPosition(0);
                }

                @Override
                public void onError() {
                    Toast.makeText(getParent(),
                            "Unable to identify the recipe on the website. \n Please use our \"Create recipe\" function to import the recipe.",
                            Toast.LENGTH_LONG)
                            .show();
                }
            }, recipeUrl);

        } else if (!mTwoPane) {
            // If in single-view mode, then start the ActivityRecipeDetails
            Intent intent = new Intent(this, ActivityRecipeDetails.class);
            intent.setData(Uri.parse(recipeUrl));
            intent.putExtra(FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_GENERIC, true);
            startActivity(intent);
        } else {
            // Inflate FragmentRecipeDetails
            inflateDetailsFragment(recipeUrl);

            // Show the FragmentRecipeDetails in the master-flow view
            showDetailsContainer();

            mDetailsVisible = true;

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

        mNavigationView.getMenu().getItem(0).setChecked(true);
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
                // Set the isConnect variable to true so the Activity doesn't register another
                // ConnectivityListener
                isConnected = true;

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
