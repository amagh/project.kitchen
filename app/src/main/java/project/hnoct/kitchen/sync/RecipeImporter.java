package project.hnoct.kitchen.sync;

import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 3/5/2017.
 */

public class RecipeImporter {
    /** Constants **/
    private static final String LOG_TAG = RecipeImporter.class.getSimpleName();

    /**
     * Detect and start the AsyncTask to import the recipe given a URL
     * @param context Interface to global Context
     * @param syncer Callback interface
     * @param recipeUrl String URL to the recipe to be imported
     */
    public static void importRecipeFromUrl(Context context, final UtilitySyncer syncer, String recipeUrl) {
        // Build the UriMatcher
        /** @see Utilities#buildUriMatcher **/
        UriMatcher matcher = Utilities.buildUriMatcher(context);

        // Parse the URL to a URI
        Uri recipeUri = Uri.parse(recipeUrl);

        // Check to make sure scheme is properly set
        String recipeScheme = recipeUri.getScheme();
        Log.d(LOG_TAG, "Scheme: " + recipeScheme);

        if (recipeScheme == null) {
            // Add scheme if missing
            recipeUri = Uri.parse(context.getString(R.string.http_scheme) +
                    "://" +
                    recipeUri.toString());

        }

        AsyncTask syncTask;

        // Match the URI and launch the correct AsyncTask
        switch (matcher.match(recipeUri)) {
            case Utilities.ALLRECIPES_URI: {
                syncTask = new AllRecipesAsyncTask(context, new AllRecipesAsyncTask.RecipeSyncCallback() {
                    @Override
                    public void onFinishLoad() {
                        syncer.onFinishLoad();
                    }
                });
                break;
            }

            case Utilities.FOOD_URI: {
                syncTask = new FoodDotComAsyncTask(context, new AllRecipesAsyncTask.RecipeSyncCallback() {
                    @Override
                    public void onFinishLoad() {
                        syncer.onFinishLoad();
                    }
                });
                break;
            }

            case Utilities.SERIOUSEATS_URI: {
                syncTask = new SeriousEatsAsyncTask(context, new AllRecipesAsyncTask.RecipeSyncCallback() {
                    @Override
                    public void onFinishLoad() {
                        syncer.onFinishLoad();
                    }
                });
                break;
            }

            case Utilities.EPICURIOUS_URI: {
                syncTask = new EpicuriousAsyncTask(context, new AllRecipesAsyncTask.RecipeSyncCallback() {
                    @Override
                    public void onFinishLoad() {
                        syncer.onFinishLoad();
                    }
                });
                break;
            }
            default: throw new UnsupportedOperationException("Unknown URL: " + recipeUrl);
        }

        syncTask.execute(recipeUri.toString());
    }

    public interface UtilitySyncer {
        void onFinishLoad();
    }
}
