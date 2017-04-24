package project.hnoct.kitchen.ui;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

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
import project.hnoct.kitchen.sync.AllRecipesListAsyncTask;
import project.hnoct.kitchen.sync.FoodDotComListAsyncTask;
import project.hnoct.kitchen.sync.RecipeImporter;
import project.hnoct.kitchen.sync.SeriousEatsListAsyncTask;

public class RecipeListActivity extends AppCompatActivity implements RecipeListFragment.RecipeCallBack, ImportRecipeDialog.ImportRecipeDialogListener {
    /** Constants **/
    private static final String LOG_TAG = RecipeListActivity.class.getSimpleName();
    private final String DETAILS_FRAGMENT = "DFTAG";

    /** Member Variables **/
    private static boolean mFabMenuOpen;
    public static boolean mTwoPane = false;
    public static boolean mDetailsVisible = false;
    private static int mPosition;
    private SearchListener mSearchListener;

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
    @Nullable @BindView(R.id.recipe_detail_container) FrameLayout mDetailsContainer;
    @Nullable @BindView(R.id.detail_fragment_container) RelativeLayout mContainer;
    @Nullable @BindView(R.id.temp_button) ImageView mTempButton;
    @Nullable @BindView(R.id.searchview) EditText mSearchView;
    @Nullable @BindView(R.id.search_icon) ImageView mSearchIcon;
    @Nullable @BindView(R.id.app_title) TextView mTitle;

    @Optional
    @OnClick(R.id.temp_button)
    public void closePreview() {
        mDetailsVisible = false;
        mContainer.setVisibility(View.GONE);
        RecipeListFragment fragment = (RecipeListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        fragment.setLayoutColumns();
//        fragment.mRecipeRecyclerView.scrollToPosition(mPosition);
    }

    @Optional
    @OnEditorAction(R.id.searchview)
    boolean onEditorAction(int actionId) {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            String searchTerm = mSearchView != null ? mSearchView.getText().toString() : null;
            if (mSearchListener != null && searchTerm != null) mSearchListener.onSearch(searchTerm);

            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            return true;
        }
        return false;
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

    void showSearch() {
        mSearchView.setVisibility(View.VISIBLE);
        mSearchView.requestFocus();
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_close_clear_cancel));
        mTitle.setVisibility(View.GONE);

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this.getCurrentFocus(), 0);
    }

    void hideSearch() {
        mSearchView.setVisibility(View.GONE);
        mSearchIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_menu_search));
        mTitle.setVisibility(View.VISIBLE);
        if (mSearchListener != null) {
            mSearchView.setText("");
            mSearchListener.onSearch("");
        }

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
        startActivity(new Intent(this, CreateRecipeActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

                // Set the visibility of the container to GONE to allow the RecipeListFragment
                // to take the full width of the view
                mContainer.setVisibility(View.GONE);

                // Create a new RecipeDetailsFragment and load it into the container
                RecipeDetailsFragment fragment = new RecipeDetailsFragment();
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                        .commit();
            }
        }

        long seedTime = Utilities.getCurrentTime();

        AllRecipesListAsyncTask allRecipesAsyncTask = new AllRecipesListAsyncTask(this, seedTime);
        allRecipesAsyncTask.execute();

        FoodDotComListAsyncTask foodAsyncTask = new FoodDotComListAsyncTask(this, seedTime);
        foodAsyncTask.execute();

        SeriousEatsListAsyncTask seriouseatsAsyncTask = new SeriousEatsListAsyncTask(this, seedTime);
        seriouseatsAsyncTask.execute();

