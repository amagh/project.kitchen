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

    public static void importRecipeFromUrl(Context context, final UtilitySyncer syncer, String recipeUrl) {
        Log.d(LOG_TAG, "URL: " + recipeUrl);
        UriMatcher matcher = Utilities.buildUriMatcher(context);
        Uri recipeUri = Uri.parse(recipeUrl);

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
