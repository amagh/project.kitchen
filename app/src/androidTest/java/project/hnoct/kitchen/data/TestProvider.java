package project.hnoct.kitchen.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.util.Pair;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

//@RunWith(AndroidJUnit4.class)
public class TestProvider extends ProviderTestCase2<RecipeProvider> {
    private final String LOG_TAG = TestProvider.class.getSimpleName();
    /** Constants **/
    MockContentResolver mMockContentResolver;

    public TestProvider() {
        super(RecipeProvider.class, RecipeContract.CONTENT_AUTHORITY);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Log.d(LOG_TAG, "in setUp");
        mMockContentResolver = getMockContentResolver();
    }

//    @BeforeClass
//    public void getContext() {
//        mContext = InstrumentationRegistry.getTargetContext();
//    }

//    @Before
//    public void deleteDatabase() {
//        // Instantiate the application context
//        mContext = InstrumentationRegistry.getTargetContext();
//
//        // Clean the database
//        mContext.deleteDatabase(RecipeDbHelper.DATABASE_NAME);
//    }

    @Test
    public void testInsertion() {
        // Set the URI for the recipe insertion
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;

        // Create values to be inserted
        ContentValues recipeValues = TestUtilities.createTestRecipeValues();

        // Call the ContentProvider to insert the data
        Uri recipeInsertionUri = mMockContentResolver.insert(
                recipeUri,
                recipeValues
        );

        // Check to make sure that a URI was returned with the insertion
        assertTrue("Error inserting recipe into database!", recipeInsertionUri != null);


        /** Repeat above steps for ingredient table **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        ContentValues ingredientValues = TestUtilities.createTestIngredientValues();
        Uri ingredientInsertionUri = mMockContentResolver.insert(
                ingredientUri,
                ingredientValues
        );
        assertTrue("Error inserting ingredient into database", ingredientInsertionUri != null);

        /** Repeat with link values **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        ContentValues linkValues = TestUtilities.createTestLinkValues();
        Uri linkInsertionUri = mMockContentResolver.insert(
                linkUri,
                linkValues
        );
        assertTrue("Error inserting link values into database", linkInsertionUri != null);
    }

    @Test
    public void testQuery() {
        // Get the values that were inserted to test against the read values from the Cursor
        ContentValues recipeValues = TestUtilities.createTestRecipeValues();
        ContentValues ingredientValues = TestUtilities.createTestIngredientValues();
        ContentValues linkValues = TestUtilities.createTestLinkValues();

        // Insert the values to query
        testInsertion();

        // Get the URI for the Recipe Table
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;

        // Query the database
        Cursor cursor = mMockContentResolver.query(
                recipeUri,
                null,
                null,
                null,
                null
        );

        // Ensure the returned Cursor is pointing to at least one row
        assertTrue("Error querying recipe table. Cursor returned empty rows.", cursor.moveToFirst());

        // Check the contents of the cursor match the inserted values
        Pair<String, String> checkPair = TestUtilities.testCursorValues(cursor, recipeValues);
        if (checkPair != null) {
            assertNull("Cursor value: " + checkPair.first + " | Inserted valued: " + checkPair.second);
        }
//        assertTrue("Recipe values in database do not match inserted values!",
//                TestUtilities.testCursorValues(cursor, recipeValues));

        // Close the cursor
        cursor.close();

        /** Repeat steps for ingredient query **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        cursor = mMockContentResolver.query(
                ingredientUri,
                null,
                null,
                null,
                null
        );

        assertTrue("Error querying ingredient table. Cursor returned empty rows.", cursor.moveToFirst());
        checkPair = TestUtilities.testCursorValues(cursor, ingredientValues);
        if (checkPair != null) {
            assertNull("Cursor value: " + checkPair.first + " | Inserted valued: " + checkPair.second);
        }
//        assertTrue("Ingredient values in database do not match inserted values!",
//                TestUtilities.testCursorValues(cursor, ingredientValues));
        cursor.close();

        /** Repeat steps for link query **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        cursor = mMockContentResolver.query(
                linkUri,
                null,
                null,
                null,
                null
        );
        assertTrue("Error querying link table. Cursor returned empty rows.", cursor.moveToFirst());
        checkPair = TestUtilities.testCursorValues(cursor, linkValues);
        if (checkPair != null) {
            assertNull("Cursor value: " + checkPair.first + " | Inserted valued: " + checkPair.second);
        }
//        assertTrue("Link values in database do not match inserted values!",
//                TestUtilities.testCursorValues(cursor, linkValues));
        cursor.close();
    }

    @Test
    public void testDelete() {
        /** Variables **/
        int rows;

        // Add values to the database and check that they exist
        testQuery();

        // Get URI for recipe table
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;

        // Delete recipe values from database
        rows = mMockContentResolver.delete(
                recipeUri,
                null,
                null
        );

        assertNotEquals("Error deleting rows from recipe table", rows, 0);

        /** Repeat steps for ingredient table **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        rows = mMockContentResolver.delete(
                ingredientUri,
                null,
                null
        );
        assertNotEquals("Error deleting rows from ingredient table.", rows, 0);

        /** Repeat steps for link table **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        rows = mMockContentResolver.delete(
                linkUri,
                null,
                null
        );
        assertNotEquals("Error deleting rows from link table.", rows, 0);
    }

    @Test
    public void testUpdate() {
        /** Variables **/
        int rows;
        Cursor cursor;

        // Insert values and check that they exist
        testQuery();

        // Generate recipe table URI
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;

        // Generate update values
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME, "TestRecipe2");

