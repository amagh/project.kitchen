package project.hnoct.kitchen.ui.adapter;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.Optional;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 2/21/2017.
 */

public class AdapterIngredient extends RecyclerView.Adapter<AdapterIngredient.IngredientViewHolder> {
    /** Constants **/

    /** Member Variables **/
    private Context mContext;                   // Interface for global context
    private Cursor mCursor;
    private ContentResolver mContentResolver;   // Reference to ContentResolver
    private boolean[] mShoppingListArray;       // TODO: For storing ingredients that need to be added to shopping list
    private boolean isShoppingList = false;

    public AdapterIngredient(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public IngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (isShoppingList) {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_shopping_list, parent, false);
        } else {
            view = LayoutInflater.from(mContext).inflate(R.layout.list_item_ingredient, parent, false);
        }

        view.setFocusable(true);

        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(IngredientViewHolder holder, int position) {
        // Move Cursor to correct position
        mCursor.moveToPosition(position);

        // Retrieve ingredient information
        String quantity = mCursor.getString(RecipeContract.LinkIngredientEntry.IDX_LINK_QUANTITY);
        quantity = Utilities.convertToUnicodeFraction(mContext, quantity);
        String ingredient = mCursor.getString(RecipeContract.LinkIngredientEntry.IDX_INGREDIENT_NAME);

        // Check whether Adapter is in shopping-list-mode
        if (isShoppingList) {
            // If in shopping-list-mode remove preparation steps from the ingredient String
            ingredient = Utilities.removePreparation(ingredient);

            // Check whether the CheckBox should be checked
            boolean isChecked = mShoppingListArray[position];

            // Set CheckBox status
            if (isChecked) {
                holder.mCheckBox.setChecked(true);
            } else {
                holder.mCheckBox.setChecked(false);
            }
        }

        // Check to see if ingredient is a header (headers are notated with a ":")
        Pattern pattern = Pattern.compile(".*:");
        Matcher match = pattern.matcher(ingredient);

        if (match.matches()) {
            // Ingredient is not an ingredient, but a header, so bold the text to make it stand out and remove the colon
            ingredient = ingredient.substring(0, ingredient.length() -1);
            holder.ingredientNameText.setText(ingredient);
            holder.ingredientNameText.setTypeface(holder.ingredientNameText.getTypeface(), Typeface.BOLD);

            if (isShoppingList) {
                // Headers do not need to display a checkbox
                holder.mCheckBox.setVisibility(View.GONE);
                holder.quantityText.setVisibility(View.GONE);
                holder.ingredientNameText.setGravity(Gravity.CENTER_HORIZONTAL);
            }
        } else {
            holder.ingredientNameText.setText(ingredient);
            holder.ingredientNameText.setTypeface(Typeface.SANS_SERIF);

            if (isShoppingList) {
                holder.mCheckBox.setVisibility(View.VISIBLE);
                holder.quantityText.setVisibility(View.VISIBLE);
                holder.ingredientNameText.setGravity(Gravity.NO_GRAVITY);
            }
        }

        // Set the view parameters
        holder.quantityText.setText(Utilities.abbreviateMeasurements(quantity));
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * Sets the Adapter to use shopping-list-mode with a different layout and parameters
     */
    public void useAsShoppingList() {
        // Set the member boolean
        isShoppingList = true;

        // Check if Cursor has already been passed to the Adapter
        if (mCursor != null) {
            // Initialize a new Array that will hold the boolean values for whether the ingredient
            // has been checked
            mShoppingListArray = new boolean[mCursor.getCount()];
            for (int i = 0; i < mShoppingListArray.length; i++) {
                // Check all ingredients to start
                mShoppingListArray[i] = true;
            }
        }
    }

    public boolean[] getShoppingListValues() {
        return mShoppingListArray;
    }

    /**
     * Swaps the Cursor in the adapter with a new Cursor
     * @param newCursor Cursor that is to be swapped in
     * @return The Cursor after it has been swapped
     */
    public Cursor swapCursor(Cursor newCursor) {
        if (mCursor != newCursor) {
            // Set the member Cursor to the new Cursor supplied
            mCursor = newCursor;

            if (isShoppingList) {
                mShoppingListArray = new boolean[mCursor.getCount()];
                for (int i = 0; i < mShoppingListArray.length; i++) {
                    mShoppingListArray[i] = false;
                }
            }

            // Notify the Adapter that the data has changed
            notifyDataSetChanged();
        }
        return mCursor;
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_ingredient_quantity) TextView quantityText;
        @BindView(R.id.list_ingredient_name) TextView ingredientNameText;
        @Nullable @BindView(R.id.list_shopping_checkbox) CheckBox mCheckBox;

        @Optional
        @OnCheckedChanged(R.id.list_shopping_checkbox)
        void onCheckChanged(boolean isChecked) {
            // Retrieve the position of the ViewHolder
            int position = getAdapterPosition();

            // Set whether the ingredient has been checked or unchecked
            if (isChecked) {
                mShoppingListArray[position] = true;
            } else {
                mShoppingListArray[position] = false;
            }
        }

        public IngredientViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
