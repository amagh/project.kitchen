package project.hnoct.kitchen.data;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 5/19/2017.
 */

public class RecipeHelper {
    // Member Variables
    // Recipe Columns
    private int recipeId;
    private String recipeSourceId;
    private String recipeName;
    private String recipeAuthor;
    private String imageUrl;
    private String recipeUrl;
    private String description;
    private double rating = -1;
    private int reviews = -1;
    private String directions;
    private long dateAdded;
    private boolean favorite = false;
    private String source;
    private double calories = -1;
    private double fat = -1;
    private double carbs = -1;
    private double protein = -1;
    private double cholesterol = -1;
    private double sodium = -1;
    private int servings = -1;

    private boolean isCustomRecipe = false;

    // Link Column
    private int ingredientOrder = 0;

    private static RecipeHelper mRecipeHelper = null;
    private static IngredientHelper mIngredientHelper = null;

    // Lists that will hold the values that are to be inserted into the database
    List<ContentValues> mRecipeCVList = null;
    List<ContentValues> mIngredientCVList = null;
    List<ContentValues> mLinkCVList = null;


    private static Context mContext;
    private static Object mObjectLock;

    /**
     * Initializes the RecipeHelper if it is not yet initialized and then returns the instance
     * if the lock matches
     * @param context Interface to global Context
     * @param objectLock The Object that the RecipeHelper will be locked to until it finishes its
     *                   operations. If mObjectLock is already set, then it checks to ensure
     *                   objectLock matches mObjectLock prior to returning the RecipeHelper instance
     * @return The Singleton instance of RecipeHelper
     */
    public static RecipeHelper getInstance(Context context, @NonNull Object objectLock) {
        if (mContext == null) {
            // Set the member variable
            mContext = context;

            // Lock RecipeHelper to the first Object that calls it so that
            mObjectLock = objectLock;
        }
        // Initialize the RecipeHelper if it is not initialized yet
        if (mRecipeHelper == null) {
            mRecipeHelper = new RecipeHelper();
        }

        if (mObjectLock == objectLock) {
            // If the Object requesting the RecipeHelper is the same Object it is locked to, return
            // mRecipeHelper. This prevents multiple AsyncTasks from attempting to override values
            // if they are run in parallel.
            return mRecipeHelper;
        }

        return null;
    }

