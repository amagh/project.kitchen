package project.hnoct.kitchen.sync;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.Pair;
import android.util.Log;
import android.util.StringBuilderPrinter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/18/2017.
 */

public class GenericRecipeAsyncTask extends AsyncTask<Void, Void, Void> {
    // Constants
    private static final String LOG_TAG = GenericRecipeAsyncTask.class.getSimpleName();

    // Member Variables
    private Context mContext;
    private String mRecipeUrl;
    private Document mRecipeDocument;
    private Element mRecipeElement;
    private JSONObject mJsonRecipe;
    private boolean jsonRecipe = false;

    public GenericRecipeAsyncTask(Context context, String recipeUrl) {
        // Initialize member variables
        mContext = context;
        mRecipeUrl = recipeUrl;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            // Connect to the supplied URL and retrieve the HTML document
            mRecipeDocument = Jsoup.connect(mRecipeUrl).get();

            // Check if the page contains a JSON Object containing recipe information
            if ((mJsonRecipe = getRecipeJson()) != null) {
                jsonRecipe = true;
            }

            mRecipeElement = mRecipeDocument.select("[itemtype*=//schema.org/Recipe]").first();
            if (!jsonRecipe && mRecipeElement == null) {
                Log.d(LOG_TAG, "No recipe found at: " + mRecipeUrl);
                return null;
            }

            // Retrieve the recipe information from the Document
            String authority = Uri.parse(mRecipeUrl).getAuthority();
            String recipeId = Long.toString(Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE));
            String recipeTitle = jsonRecipe ? getJsonRecipeTitle() : getRecipeTitle();
            String description = jsonRecipe ? getJsonDescription() : getDescription();
            String imageUrl = jsonRecipe ? getJsonImageUrl() : getImageUrl();
            String author = jsonRecipe ? getJsonAuthor() : getRecipeAuthor();
            double rating = jsonRecipe ? -1 : getRating();
            int reviews = jsonRecipe ? -1 : getReviews();
            int servings = jsonRecipe ? getJsonYield() : getRecipeYield();

            if (rating == -1 && reviews == -1) {
                // Do not include reviews/ratings if there are none found
            }

            Log.d(LOG_TAG, "Recipe Source ID: " + recipeId);
            Log.d(LOG_TAG, "Recipe name: " + recipeTitle);

            if (description == null || description.isEmpty()) {
                StringBuilder  builder = new StringBuilder();
                for (String descriptionString : getAllDescription()) {
                    if (!descriptionString.trim().isEmpty()) {
                        builder.append(descriptionString)
                                .append("\n");
                    }
                }

                description = builder.toString().trim();
            }

            Log.d(LOG_TAG, "Description: " + description);
            Log.d(LOG_TAG, "Image URL: " + imageUrl);
            Log.d(LOG_TAG, "Author: " + author);
            Log.d(LOG_TAG, "Source: " + authority);
            Log.d(LOG_TAG, "Rating: " + rating);
            Log.d(LOG_TAG, "Reviews: " + reviews);
            Log.d(LOG_TAG, "Servings: " + servings);

            if (recipeTitle == null || imageUrl == null) {
                // Title and image are too important to not have for a recipe. If they are not
                // found, terminate the process
                return null;
            }

            if (author == null) {
                // If author is not found, then just set it to an empty String so the recipe can
                // still be added to the database
                author = "";
            }

            List<Pair<String, String>> ingredientQuantityPairList = jsonRecipe ? getJsonIngredients() : getIngredientsAndQuantities();

            for (Pair<String, String> ingredientQuantityPair : ingredientQuantityPairList) {
                Log.d(LOG_TAG, "Quantity: " + ingredientQuantityPair.second + " | Ingredient: " + ingredientQuantityPair.first);
            }

            if (ingredientQuantityPairList.isEmpty()) {
                // No ingredients, no recipe
                return null;
            }

            String directions = jsonRecipe ? getJsonDirections() : getRecipeDirections();

            if (directions == null || directions.isEmpty()) {
                // No directions, no recipe
                return null;
            }

