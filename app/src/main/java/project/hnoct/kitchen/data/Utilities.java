package project.hnoct.kitchen.data;

import android.net.Uri;
import android.os.Build;
import android.support.v4.util.Pair;

import com.annimon.stream.Stream;

import project.hnoct.kitchen.data.RecipeContract.*;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hnoct on 2/20/2017.
 */

public class Utilities {
    /** Constants **/

    /** Member Variables **/

    /**
     *
     * @param reviews
     * @return
     */
    public static String formatReviews(long reviews) {
        // Append 'review(s)' to the number of reviews
        if (reviews > 1) {
            return String.format("%s reviews", reviews);
        } else {
            return String.format("%s review", reviews);
        }
    }

    /**
     *
     * @param rating
     * @return
     */
    public static String formatRating(double rating) {
        // Truncate rating to a single decimal point
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(rating);
    }

    /**
     * Helper method for splitting quantity and ingredient from a single String that contains both
     * @param ingredientAndQuantity String containing quantity and a single ingredient
     * @return Pair with ingredient as First and quantity as Second
     */
    public static Pair<String,String> getIngredientQuantity(String ingredientAndQuantity) {
        // Retrieve array of measurements from RecipeContract
        String[] measurements = IngredientEntry.quantities;

        for (String measurement : measurements) {
            // Check to see if any measurement is used in the input String
            if (ingredientAndQuantity.contains(measurement)) {
                // If found, split the String in two so that the quantity and ingredient are separated

                // Make sure the measurement isn't just part of an ingredient
                // e.g. 'can' in 'American cheese'
                if (ingredientAndQuantity.charAt(ingredientAndQuantity.indexOf(measurement) - 1) != ' ') {
                    continue;
                }

                // Get the last index of the measurement in the input String
                int lastCharIdx = ingredientAndQuantity.indexOf(measurement) + measurement.length();

                if (ingredientAndQuantity.charAt(lastCharIdx) == 's') {
                    // Check to see if next character is an 's'. If so, include it
                    lastCharIdx++;
                }

                // Create two strings as substrings of the inputString
                String ingredientQuantity = ingredientAndQuantity.substring(0, lastCharIdx).trim();
                String ingredient = ingredientAndQuantity.substring(lastCharIdx).trim();

                // Return the Strings as a Pair
                return new Pair<>(ingredient, ingredientQuantity);
            }
        }
        // If the input String does not contain any known measurements, then use Regex to
        // obtain the quantity of the ingredient
        Pattern pattern = Pattern.compile("([1-9]* *[1-9]*\\/*[1-9])(.*)");
        Matcher matcher = pattern.matcher(ingredientAndQuantity);

        if (matcher.find()) {
            // If quantity is found, create new Strings from the groups matched
            String ingredientQuantity = matcher.group(1).trim();
            String ingredient = matcher.group(2).trim();
            return new Pair<>(ingredient, ingredientQuantity);
        } else {
            // If no quantity is found, then ingredient is informal measurement
            // e.g. dash of paprika, salt and pepper to taste, etc.
            // Return an empty String as the quantity
            return new Pair<>(ingredientAndQuantity, "");
        }
    }

    /**
     * Helper method for obtaining recipeId from the recipe URL
     * @param recipeUrl URL for the recipe
     * @return recipeId as long
     */
    public static long getRecipeIdFromAllRecipesUrl(String recipeUrl) {
        // Convert URL to URI
        Uri recipeLinkUri = Uri.parse(recipeUrl);

        // Return 3rd segment of the URI as the recipeId
        return Long.parseLong(recipeLinkUri.getPathSegments().get(2));
    }

    /**
     * Converts thumbnail URL to higher resolution image URL
     * @param thumbnailUrl URL of the thumbnail as String
     * @return URL of the image as String
     */
    public static String getImageUrlFromThumbnailUrl(String thumbnailUrl) {
        /** Constants **/
        final String ALL_RECIPES_IMAGE_URL_BASE = "http://images.media-allrecipes.com/userphotos/560x315/";

        // Convert URL to URI
        Uri thumbnailUri = Uri.parse(thumbnailUrl);

        // The last segment of the URI does not change based on photo size, so it is retrieved and
        // appended to the image URL base
        String photoId = thumbnailUri.getLastPathSegment();

        // Append the photoId to the image URL's base and return the result
        return Uri.parse(ALL_RECIPES_IMAGE_URL_BASE).buildUpon()
                .appendPath(photoId)
                .toString();
    }
}
