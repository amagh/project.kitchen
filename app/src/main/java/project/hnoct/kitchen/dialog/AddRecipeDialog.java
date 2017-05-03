package project.hnoct.kitchen.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterRecipe;
import project.hnoct.kitchen.ui.ActivityRecipeList;

/**
 * Created by hnoct on 4/5/2017.
 */

public class AddRecipeDialog extends DialogFragment {
    /** Constants **/

    private AdapterRecipe mRecipeAdapter;
    private SelectionListener mListener;
    private Cursor mCursor;
    Context mContext;

    // Views Bound by ButterKnife
    @BindView(R.id.dialog_add_recipe_recyclerview) RecyclerView mRecyclerView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Initialize the Builder for the Dialg
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Inflate the view to be used
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_recyclerview, null);
        ButterKnife.bind(this, view);

        mRecipeAdapter = new AdapterRecipe(
                getActivity(),
                new AdapterRecipe.RecipeAdapterOnClickHandler() {
                    @Override
                    public void onClick(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
                        if (mListener != null) {
                            // Utilize Callback interface to send information about recipe that was
                            // selected
                            mListener.onRecipeSelected(recipeUrl);

                            // Close the Cursor
                            if (mCursor != null) mCursor.close();

                            dismiss();
                        }
                    }
                }
        );

        // Get the number of columns to be used in the RecyclerView
        int columns = (ActivityRecipeList.mTwoPane && ActivityRecipeList.mDetailsVisible) ?
                getResources().getInteger(R.integer.recipe_twopane_columns) :
                getResources().getInteger(R.integer.recipe_columns);

        // Initialize the StaggeredLayoutManager for the RecyclerView
        StaggeredGridLayoutManager sglm = new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL);

        // Set the LayoutManager and the Adapter for the RecyclerView
        mRecyclerView.setLayoutManager(sglm);
        mRecyclerView.setAdapter(mRecipeAdapter);

        // Query the database for all recipes sorting by favorites and then date added
        String sortOrder = RecipeEntry.COLUMN_FAVORITE + " DESC, " + RecipeEntry.COLUMN_DATE_ADDED + " DESC";
        mCursor = getActivity().getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                RecipeEntry.RECIPE_PROJECTION,
                null,
                null,
                sortOrder
        );

        // Swap the Cursor into the AdapterRecipe
        mRecipeAdapter.swapCursor(mCursor);

        // Set the View to be displayed
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // Close the Cursor if it is still opened
        if (mCursor != null) mCursor.close();
    }

    /**
     * Interface for notifying which recipe has been selected
     */
    public interface SelectionListener {
        void onRecipeSelected(String recipeUrl);
    }

    /**
     * Registers an observer to be notified for recipe selection
     * @param listener SelectionListener to be set to the member variable
     */
    public void setSelectionListener(SelectionListener listener) {
        mListener = listener;
    }

}