            Log.d(LOG_TAG, "Directions: " + directions);

        } catch (IOException e) {

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Retrieve the author of the recipe from the document
     * @return String author
     */
    private String getRecipeTitle() {
        // Array of Elements that could possibly contain the recipe author's name
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[itemprop=name]").first(),
        };

        // Iterate through the Elements and attempt to retrieve author information
        for (Element recipeTitleElement : elementArray) {
            if (recipeTitleElement != null) {
                // If an Element is valid, remove the "by" tag and return the String
                return recipeTitleElement.text().replaceAll("[Bb][Yy] ", "");
            }
        }

        return null;
    }

    /**
     * Retrieve the description of the recipe from the Document
     * @return String of the description
     */
    private String getDescription() {
        // Array of Elements that could potentiall contain the recipe's description
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[name=description]").first()
        };
        // Iterate and find a valid Element containing the description
        for (Element descriptionElement : elementArray) {
            if (descriptionElement != null) {
                // Return the description
                return descriptionElement.attr("content");
            }
        }

        return null;
    }

    private List<String> getAllDescription() {
        StringBuilder builder = new StringBuilder();

        for (String line : mRecipeDocument.toString().split("\n")) {
            if (line.matches(".*//schema.org/[Rr]ecipe.*")) {
                break;
            }

            builder.append(line)
                    .append("\n");
        }

        Document partialDocument = Jsoup.parse(builder.toString());

        Elements descriptionElements = partialDocument.select("p");

        List<String> descriptionList = new ArrayList<>();

        for (Element descriptionElement : descriptionElements) {
            descriptionList.add(descriptionElement.text());
        }

        return descriptionList;
    }

    /**
     * Retrieve the recipe's image URL from the document
     * @return String of the URL for the recipe's image
     */
    private String getImageUrl() {
        // Array of Elements that could potentially contain the image URL
        String[] elementArray  = new String[] {
                mRecipeElement.select("[itemprop=image]").attr("href"),
                mRecipeElement.select("img[alt*=" + getRecipeTitle() + "]").attr("src"),
                mRecipeDocument.select("img[srcset").attr("src"),
                mRecipeDocument.select("img[src]").attr("src")
        };

        // See if any of the Elements returns a valid String for the image URL
        for (String imageUrl : elementArray) {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // If the String is valid, return it as the image URL
                return imageUrl;
            }
        }

        return null;
    }

    /**
     * Retrieves the author of the recipe from the document
     * @return String author
     */
    private String getRecipeAuthor() {
        // Array of Elements that could potentially contain the author
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[itemprop=author").first(),
                mRecipeDocument.select("[class=byline]").first(),
        };

        // Iterate through the Elements and check if any are valid
        for (Element authorElement : elementArray) {
            if (authorElement != null) {
                // Return the author if Element is valid
                String author;

                if (authorElement.text() != null && !authorElement.text().isEmpty()) {
                    author = authorElement.text();
                } else {
                    author = authorElement.attr("content");
                }
                return author;
            }
        }

        return null;
    }

    /**
     * Parse the Document and return a list of Pairs of ingredients with their quantities separated
     * @return List of Pairs with the first value being the ingredient and the second value being
     * the quantity
     */
    private List<Pair<String, String>> getIngredientsAndQuantities() {
        // Initialize the List that will contain the ingredient-quantity Pairs
        List<Pair<String, String>> ingredientQuantityPairList = new ArrayList<>();

        // Array of Elements that could potentially contain the ingredient information
        Elements[] elementsArray = new Elements[] {
                mRecipeElement.select("div[class*=ngredient]").select("ul").select("[itemprop*=ingredients]"),
                mRecipeElement.select("div[class*=content]").select("ul").select("[itemprop*=ingredients]"),
                mRecipeElement.select("div[class*=list").select("[itemprop*=ingredients]"),
                mRecipeElement.select("ul[class*=list],ul[id*=list]").select("p,li,div")
        };

        // Iterate through the Array of Elements and check for validity of the Elements
        for (Elements ingredientElements : elementsArray) {
            if (ingredientElements != null && ingredientElements.size() > 0) {
                // If the Element is valid, iterate through and retrieve the ingredient information
                for (Element ingredientElement : ingredientElements) {
                    // Convert the list item to String
                    String ingredientQuantity = ingredientElement.text();

                    if (ingredientQuantity.trim().isEmpty()) {
                        // If the Element is just used as spacing, it can be skipped
                        continue;
                    }

                    // Break the String into Pairs with quantity separated
                    Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);

                    // Add the Pair to the List
                    ingredientQuantityPairList.add(ingredientQuantityPair);
                }
                break;
            }
        }

        return ingredientQuantityPairList;
    }

    /**
     * Parses the Document to returns a String containing the directions for a recipe within it
     * @return String of directions, separated by a new line (\n)
     */
    private String getRecipeDirections() {
        // Array of Elements that could potentially contain the directions
        Elements[] elementsArray = new Elements[] {
                mRecipeElement.select("div[class*=irection]").select("ol").select("li"),
                mRecipeElement.select("div[class*=nstruction]").select("ol").select("li"),
                mRecipeElement.select("div[class*=content").select("ol").select("li"),
                mRecipeElement.select("div[class*=nstruction]").select("p"),
                mRecipeElement.select("div[itemprop*=nstruction").select("ol").select("li"),
                mRecipeElement.select("div[itemprop*=nstruction").select("p"),
                mRecipeElement.select("ol[id*=nstruction],ol[id=irection]").select("li,div")
        };

        // Initialize the StringBuilder that will be used to link the direction list items into a
        // single String separated by new lines
        StringBuilder builder = new StringBuilder();

        // Iterate through the Elements and attempt to find valid Elements containing directions
        for (Elements directionElements : elementsArray) {
            if (directionElements != null && directionElements.size() > 0) {
                // If found, iterate through the Elements and retrieve the direction information
                for (Element directionElement : directionElements) {
                    // Append the direction onto the StringBuilder and a new line so it can be easily
                    // separated at a later time
                    if (directionElement.text() != null && !directionElement.text().isEmpty()) {
                        builder.append(directionElement.text().replaceAll("^([Ss]tep)? ?[1-9]\\.?:?", "").trim())
                                .append("\n");
                    }
                }
                break;
            }
        }

        String directions = builder.toString().trim();

        // Check that directions is not empty before returning it
        if (directions.isEmpty()) {
            return null;
        } else {
            return directions;
        }
    }

    /**
     * Parses the Document for the number of reviews for a recipe
     * @return Integer number of reviews for the recipe
     */
    private int getReviews() {
        // Array of Elements that could potentially contain the review information
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[itemprop=ratingCount").first()
        };

        // Iterate through Elements and attempt to find review information
        for (Element reviewElement : elementArray) {
            if (reviewElement != null) {
                // Convert the String to Integer and return the value
                return Integer.parseInt(reviewElement.text());
            }
        }

        return -1;
    }

    /**
     * Parses the Document for the rating of the recipe
     * @return Double value of the rating of the recipe
     */
    private double getRating() {
        // Array of Elements that could potentially contain the rating information
        Element[] elementArray = new Element[] {
                mRecipeElement.select("[itemprop=ratingValue]").first()
        };

        // Iterate and attemp to find rating information
        for (Element ratingElement : elementArray) {
            if (ratingElement != null) {
                // Return the rating as a Double
                return Double.parseDouble(ratingElement.text());
            }
        }

        return -1;
    }

    /**
     * Parse the Document and retrieve information about the number of servings the recipe
     * provides
     * @return Integer number of servings per recipe
     */
    private int getRecipeYield() {
        // Array of Elements that could potentially contain serving information
        Element[] elementArray = new Element[] {
                mRecipeDocument.select("[itemprop=recipeYield").first()
        };

        // Iterate and attempt to find serving information
        for (Element yieldElement : elementArray) {
            if (yieldElement != null) {
                // Retrieve the yield as a String
                String yield = yieldElement.text();

//                Log.d(LOG_TAG, "URL: " + mRecipeUrl);
//                Log.d(LOG_TAG, "Yield: " + yield);

                // Strip all words and upper limits for the serving information out
                // e.g. "2-4 servings" will remove "-4 servings"
                Pattern pattern = Pattern.compile("(\\d+) ?.*");
                Matcher match = pattern.matcher(yield);
                if (match.find()) {
                    return Integer.parseInt(match.group(1));
                }
                return Integer.parseInt(yield.replaceAll("(-\\d)?", "").replaceAll("\\D*", ""));
            }
        }

        return -1;
    }

    /**
     * Parses the Document and attempts to find a recipe contained as a JSON Object
     * @return JSON Object containing recipe information
     * @throws JSONException Error if the String parsed to JSON Object is not a valid JSON Object
     */
    private JSONObject getRecipeJson() throws JSONException {
        // Select all Elements with a script tag
        Elements scriptElements = mRecipeDocument.select("script").select("[type*=json]");

        // Iterate through and find the JSON Object
        for (Element scriptElement : scriptElements) {
            String line = scriptElement.toString();
            // If a script is found containing a JSON Object, initialize a Stack to keep track
            // of the number open and close brackets (i.e. { & })
            Stack<Character> bracketStack = new Stack<>();

            // Initialize the variables that will keep track of where the JSON Object starts and
            // ends
            int jsonStart = 0;
            int jsonEnd = 0;

            // Iterate through each character in the line to find each open and close bracket
            for (int i = 0; i < line.length(); i++) {
                // Initialize the char
                char c = line.charAt(i);

                // Check if the char is either an open or close bracket
                if (c == '{') {
                    if (jsonStart == 0) {
                        // First occurrence of the open bracket is set as the start of the JSON
                        // Object
                        jsonStart = i;
                    }

                    // Push the open bracket onto the Stack
                    bracketStack.push('{');
                } else if (c == '}') {
                    // Closed brackets pop the Stack
                    bracketStack.pop();
                    if (bracketStack.size() == 0) {
                        // When the final open bracket is popped off the Stack, the JSON Object
                        // is complete. Set the end of the JSON Object to be the next char index
                        // to include the last bracket
                        jsonEnd = i + 1;
                    }
                }
            }

            if (!line.matches("\"@type\":\"Recipe\"")) {
                return null;
            }

            return new JSONObject(line.substring(jsonStart, jsonEnd));
        }

        return null;
    }

    private String getJsonRecipeTitle() throws JSONException {
        String JSON_RECIPE_NAME = "name";
        return mJsonRecipe.getString(JSON_RECIPE_NAME);
    }

    private String getJsonAuthor() throws JSONException {
        String JSON_AUTHOR_OBJ = "author";
        String JSON_AUTHOR = "name";

        JSONObject jsonAuthor = mJsonRecipe.getJSONObject(JSON_AUTHOR_OBJ);
        return jsonAuthor.getString(JSON_AUTHOR);
    }

    private String getJsonImageUrl() throws JSONException {
        String JSON_IMAGE = "image";
        return mJsonRecipe.getString(JSON_IMAGE);
    }

    private String getJsonDescription() throws JSONException {
        String JSON_DESCRIPTION = "description";
        return mJsonRecipe.getString(JSON_DESCRIPTION);
    }

    private int getJsonYield() throws JSONException {
        String JSON_YIELD = "recipeYield";

        // Replace any written words with their number equivalent
        String yield = mJsonRecipe.getString(JSON_YIELD);
        if (yield.matches(".*[0-9]+.*")) {
            return Integer.parseInt(yield.replaceAll("(-\\d)?( \\w*)", ""));
        } else {
            yield = yield.replaceAll("[Oo]ne", "1");
            yield = yield.replaceAll("[Tt]wo", "2");
            yield = yield.replaceAll("[Tt]hree", "3");
            yield = yield.replaceAll("[Ff]our", "4");
            yield = yield.replaceAll("[Ff]ive", "5");
            yield = yield.replaceAll("[Ss]ix", "6");
            yield = yield.replaceAll("[Ss]even", "7");
            yield = yield.replaceAll("[Ee]ight", "8");
            yield = yield.replaceAll("[Nn]ine", "9");

            Pattern pattern = Pattern.compile("(\\d+) ?.*");
            Matcher match = pattern.matcher(yield);

            if (match.find()) {
                return Integer.parseInt(match.group(1));
            }

            return Integer.parseInt(yield.replaceAll("(-\\d)?", "").replaceAll("\\D*", ""));
        }
    }

    private List<Pair<String, String>> getJsonIngredients() throws JSONException {
        String JSON_INGREDIENT_ARRAY = "recipeIngredient";

        List<Pair<String, String>> ingredientQuantityPairList = new ArrayList<>();

        JSONArray jsonIngredientArray = mJsonRecipe.getJSONArray(JSON_INGREDIENT_ARRAY);

        for (int i = 0; i < jsonIngredientArray.length(); i++) {
            String ingredientQuantity = jsonIngredientArray.getString(i);

            Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientQuantity);

            ingredientQuantityPairList.add(ingredientQuantityPair);
        }

        return ingredientQuantityPairList;
    }

    private String getJsonDirections() throws JSONException {
        String JSON_DIRECTIONS_ARRAY = "recipeInstructions";

        StringBuilder builder = new StringBuilder();

        JSONArray jsonDirectionsArray = mJsonRecipe.getJSONArray(JSON_DIRECTIONS_ARRAY);

        for (int i = 0; i < jsonDirectionsArray.length(); i++) {
            String direction = jsonDirectionsArray.getString(i);

            builder.append(direction.replaceAll("^([Ss]tep)? ?[1-9]\\.?:?", "").trim())
                    .append("\n");
        }

        String directions = builder.toString().trim();

        // Check that directions aren't empty before returning the value
        if (directions.isEmpty()) {
            return null;
        } else {
            return builder.toString().trim();
        }
    }
}
