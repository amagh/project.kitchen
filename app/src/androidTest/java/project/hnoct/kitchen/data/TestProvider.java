package project.hnoct.kitchen.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TestProvider {
    /** Constants **/
    Context mContext;

    @Before
    public void createDatabase() {
        mContext = InstrumentationRegistry.getContext();
        System.out.println("Current number of databases: " + mContext.databaseList().length);
        mContext.deleteDatabase(RecipeDbHelper.DATABASE_NAME);
    }

    @Test
    public void testInsertion() {
        System.out.println("Test");
        assertTrue("Database not properly cleaned prior to test. " + mContext.databaseList().length, mContext.databaseList().length == 0);
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
//        assertTrue("Database not properly created!. " + mContext.databaseList().length, mContext.databaseList().length == 1);

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
        assertTrue("Database not properly cleaned prior to test.", mContext.databaseList().length == 0);
        testInsertion();
        Uri recipeUri = RecipeContract.RecipeEntry.CONTENT_URI;
        Cursor cursor = mContext.getContentResolver().query(
                recipeUri,
                null,
                null,
                null,
                null
        );

        assertTrue("Error querying recipe database. No cursor returned", cursor.moveToFirst());
    }

}