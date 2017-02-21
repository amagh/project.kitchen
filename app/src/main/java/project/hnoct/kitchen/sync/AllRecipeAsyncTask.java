package project.hnoct.kitchen.sync;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

/**
 * Created by hnoct on 2/20/2017.
 */

public class AllRecipeAsyncTask extends AsyncTask<String, Void, Void> {

    @Override
    protected Void doInBackground(String... params) {
        String recipeUrl = params[0];
        try {
            Document recipeDoc = Jsoup.connect(recipeUrl).get();
            /**
             * Things to get:
             * Directions
             * ImageURL
             * Ingredients
             */
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