    /**
     * Private Constructor for Singleton Construction
     */
    private RecipeHelper() {
        // Stub for Singleton
        initializeCVLists();

        // Initialize the IngredientHelper if it hasn't already been initialized
        if (mIngredientHelper == null) {
            mIngredientHelper = new IngredientHelper();
        }

        recipeId = (int) Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);
    }

    /**
     * Creates new ArrayLists to store the ContentValues of the Recipes to be added to the
     * database
     */
    private void initializeCVLists() {
        // Initialize the ArrayLists
        mRecipeCVList = new ArrayList<>();
        mIngredientCVList = new ArrayList<>();
        mLinkCVList = new ArrayList<>();
    }

    /**
     * Adds the recipe to the list of recipes to be inserted into the database
     * @return The recipeId of the recipe just added to the ArrayList to be inserted into the
     * database
     */
    public int nextNewRecipe() {
        if (!checkRecipeValues()) {
            return -1;
        }

        mRecipeCVList.add(generateRecipeValues());
        resetRecipeValues();

        return recipeId - 1;
    }

    public void insertAllValues() {
        insertRecipeValues();
        insertIngredientValues();
        insertLinkValues();

        mIngredientHelper = null;
        mRecipeHelper = null;
    }

    /**
     * Checks and correct recipe values where applicable.
     * @return true if recipe values can be added to database, false if required information is
     * missing
     */
    private boolean checkRecipeValues() {
        // Check for required values and
        if (recipeName == null || recipeName.isEmpty() || imageUrl == null || imageUrl.isEmpty()) {
            // Required values, if not set, eturn false to indicate the recipes values are invalid
            return false;
        }

        if (recipeAuthor == null) {
            // If no author is given, set the Author to an empty String
            recipeAuthor = "";
        }

        if (isCustomRecipe) {
            // Utilize the function to fill in any information that is missing for custom recipes
            customRecipe();
        }

        if (recipeUrl == null || recipeUrl.isEmpty()) {
            // Required value, if not set, return false to indicate the recipes values are invalid
            return false;
        }

        if (recipeSourceId == null || recipeSourceId.isEmpty()) {
            // Utilize the recipeId as the recipeSourceId if the recipe/website does not
            // provide one
            recipeSourceId = Integer.toString(recipeId);
        }

        if (dateAdded == 0) {
            // Initialize the dateAdded Field if it has not been manually set
            dateAdded = Utilities.getCurrentTime();
        }

        if (source == null || source.isEmpty()) {
            // Check that the recipeUrl is a valid website
            boolean generateSource = Uri.parse(recipeUrl).getScheme().matches("http[s]?");

            if (generateSource) {
                // If the source is not explicitly given, utilize the authority of the website
                // as the source
                source = Uri.parse(recipeUrl).getAuthority();
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Generates a ContentValues containing the recipe's information
     * @return ContentValues with recipe information
     */
    public ContentValues generateRecipeValues() {
        // Initialize the ContentValues for the recipe information
        ContentValues recipeValues = new ContentValues();

        // Add the recipe information into the ContentValues
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_SOURCE_ID, recipeSourceId);
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, recipeName);
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, recipeAuthor);
        recipeValues.put(RecipeEntry.COLUMN_IMG_URL, imageUrl);
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, recipeUrl);
        recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, description);

        if (rating != -1) {
            recipeValues.put(RecipeEntry.COLUMN_RATING, rating);
        }
        if (reviews != -1) {
            recipeValues.put(RecipeEntry.COLUMN_REVIEWS, reviews);
        }

        recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, directions);
        recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, dateAdded);
        recipeValues.put(RecipeEntry.COLUMN_FAVORITE, favorite ? 1 : 0);
        recipeValues.put(RecipeEntry.COLUMN_SOURCE, source);

        // Nutrition info
        if (calories != -1) {
            recipeValues.put(RecipeEntry.COLUMN_CALORIES, calories);
        }
        if (fat != -1) {
            recipeValues.put(RecipeEntry.COLUMN_FAT, fat);
        }
        if (carbs != -1) {
            recipeValues.put(RecipeEntry.COLUMN_CARBS, carbs);
        }
        if (protein != -1) {
            recipeValues.put(RecipeEntry.COLUMN_PROTEIN, protein);
        }
        if (cholesterol != -1) {
            recipeValues.put(RecipeEntry.COLUMN_CHOLESTEROL, cholesterol);
        }
        if (sodium != -1) {
            recipeValues.put(RecipeEntry.COLUMN_SODIUM, sodium);
        }
        if (servings != -1) {
            recipeValues.put(RecipeEntry.COLUMN_SERVINGS, servings);
        }

        return recipeValues;
    }

    /**
     * Inserts all ContentValues in mRecipeCVList into the database
     * @return Number of recipes inserted into database
     */
    public int insertRecipeValues() {
        if (checkRecipeValues()) {
            mRecipeCVList.add(generateRecipeValues());
            resetRecipeValues();
        }

        if (mRecipeCVList.size() == 0) {
            return 0;
        } else if (mRecipeCVList.size() == 1) {
            mContext.getContentResolver().insert(
                    RecipeEntry.CONTENT_URI,
                    mRecipeCVList.get(0)
            );
        } else {
            ContentValues[] recipeCVArray = new ContentValues[mRecipeCVList.size()];
            mRecipeCVList.toArray(recipeCVArray);

            mContext.getContentResolver().bulkInsert(
                    RecipeEntry.CONTENT_URI,
                    recipeCVArray
            );
        }

        return mRecipeCVList.size();
    }

    /**
     * @see #insertRecipeValues()
     *
     * @return Number of ingredients inserted into the database
     */
    public int insertIngredientValues() {
        if (mIngredientCVList.size() == 0) {
            return 0;
        } else if (mIngredientCVList.size() == 1) {
            mContext.getContentResolver().insert(
                    IngredientEntry.CONTENT_URI,
                    mIngredientCVList.get(0)
            );
        } else {
            ContentValues[] ingredientCVArray = new ContentValues[mIngredientCVList.size()];
            mIngredientCVList.toArray(ingredientCVArray);

            mContext.getContentResolver().bulkInsert(
                    IngredientEntry.CONTENT_URI,
                    ingredientCVArray
            );
        }

        return mIngredientCVList.size();
    }

    /**
     * @see #insertRecipeValues()
     * @return Number of link values inserted into database
     */
    public int insertLinkValues() {
        if (mLinkCVList.size() == 0) {
            return 0;
        } else if (mLinkCVList.size() == 1) {
            mContext.getContentResolver().insert(
                    LinkIngredientEntry.CONTENT_URI,
                    mLinkCVList.get(0)
            );
        } else {
            ContentValues[] linkCVArray = new ContentValues[mLinkCVList.size()];
            mLinkCVList.toArray(linkCVArray);

            mContext.getContentResolver().bulkInsert(
                    LinkIngredientEntry.CONTENT_URI,
                    linkCVArray
            );
        }

        return mLinkCVList.size();
    }

    /**
     * Used to indicate that the recipe is a custom recipe
     */
    public void customRecipe() {
        // Set boolean to indicate recipe is custom
        isCustomRecipe = true;

        if (recipeSourceId != null) {
            // If the source ID has already been set, prepend the asterisk to denote it is custom
            // within the database
            recipeSourceId = "*" + recipeSourceId;
        } else {
            // Otherwise prepend an asterisk to the recipeId to be used as the recipeSourceId
            recipeSourceId = "*" + recipeId;
        }

        // Generate a recipeUrl based on the recipeSourceId
        recipeUrl = "content://user.custom/" + recipeSourceId;

        // Utilize the custom-source as the source, if it is a user-created source
        if (source == null && recipeSourceId.equals("*" + recipeId)) {
            source = Resources.getSystem().getString(R.string.attribution_custom);
        }
    }

    public void addIngredient(String ingredientAndQuality) {
        mIngredientHelper.addIngredient(ingredientAndQuality);
    }

    private void resetRecipeValues() {
        // Increment the recipeId
        recipeId++;

        // Reset all other Fields to their default values
        recipeSourceId = "";
        recipeName = "";
        recipeAuthor = "";
        imageUrl = "";
        recipeUrl = "";
        description = "";
        rating = -1;
        reviews = -1;
        directions = "";
        dateAdded = 0;
        favorite = false;
        source = "";
        calories = -1;
        fat = -1;
        carbs = -1;
        protein = -1;
        cholesterol = -1;
        sodium = -1;
        servings = -1;

        isCustomRecipe = false;

        ingredientOrder = 0;
    }

    public int getRecipeCount() {
        return mRecipeCVList.size();
    }

    public int getIngredientCount() {
        return mIngredientCVList.size();
    }

    public int getLinkCount() {
        return mLinkCVList.size();
    }

    private class IngredientHelper {
        // Member Variables
        private IngredientHelper mIngredientHelper = null;

        Map<Long, String> ingredientIdNameMap;

        // Ingredient Columns
        String ingredientAndQuantity;
        long ingredientId;
        long allrecipesId = -1;
        long foodId = -1;
        String ingredient;
        String quantity;

//        public IngredientHelper getInstance() {
//            // Initialize the IngredientHelper if it hasn't already been initialized
//            if (mIngredientHelper == null) {
//                mIngredientHelper = new IngredientHelper();
//            }
//            // Return the Singleton instance of the IngredientHelper
//            return mIngredientHelper;
//        }

        @SuppressLint("UseSparseArrays")
        private IngredientHelper() {
            ingredientIdNameMap = new HashMap<>();
        }

        private void resetIngredientHelper() {
            mIngredientHelper = null;
        }

        /**
         * Take an ingredient String, generates ContentValues for the ingredient and link values,
         * then adds them to the list
         * @param ingredientAndQuantity String containing the ingredient and quantity
         */
        private void addIngredient(String ingredientAndQuantity) {
            // Split the ingredient String into separated quantity and ingredient name
            Pair<String, String> ingredientQuantityPair = Utilities.getIngredientQuantity(ingredientAndQuantity);
            ingredient = ingredientQuantityPair.first;
            quantity = ingredientQuantityPair.second;

            // Check to see if ingredient already exists in database
            ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient);
            boolean skipAddIngredient = false;

            if (ingredientId == -1) {
                // If it does not, find the ID that will be automatically generated for this
                // ingredient
                ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
            } else {
                skipAddIngredient = true;
            }

            // Check to see if the ingredient ID has already been used by a previous ingredient
            // for this recipe
            while (ingredientIdNameMap.containsKey(ingredientId) &&
                    !ingredient.equals(ingredientIdNameMap.get(ingredientId))) {
                // If so, increment the ingredientID until an unused one is found
                ingredientId++;
            }

            // Final check to see if ingredient already exists in ingredientIdNameMap
            String ingredientMapName = ingredientIdNameMap.get(ingredientId);

            if (ingredient.equals(ingredientMapName)) {
                // If it exists, there is no need to add a duplicate to the ingredient table
                skipAddIngredient = true;
            }

            // Add the ingredient ID to ingredientIdNameMap to keep track of which IDs have
            // already been used
            ingredientIdNameMap.put(ingredientId, ingredient);

            Log.d("TEST", "Quantity: " + quantity + " | Ingredient: " + ingredient);

            if (!skipAddIngredient) {
                mIngredientCVList.add(generateIngredientValues());
            }

            mLinkCVList.add(generateLinkValues());
        }

        /**
         * Generate a ContentValues containing ingredient information
         * @return ContentValues with ingredient information
         */
        private ContentValues generateIngredientValues() {
            // Initialize the ContentValues
            ContentValues ingredientValues = new ContentValues();

            // Add ingredient information
            ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
            ingredientValues.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);

            if (allrecipesId != -1) {
                ingredientValues.put(IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID, allrecipesId);
            }

            if (foodId != -1) {
                ingredientValues.put(IngredientEntry.COLUMN_FOOD_INGREDIENT_ID, foodId);
            }

            return ingredientValues;
        }

        /**
         * Generate a ContentValues with the link table information
         * @return ContentValues with link information
         */
        private ContentValues generateLinkValues() {
            // Initialize the ContentValues
            ContentValues linkValues = new ContentValues();

            // Add the link information
            linkValues.put(RecipeEntry.COLUMN_RECIPE_ID, recipeId);
            linkValues.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
            linkValues.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
            linkValues.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, ingredientOrder);

            // Increment the ingredient order
            ingredientOrder++;

            return linkValues;
        }

        // Setters ---------------------------------------------------------------------------------
        public IngredientHelper setIngredient(String ingredientAndQuantity) {
            this.ingredientAndQuantity = ingredient;

            return mIngredientHelper;
        }

        public IngredientHelper setAllrecipesId(long allrecipesId) {
            this.allrecipesId = allrecipesId;

            return mIngredientHelper;
        }

        public IngredientHelper setFoodId(long foodId) {
            this.foodId = foodId;

            return mIngredientHelper;
        }
    }

    // Getters -------------------------------------------------------------------------------------

    public int getRecipeId() {
        return recipeId;
    }

    public String getRecipeSourceId() {
        return recipeSourceId;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getRecipeAuthor() {
        return recipeAuthor;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getRecipeUrl() {
        return recipeUrl;
    }

    public String getDescription() {
        return description;
    }

    public double getRating() {
        return rating;
    }

    public int getReviews() {
        return reviews;
    }

    public String getDirections() {
        return directions;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public String getSource() {
        return source;
    }

    public double getCalories() {
        return calories;
    }

    public double getFat() {
        return fat;
    }

    public double getCarbs() {
        return carbs;
    }

    public double getProtein() {
        return protein;
    }

    public double getCholesterol() {
        return cholesterol;
    }

    public double getSodium() {
        return sodium;
    }

    public int getServings() {
        return servings;
    }

    // Setters -------------------------------------------------------------------------------------

    public RecipeHelper setRecipeId(int recipeId) {
        this.recipeId = recipeId;

        return mRecipeHelper;
    }

    public RecipeHelper setRecipeSourceId(String recipeSourceId) {
        this.recipeSourceId = recipeSourceId;

        return mRecipeHelper;
    }

    public RecipeHelper setRecipeName(String recipeName) {
        this.recipeName = recipeName;

        return mRecipeHelper;
    }

    public RecipeHelper setRecipeAuthor(String recipeAuthor) {
        this.recipeAuthor = recipeAuthor;

        return mRecipeHelper;
    }

    public RecipeHelper setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;

        return mRecipeHelper;
    }

    public RecipeHelper setRecipeUrl(String recipeUrl) {
        this.recipeUrl = recipeUrl;

        return mRecipeHelper;
    }

    public RecipeHelper setDescription(String description) {
        this.description = description;

        return mRecipeHelper;
    }

    public RecipeHelper setRating(double rating) {
        this.rating = rating;

        return mRecipeHelper;
    }

    public RecipeHelper setReviews(int reviews) {
        this.reviews = reviews;

        return mRecipeHelper;
    }

    public RecipeHelper setDirections(String directions) {
        this.directions = directions;

        return mRecipeHelper;
    }

    public RecipeHelper setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;

        return mRecipeHelper;
    }

    public RecipeHelper setFavorite(boolean favorite) {
        this.favorite = favorite;

        return mRecipeHelper;
    }

    public RecipeHelper setSource(String source) {
        this.source = source;

        return mRecipeHelper;
    }

    public RecipeHelper setCalories(double calories) {
        this.calories = calories;

        return mRecipeHelper;
    }

    public RecipeHelper setFat(double fat) {
        this.fat = fat;

        return mRecipeHelper;
    }

    public RecipeHelper setCarbs(double carbs) {
        this.carbs = carbs;

        return mRecipeHelper;
    }

    public RecipeHelper setProtein(double protein) {
        this.protein = protein;

        return mRecipeHelper;
    }

    public RecipeHelper setCholesterol(double cholesterol) {
        this.cholesterol = cholesterol;

        return mRecipeHelper;
    }

    public RecipeHelper setSodium(double sodium) {
        this.sodium = sodium;

        return mRecipeHelper;
    }

    public RecipeHelper setServings(int servings) {
        this.servings = servings;

        return mRecipeHelper;
    }
}
