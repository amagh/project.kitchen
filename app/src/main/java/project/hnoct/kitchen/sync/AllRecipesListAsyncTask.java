package project.hnoct.kitchen.sync;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

/**
 * Created by hnoct on 2/15/2017.
 */

public class AllRecipesListAsyncTask extends AsyncTask<Void, Void, Void> {
    // Constants
    final String ALL_RECIPES_BASE_URL = "http://www.allrecipes.com";
    private final String LOG_TAG = AllRecipesListAsyncTask.class.getSimpleName();

    // Member Variables


    @Override
    protected Void doInBackground(Void... params) {
        try {
            // Connect and downloading the HTML document
            Document document = Jsoup.connect(ALL_RECIPES_BASE_URL).get();

            // Select the elements from the document to add to the database as part of the recipe
            Elements recipes = document.getElementsByClass("grid-col--fixed-tiles");
            for (Element recipe : recipes) {
                // Retrieve the href for the recipe
                Element recipeLinkElement = recipe.getElementsByTag("a").first();
                if (recipeLinkElement == null) {
                    // Some advertisements do not contain an href and should be skipped
                    continue;
                }
                String recipeLink = recipeLinkElement.attr("href");
                if (!recipeLink.contains("recipe")) {
                    // Skip any links that don't direct to a recipe
                    continue;
                }

                // Get the recipe Id by converting the link to a URI and selecting the 2nd segment
                Uri recipeUri = Uri.parse(recipeLink);
                int recipeId = Integer.parseInt(recipeUri.getPathSegments().get(1));

                // Retrieve the recipe name, thumbnail URL, and description
                Element recipeElement = recipe.getElementsByClass("grid-col__rec-image").first();
                if (recipeElement == null) {
                    // Advertisements do not contain this element, so they can be skipped if found
                    continue;
                }

                // Replace some elements of the recipe title to get the recipe name
                String recipeTitle = recipeElement.attr("title");
                String recipeName = recipeTitle.replace(" Recipe", "").replace(" and Video", "");

                String recipeThumbnailUrl = recipeElement.attr("data-original-src");

                // Recipe description contains name of recipe, so it is removed
                String recipeDescription = recipeElement.attr("alt");
                recipeDescription = recipeDescription.substring(recipeTitle.length() + 3);

                // Retrieve the rating
                Element ratingElement = recipe.getElementsByClass("rating-stars").first();
                if (ratingElement == null) {
                    // Second check for advertisements as they do not contain ratings
                    continue;
                }
                double rating = Double.parseDouble(ratingElement.attr("data-ratingstars"));

                // Retrieve the number of reviews
                Element reviewElement = recipe.getElementsByTag("format-large-number").first();
                long reviews = Long.parseLong(reviewElement.attr("number"));
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(NullPointerException e) {
            Log.d(LOG_TAG, "Error parsing document", e);
            e.printStackTrace();
        }

        return null;
    }
}
