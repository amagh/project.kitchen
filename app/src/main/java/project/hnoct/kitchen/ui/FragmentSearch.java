package project.hnoct.kitchen.ui;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.search.AllRecipesSearchAsyncTask;
import project.hnoct.kitchen.search.EpicuriousSearchAsyncTask;
import project.hnoct.kitchen.search.FoodDotComSearchAsyncTask;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.view.StaggeredGridLayoutManagerWithSmoothScroll;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentSearch extends Fragment {
    // Constants

    // Member Variables
    private Context mContext;
    private String mSearchTerm;
    private AdapterRecipe mAdapter;
    private StaggeredGridLayoutManagerWithSmoothScroll mStaggeredLayoutManager;
    private int mPosition;

    // Views bound by ButterKnife
    @BindView(R.id.search_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.search_webview) WebView mWebView;

    public FragmentSearch() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_activity_search, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        // Retrieve arguments passed from ActivitySearch
        Bundle args = getArguments();
        if (args != null) {
            mSearchTerm = args.getString(ActivitySearch.SEARCH_TERM);
        }

        // Initialize and pass the searchTerm to the search AsyncTasks
        AllRecipesSearchAsyncTask allrecipesSearchTask = new AllRecipesSearchAsyncTask(mContext, new AllRecipesSearchAsyncTask.SyncListener() {
            @Override
            public void onFinishLoad(List<Map<String, Object>> recipeList) {
                mAdapter.addList(recipeList);
            }
        });
        allrecipesSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSearchTerm);

        EpicuriousSearchAsyncTask epicuriousSearchTask = new EpicuriousSearchAsyncTask(mContext, new EpicuriousSearchAsyncTask.SyncListener() {
            @Override
            public void onFinishLoad(List<Map<String, Object>> recipeList) {
                mAdapter.addList(recipeList);
            }
        });
        epicuriousSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSearchTerm);

        setupWebView();
        mWebView.loadUrl(generateFoodUrl(mSearchTerm));

        mAdapter = new AdapterRecipe(mContext, new AdapterRecipe.RecipeAdapterOnClickHandler() {
            @Override
            public void onClick(long recipeId, AdapterRecipe.RecipeViewHolder viewHolder) {
                mPosition = viewHolder.getAdapterPosition();
            }
        });

        mRecyclerView.setAdapter(mAdapter);

        setLayoutColumns();



        return rootView;
    }

    private void setupWebView() {
        // Set up the WebView to enable JavaScript and other settings that will allow a JavaScript-
        // generated HTML document to be donwloaded
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        mWebView.setScrollbarFadingEnabled(false);

        // Register a new JavaScriptInterface called "HTMLOUT"
        mWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url)
            {
                // Inject JavaScript into the page that has finished loading
                mWebView.loadUrl("javascript:HTMLOUT.processHTML(document.documentElement.outerHTML);");
            }
        });
    }

    private class MyJavaScriptInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            FoodDotComSearchAsyncTask foodSearchAsyncTask = new FoodDotComSearchAsyncTask(mContext,
                    new FoodDotComSearchAsyncTask.SyncListener() {
                        @Override
                        public void onFinishLoad(List<Map<String, Object>> recipeList) {
                            mAdapter.addList(recipeList);
                        }
                    });
            foodSearchAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, html);
        }
    }

    private String generateFoodUrl(String searchTerm) {
        String FOOD_BASE_URL = "http://www.food.com";
        String FOOD_SEARCH_PATH = "search";
        String FOOD_PAGE_QUERY_PARAM = "pn";

        Uri foodSearchUri = Uri.parse(FOOD_BASE_URL)
                .buildUpon()
                .appendPath(FOOD_SEARCH_PATH)
                .appendPath(searchTerm)
                .build();

        return foodSearchUri.toString();
    }

    /**
     * Sets the number columns used by the StaggeredGridLayoutManager
     */
    void setLayoutColumns() {
        // Retrieve the number of columns needed by the device/orientation
        int columns;
        if (ActivityRecipeList.mTwoPane && ActivityRecipeList.mDetailsVisible) {
            columns = getResources().getInteger(R.integer.recipe_twopane_columns);
        } else {
            columns = getResources().getInteger(R.integer.recipe_columns);
        }

        if (mRecyclerView.getLayoutManager() == null) {
            // Instantiate the LayoutManager
            mStaggeredLayoutManager = new StaggeredGridLayoutManagerWithSmoothScroll(
                    columns,
                    StaggeredGridLayoutManager.VERTICAL
            );

            // Set the LayoutManager for the RecyclerView
            mRecyclerView.setLayoutManager(mStaggeredLayoutManager);

        } else {
//            mStaggeredLayoutManager =
//                    (StaggeredGridLayoutManagerWithSmoothScroll) mRecipeRecyclerView
//                            .getLayoutManager();
            mStaggeredLayoutManager.setSpanCount(columns);
        }


        AdapterRecipe adapter = ((AdapterRecipe) mRecyclerView.getAdapter());
        if (adapter != null) {
//            adapter.hideDetails();
        }

        // Scroll to the position of the recipe last clicked due to change in visibility of the
        // Detailed View in Master-Flow layout
        if (ActivityRecipeList.mTwoPane) {
            mRecyclerView.smoothScrollToPosition(mPosition);
        }
    }
}
