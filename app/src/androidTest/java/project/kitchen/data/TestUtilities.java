package project.kitchen.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.v4.util.Pair;
import android.util.Log;

/**
 * Created by hnoct on 2/17/2017.
 */

class TestUtilities {
    /** Constants **/
    private static final long TEST_RECIPE_ID = 000001;
    private static final long TEST_INGREDIENT_ID = 123;
    private static final long TEST_TIME = 1234567890L;
    static ContentValues createTestRecipeValues() {

        ContentValues recipeValues = new ContentValues();
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_ID, TEST_RECIPE_ID);
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME, "TestRecipe");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_IMG_URL, "http://www.testurl.com/img.jpg");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_URL, "http://www.testurl.com/recipe/000001");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RATING, "5");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_REVIEWS, "100");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_SHORT_DESC, "This is the test recipe description");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_DIRECTIONS, "These are the directions for the test recipe");
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_DATE_ADDED, TEST_TIME);

        return recipeValues;
    }

    static ContentValues createTestIngredientValues() {
        ContentValues ingredientValues = new ContentValues();
        ingredientValues.put(RecipeContract.IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID, TEST_INGREDIENT_ID);
        ingredientValues.put(RecipeContract.IngredientEntry.COLUMN_INGREDIENT_NAME, "salt");

        return ingredientValues;
    }

    static ContentValues createTestLinkValues() {
        ContentValues linkValues = new ContentValues();
        linkValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_ID, TEST_RECIPE_ID);
        linkValues.put(RecipeContract.IngredientEntry.COLUMN_ALLRECIPES_INGREDIENT_ID, TEST_INGREDIENT_ID);
        linkValues.put(RecipeContract.LinkIngredientEntry.COLUMN_QUANTITY, "1/4 tsp");

        return linkValues;
    }

    static Pair<String, String> testCursorValues(Cursor cursor, ContentValues values) {
        for (String key : values.keySet()) {
            String cursorValue = cursor.getString(cursor.getColumnIndex(key));
            if (!cursorValue.equals(values.get(key).toString())) {
                Log.d("TEST", "Cursor value: " + cursorValue + " | Inserted value: " + values.get(key).toString());
                return new Pair<> (cursorValue, values.get(key).toString());
            }
        }
        return null;
    }
}
