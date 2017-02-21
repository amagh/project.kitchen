package project.hnoct.kitchen.data;

import java.text.DecimalFormat;
import java.util.Locale;

/**
 * Created by hnoct on 2/20/2017.
 */

public class Utilities {
    /** Constants **/

    /** Member Variables **/

    public static String formatReviews(long reviews) {
        if (reviews > 1) {
            return String.format("%s reviews", reviews);
        } else {
            return String.format("%s review", reviews);
        }
    }

    public static String formatRating(double rating) {
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(rating);
    }
}
