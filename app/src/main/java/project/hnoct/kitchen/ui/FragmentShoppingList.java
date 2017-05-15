package project.hnoct.kitchen.ui;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterIngredient;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentShoppingList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // Constants
    private static final String LOG_TAG = FragmentShoppingList.class.getSimpleName();
    private final int SHOPPING_LOADER = 5;

    // Member Variables
    private Context mContext;
    private Cursor mCursor;
    private AdapterIngredient mAdapter;
    private LinearLayoutManager mLayoutManager;

    // ButterKnife Bounds Views
    @BindView(R.id.shopping_list_recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.shopping_list_info_cardview) CardView mCardView;

    public FragmentShoppingList() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_list, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        mAdapter = new AdapterIngredient(mContext);
        mAdapter.useAsShoppingList();

        mLayoutManager = new LinearLayoutManager(mContext);

        // Set the Adapter and LayoutManager for the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Set up parameters for creating the CursorLoader
        Uri linkUri = LinkIngredientEntry.CONTENT_URI;
        String[] projection = LinkIngredientEntry.LINK_PROJECTION;
        String selection = LinkIngredientEntry.COLUMN_SHOPPING + " = ?";
        String[] selectionArgs = new String[] {"1"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC, " +
                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " ASC";

        // Create and return the CursorLoader
        return new CursorLoader(mContext,
                linkUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            // Set the member variable as the returned Cursor
            mCursor = cursor;

            // Swap mCursor into mAdapter
            mAdapter.swapCursor(mCursor);

            if (mAdapter.getToggleStatus()) {
                mAdapter.toggleChecked();
            }

            mAdapter.addRecipeTitles();

            if (cursor.moveToFirst()) {
                mCardView.setVisibility(View.GONE);
            } else {
                mCardView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPause() {
        // Save checked items to database
        saveCheckedItems();

        super.onPause();
    }

    /**
     * Save all checked items to the database so their values can be retrieved later
     */
    private void saveCheckedItems() {
        // Initialize the Array that will hold the corresponding recipe and ingredient IDs
        boolean[] checkedArray = mAdapter.getListCheckedArray();
        int[] recipeIdArray = new int[checkedArray.length];
        int[] ingredientIdArray = new int[checkedArray.length];

        // Create an Array of matching recipe and ingredient IDs
        for (int i = 0; i < recipeIdArray.length; i++) {
            mCursor.moveToPosition(i);
            recipeIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_ID);
            ingredientIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_INGREDIENT_ID);
        }

        // Run the AsyncTask that will update the database with the new checked status of the items
        ModifyDatabaseChecked checkedAsyncTask = new ModifyDatabaseChecked(recipeIdArray, ingredientIdArray, checkedArray);
        checkedAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * AsyncTask for storing the checkedArray value of an ingredient
     */
    private class ModifyDatabaseChecked extends AsyncTask<Void, Void, Void> {
        // Member Variables
        int[] recipeIdArray;
        int[] ingredientIdArray;
        boolean[] checkedArray;

        public ModifyDatabaseChecked(int[] recipeIdArray, int[] ingredientIdArray, boolean[] checkedArray) {
            // Set up the parameters for modifying the database
            this.recipeIdArray = recipeIdArray;
            this.ingredientIdArray = ingredientIdArray;
            this.checkedArray = checkedArray;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            // Set up the update operation parameters
            String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";

            // Build a ContentProviderOperation for each item in the Array
            for (int i = 0; i < recipeIdArray.length; i++) {
                // Initialize the selection arguments
                String[] selectionArgs = new String[]{Integer.toString(recipeIdArray[i]),
                        Integer.toString(ingredientIdArray[i])};

                // Builder the update operation
                ContentProviderOperation operation =
                        ContentProviderOperation.newUpdate(LinkIngredientEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING_CHECKED, checkedArray[i] ? 1 : 0)
                                .build();

                // Add the operation to the List of operations to be performed on the database
                operations.add(operation);
            }

            // Batch apply all update operations
            try {
                mContext.getContentResolver().applyBatch(
                        RecipeContract.CONTENT_AUTHORITY,
                        operations
                );
            } catch (OperationApplicationException | RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap in a null Cursor
        mAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initiailize the CursorLoader
        getLoaderManager().initLoader(SHOPPING_LOADER, null, this);
    }

    public void deleteCheckedItems() {
        boolean[] checkedArray = mAdapter.getListCheckedArray();
        int[] recipeIdArray = new int[checkedArray.length];
        int[] ingredientIdArray = new int[checkedArray.length];

        // Create an Array of matching recipe and ingredient IDs
        for (int i = 0; i < recipeIdArray.length; i++) {
            mCursor.moveToPosition(i);
            recipeIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_ID);
            ingredientIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_INGREDIENT_ID);
        }

        ModifyDatabaseDeleteChecked deleteAsyncTask = new ModifyDatabaseDeleteChecked(recipeIdArray, ingredientIdArray, checkedArray);
        deleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class ModifyDatabaseDeleteChecked extends AsyncTask<Void, Void, Void> {
        // Member Variables
        int[] recipeIdArray;
        int[] ingredientIdArray;
        boolean[] checkedArray;

        public ModifyDatabaseDeleteChecked(int[] recipeIdArray, int[] ingredientIdArray, boolean[] checkedArray) {
            // Set up the parameters for modifying the database
            this.recipeIdArray = recipeIdArray;
            this.ingredientIdArray = ingredientIdArray;
            this.checkedArray = checkedArray;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            // Set up the update operation parameters
            String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";

            // Build a ContentProviderOperation for each item in the Array
            for (int i = 0; i < recipeIdArray.length; i++) {
                // Initialize the selection arguments
                String[] selectionArgs = new String[]{Integer.toString(recipeIdArray[i]),
                        Integer.toString(ingredientIdArray[i])};

                // Builder the update operation
                ContentProviderOperation operation =
                        ContentProviderOperation.newUpdate(LinkIngredientEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING_CHECKED, 0)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING, checkedArray[i] ? 0 : 1)
                                .build();

                // Add the operation to the List of operations to be performed on the database
                operations.add(operation);
            }

            // Batch apply all update operations
            try {
                mContext.getContentResolver().applyBatch(
                        RecipeContract.CONTENT_AUTHORITY,
                        operations
                );
            } catch (OperationApplicationException | RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}
