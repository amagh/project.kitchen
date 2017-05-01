package project.hnoct.kitchen.ui;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
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
import project.hnoct.kitchen.search.FoodDotComSearchAsyncTask;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentSearch extends Fragment {
    // Constants

    // MemberVariables
    private Context mContext;
    private String mSearchTerm;

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

            }
        });
//        allrecipesSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mSearchTerm);

        setupWebView();
        mWebView.loadUrl(generateFoodUrl(mSearchTerm));
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
}
