package project.hnoct.kitchen.sync;

import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

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

        // Match the URI and launch the correct AsyncTask
        switch (matcher.match(recipeUri)) {
            case Utilities.ALLRECIPES_URI: {
                AllRecipesAsyncTask syncTask = new AllRecipesAsyncTask(context, new AllRecipesAsyncTask.RecipeSyncCallback() {
                    @Override
                    public void onFinishLoad() {
                        syncer.onFinishLoad();
                    }
                });
                syncTask.execute(recipeUrl, Long.toString(Utilities.getRecipeIdFromUrl(context, recipeUrl)));
                return;
            }
            default: throw new UnsupportedOperationException("Unknown URL: " + recipeUrl);
        }
    }

    public interface UtilitySyncer {
        void onFinishLoad();
    }
}
