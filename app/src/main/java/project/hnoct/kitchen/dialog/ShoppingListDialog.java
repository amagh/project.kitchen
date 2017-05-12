package project.hnoct.kitchen.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.ui.adapter.AdapterIngredient;

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
}
