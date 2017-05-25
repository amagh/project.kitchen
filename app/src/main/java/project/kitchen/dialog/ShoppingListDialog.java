package project.kitchen.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import project.kitchen.R;
import project.kitchen.data.RecipeContract;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.ui.adapter.AdapterIngredient;

/**
 * Created by hnoct on 5/11/2017.
 */

public class ShoppingListDialog extends DialogFragment {
    // Constant

    // Member Variables
    private AdapterIngredient mAdapter;     // For displaying ingredient information in RecyclerView
    private Cursor mCursor;                 // Cursor with ingredient information

    // ButterKnife bound views
    @BindView(R.id.dialog_shopping_list_recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.dialog_shopping_list_checkbox) CheckBox mCheckBox;

    @OnCheckedChanged(R.id.dialog_shopping_list_checkbox)
    void onCheckChanged(boolean isChecked) {
        mAdapter.toggleChecked();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate the layout that will be used for the DialogFragment
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_shopping_list, null);
        ButterKnife.bind(this, view);

        // Initialize the AdapterIngredient used for displaying ingredient information
        mAdapter = new AdapterIngredient(getActivity());

        // Swap the Cursor passed from ActivityRecipeDetails
        mAdapter.swapCursor(mCursor);

        // Set the Adapter to use ShoppingListMode
        mAdapter.useAsShoppingList();

        // Initialize and set the LayoutManager
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(llm);

        // Set the Adapter to the RecyclerView
        mRecyclerView.setAdapter(mAdapter);

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SetDatabaseShoppingList asyncTask = new SetDatabaseShoppingList();
                asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                dismiss();
            }
        });

        builder.setNegativeButton(getString(R.string.button_deny), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });

        return builder.create();
    }

    /**
     * Receives a Cursor with ingredient information to be used for mAdapter
     * @param cursor Cursor with ingredient information
     */
    public void setIngredientCursor(Cursor cursor) {
        mCursor = cursor;
    }

    /**
     * AsyncTask that updates the database with the new shopping list values
     */
    private class SetDatabaseShoppingList extends AsyncTask<Void, Void, Void> {
        // Member Variables
        boolean[] shoppingListValues;                               // Holds boolean value for whether ingredient should be in shopping list
        ArrayList<ContentProviderOperation> editOperationsList;     // List of edit operations to perform on the database

        public SetDatabaseShoppingList() {
            // Initialize member variables
            shoppingListValues = mAdapter.getListCheckedArray();
            editOperationsList = new ArrayList<>(shoppingListValues.length);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Move the Cursor to the first position
            mCursor.moveToFirst();

            // Generate the ContentProviderOperations to be performed on the database
            createEditOperation();

            try {
                // Batch apply the ContentProviderOperations
                getActivity().getContentResolver().applyBatch(
                        RecipeContract.CONTENT_AUTHORITY,
                        editOperationsList
                );
            } catch (RemoteException | OperationApplicationException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Recursive function that will populate editOperationsList with ContentProviderOperations
         * that edit the database with the new shopping list value of each ingredient
         */
        void createEditOperation() {
            // Retrieve the values to be updated in the database
            long ingredientId = mCursor.getLong(LinkIngredientEntry.IDX_INGREDIENT_ID);
            int recipeId = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_ID);
            int inShoppingList = shoppingListValues[0] ? 1 : 0;

            // Parameters for the ContentProviderOperation
            Uri linkUri = LinkIngredientEntry.CONTENT_URI;
            String selection = IngredientEntry.COLUMN_INGREDIENT_ID + " = ? AND " +
                    RecipeEntry.COLUMN_RECIPE_ID + " = ?";
            String[] selectionArgs = new String[] {Long.toString(ingredientId),
                    Integer.toString(recipeId)};

            // Generate the ContentProviderOperation with the parameters and values
            ContentProviderOperation operation = ContentProviderOperation.newUpdate(linkUri)
                    .withSelection(selection, selectionArgs)
                    .withValue(LinkIngredientEntry.COLUMN_SHOPPING, inShoppingList)
                    .build();

            // Add the ContentProviderOperation to the ArrayList
            editOperationsList.add(operation);

            // Create a copy of shoppingListValues that removes the first value in the Array
            boolean[] tempArray = new boolean[shoppingListValues.length - 1];
            System.arraycopy(shoppingListValues, 1, tempArray, 0, tempArray.length);

            // Set shoppingListValues to the newly created boolean[]
            shoppingListValues = tempArray;

            if (mCursor.moveToNext()) {
                // Call itself to generate the next ContentProviderOperation
                createEditOperation();
            }
        }
    }
}
