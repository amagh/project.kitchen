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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestProvider {
    /** Constants **/
    Context mContext;

    @Before
    public void deleteDatabase() {
        // Instantiate the application context
        mContext = InstrumentationRegistry.getTargetContext();

        // Clean the database
        mContext.deleteDatabase(RecipeDbHelper.DATABASE_NAME);
    }

    @Test
    public void testInsertion() {
        // Set the URI for the recipe insertion
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;

        // Create values to be inserted
        ContentValues recipeValues = TestUtilities.createTestRecipeValues();

        // Call the ContentProvider to insert the data
        Uri recipeInsertionUri = mContext.getContentResolver().insert(
                recipeUri,
                recipeValues
        );

        // Check to make sure that a URI was returned with the insertion
        assertTrue("Error inserting recipe into database!", recipeInsertionUri != null);

        /** Repeat above steps for ingredient table **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        ContentValues ingredientValues = TestUtilities.createTestIngredientValues();
        Uri ingredientInsertionUri = mContext.getContentResolver().insert(
                ingredientUri,
                ingredientValues
        );
        assertTrue("Error inserting ingredient into database", ingredientInsertionUri != null);

        /** Repeat with link values **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        ContentValues linkValues = TestUtilities.createTestLinkValues();
        Uri linkInsertionUri = mContext.getContentResolver().insert(
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
        Cursor cursor = mContext.getContentResolver().query(
                recipeUri,
                null,
                null,
                null,
                null
        );

        // Ensure the returned Cursor is pointing to at least one row
        assertTrue("Error querying recipe table. Cursor returned empty rows.", cursor.moveToFirst());

        // TODO: Check the contents of the cursor match the inserted values
        assertTrue("Recipe values in database do not match inserted values!",
                TestUtilities.testCursorValues(cursor, recipeValues));

        // Close the cursor
        cursor.close();

        /** Repeat steps for ingredient query **/
        Uri ingredientUri = RecipeContract.IngredientEntry.CONTENT_URI;
        cursor = mContext.getContentResolver().query(
                ingredientUri,
                null,
                null,
                null,
                null
        );

        assertTrue("Error querying ingredient table. Cursor returned empty rows.", cursor.moveToFirst());
        assertTrue("Ingredient values in database do not match inserted values!",
                TestUtilities.testCursorValues(cursor, ingredientValues));

        cursor.close();

        /** Repeat steps for link query **/
        Uri linkUri = RecipeContract.LinkEntry.CONTENT_URI;
        cursor = mContext.getContentResolver().query(
                linkUri,
                null,
                null,
                null,
                null
        );
        assertTrue("Error querying link table. Cursor returned empty rows.", cursor.moveToFirst());
        assertTrue("Link values in database do not match inserted values!",
                TestUtilities.testCursorValues(cursor, linkValues));
        cursor.close();
    }

}