//        temp();
    }

    boolean loaded = false;
    private void temp() {

//        final WebView browser = new WebView(this);
//        browser.getSettings().setJavaScriptEnabled(true);
//        browser.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");
//        browser.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                if (!loaded) {
//                    browser.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
//                    loaded = true;
//                }
//            }
//        });
//
//        browser.loadUrl("http://www.food.com/recipe/all/popular");

        final WebView browser = new WebView(this);
/* JavaScript must be enabled if you want it to work, obviously */
        browser.getSettings().setJavaScriptEnabled(true);

/* Register a new JavaScript interface called HTMLOUT */
        browser.addJavascriptInterface(new JavaScriptInterface(), "HTMLOUT");

/* WebViewClient must be set BEFORE calling loadUrl! */
        browser.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
        /* This call inject JavaScript into the page which just finished loading. */
                browser.loadUrl("javascript:HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
            }
        });

/* load a web page */
        browser.loadUrl("http://www.food.com/recipe/all/popular");
    }

    class JavaScriptInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            InputStream stream = new ByteArrayInputStream(html.getBytes(Charset.forName("UTF-8")));
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(LOG_TAG, line);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(LOG_TAG, html);
        }
    }

    private void selectDrawerItem(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favorites: {
                startActivity(new Intent(this, FavoritesActivity.class));
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
                hideNavigationDrawer();
                break;
            }
            case R.id.action_my_recipe_books: {
                hideNavigationDrawer();
                Intent intent = new Intent(this, RecipeBookActivity.class);
                startActivity(intent);
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
    public void onItemSelected(String recipeUrl, RecipeAdapter.RecipeViewHolder viewHolder) {
        if (!mTwoPane) {
            // If in single-view mode, then start the RecipeDetailsActivity
            Intent intent = new Intent(this, RecipeDetailsActivity.class);
            intent.setData(Uri.parse(recipeUrl));
            startActivity(intent);
        } else {
            mDetailsVisible = true;

            // Create a new RecipeDetailsFragment
            RecipeDetailsFragment fragment = new RecipeDetailsFragment();

            // Create the Bundle and add the recipe's URL to it and set it as the argument for the
            // fragment
            Bundle args = new Bundle();
            args.putParcelable(RecipeDetailsFragment.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            fragment.setArguments(args);

            // Replace the existing RecipeDetailsFragment with the newly created one
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                    .commit();

            // Show the RecipeDetailsFragment in the master-flow view
            mContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String recipeUrl) {
        if (getResources().getBoolean(R.bool.recipeAdapterUseDetailView)) {

            RecipeImporter.importRecipeFromUrl(this, new RecipeImporter.UtilitySyncer() {
                @Override
                public void onFinishLoad() {
                    RecipeListFragment recipeListFragment =
                            (RecipeListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
                    recipeListFragment.mRecipeAdapter.notifyDataSetChanged();
                    recipeListFragment.mRecipeAdapter.setDetailCardPosition(0);
                }
            }, recipeUrl);

        } else if (!mTwoPane) {
            // If in single-view mode, then start the RecipeDetailsActivity
            Intent intent = new Intent(this, RecipeDetailsActivity.class);
            intent.setData(Uri.parse(recipeUrl));
            startActivity(intent);
        } else {
            mDetailsVisible = true;

            // Create a new RecipeDetailsFragment
            RecipeDetailsFragment fragment = new RecipeDetailsFragment();

            // Create the Bundle and add the recipe's URL to it and set it as the argument for the
            // fragment
            Bundle args = new Bundle();
            args.putParcelable(RecipeDetailsFragment.RECIPE_DETAILS_URL, Uri.parse(recipeUrl));
            fragment.setArguments(args);

            // Replace the existing RecipeDetailsFragment with the newly created one
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.recipe_detail_container, fragment, DETAILS_FRAGMENT)
                    .commit();

            // Show the RecipeDetailsFragment in the master-flow view
            mContainer.setVisibility(View.VISIBLE);

            RecipeListFragment recipeListFragment = (RecipeListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
            recipeListFragment.setLayoutColumns();
        }

    }

    interface SearchListener {
        void onSearch(String searchTerm);
    }

    public void setSearchListener(SearchListener listener) {
        mSearchListener = listener;
    }

}