        // Update recipe value
        rows = mMockContentResolver.update(
                recipeUri,
                recipeValues,
                RecipeContract.RecipeEntry.COLUMN_RECIPE_NAME + " = ?",
                new String[] {"TestRecipe"}
        );

        // Check that rows were updated
        assertNotEquals("Error updating recipe table.", rows, 0);

        // Query the database to check the values
        cursor = mMockContentResolver.query(
                recipeUri,
                null,
                null,
                null,
                null
        );
        cursor.moveToFirst();

        // Check that the values match the update values
        Pair<String, String> checkPair = TestUtilities.testCursorValues(cursor, recipeValues);
        if (checkPair != null) {
            assertNull("{Recipe} Cursor value: " + checkPair.first + " | Updated value: " + checkPair.second , checkPair);
        }

        /** Repeat steps for ingredient table **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        ContentValues ingredientValues = new ContentValues();
        ingredientValues.put(RecipeContract.IngredientEntry.COLUMN_INGREDIENT_NAME, "pepper");
        rows = mMockContentResolver.update(
                ingredientUri,
                ingredientValues,
                RecipeContract.IngredientEntry.COLUMN_INGREDIENT_NAME + " = ?",
                new String[] {"salt"}
        );

        assertNotEquals("Error updating ingredient table.", rows, 0);

        cursor = mMockContentResolver.query(
                ingredientUri,
                null,
                null,
                null,
                null
        );
        cursor.moveToFirst();
        checkPair = TestUtilities.testCursorValues(cursor, ingredientValues);
        if (checkPair != null) {
            assertNull("{Ingredient} Cursor value: " + checkPair.first + " | Updated value: " + checkPair.second , checkPair);
        }

        /** Repeat steps for link table **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        ContentValues linkValues = new ContentValues();
        linkValues.put(RecipeContract.LinkEntry.COLUMN_QUANTITY, "1/2 tbsp");
        rows = mMockContentResolver.update(
                linkUri,
                linkValues,
                RecipeContract.LinkEntry.COLUMN_QUANTITY + " = ?",
                new String[] {"1/4 tsp"}
        );

        assertNotEquals("Error updating ingredient table.", rows, 0);

        cursor = mMockContentResolver.query(
                linkUri,
                null,
                null,
                null,
                null
        );
        cursor.moveToFirst();
        checkPair = TestUtilities.testCursorValues(cursor, linkValues);
        if (checkPair != null) {
            assertNull("{Link} Cursor value: " + checkPair.first + " | Updated value: " + checkPair.second , checkPair);
        }
    }
}