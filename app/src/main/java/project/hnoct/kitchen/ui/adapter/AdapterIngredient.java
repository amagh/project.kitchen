package project.hnoct.kitchen.ui.adapter;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.Optional;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 2/21/2017.
 */

public class AdapterIngredient extends RecyclerView.Adapter<AdapterIngredient.IngredientViewHolder> {
    /** Constants **/
    private final static String LOG_TAG = AdapterIngredient.class.getSimpleName();
    private final int INGREDIENT_VIEW = 0;
    private final int SHOPPING_LIST_VIEW = 1;
    private final int RECIPE_TITLE_VIEW = 2;

    /** Member Variables **/
    private Context mContext;                   // Interface for global context
    private Cursor mCursor;
    private ContentResolver mContentResolver;   // Reference to ContentResolver
    private boolean[] mCheckedArray;       // TODO: For storing ingredients that need to be added to shopping list
    private boolean isShoppingList = false;
    private boolean toggleChecked = true;
    private boolean showRecipeTitles = false;

    private List<Integer> mRecipeTitlePositionList;
    private List<String> mRecipeTitlesList;

    private CheckListener mCheckListener;

    public AdapterIngredient(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public IngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case INGREDIENT_VIEW: {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item_ingredient, parent, false);
                break;
            }
            case SHOPPING_LIST_VIEW: {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item_shopping_list, parent, false);
                break;
            }
            case RECIPE_TITLE_VIEW: {
                view = LayoutInflater.from(mContext).inflate(R.layout.list_item_ingredient_recipe_title, parent, false);
                break;
            }
            default: throw new UnsupportedOperationException("Unknown viewtype");
        }

        view.setFocusable(true);

        return new IngredientViewHolder(view);
    }

    @Override
    public long getItemId(int position) {
        // Utilize the Link ID as the item ID so that animations can be correctly used when items
        // are removed from the Cursor and notifyDataSetChanged is called
        if (mCursor != null) {
            // Initialize the position modifier
            int positionModifier = 0;
            for (int i : mRecipeTitlePositionList) {
                // For recipe titles, they can just return their position as they do not need to
                // be animated
                if (position == i) {
                    return position;
                } else if (position > i) {
                    // Add one to the position modifier for every title it is greater than
                    positionModifier++;
                }

                // Modify the position the Cursor should point to
                position = position - positionModifier;
            }

            // Retrieve the Link ID to be used as the item ID
            mCursor.moveToPosition(position);
            return mCursor.getInt(LinkIngredientEntry.IDX_LINK_ID);
        } else {
            return position;
        }
    }

    @Override
    public void onBindViewHolder(IngredientViewHolder holder, int position) {
        // Initialize a position modifier to compensate for the recipe title positions
        int positionModifier = 0;

        // Check whether the Adapter is in shopping-list-mode
        if (showRecipeTitles) {
            // Iterate through mRecipeTitlePosition and check if the position matches
            for (int i = 0; i < mRecipeTitlePositionList.size(); i++) {
                if (position == mRecipeTitlePositionList.get(i)) {
                    // If it matches, then populate the view with the recipe title
                    String recipeTitle = mRecipeTitlesList.get(i);
                    holder.ingredientNameText.setText(recipeTitle);

                    return;
                } else if (position > mRecipeTitlePositionList.get(i)) {
                    // If it is greater than the position, then increment the position modifier
                    positionModifier++;
                }
            }

            // Compensate for any added recipe titles
            position = position - positionModifier;

            // Store the modifier as the tag on the itemView so that it can be properly re-set
            // in the ViewHolder's #onCheckChanged method
            holder.itemView.setTag(positionModifier);
        }

        // Move Cursor to correct position
        mCursor.moveToPosition(position);


        // Retrieve ingredient information
        String quantity = mCursor.getString(LinkIngredientEntry.IDX_LINK_QUANTITY);
        quantity = Utilities.convertToUnicodeFraction(mContext, quantity);
        String ingredient = mCursor.getString(LinkIngredientEntry.IDX_INGREDIENT_NAME);

        // Check whether Adapter is in shopping-list-mode
        if (isShoppingList) {
            // If in shopping-list-mode remove preparation steps from the ingredient String
            ingredient = Utilities.removePreparation(ingredient);

            // Check whether the CheckBox should be checked
            boolean isChecked = mCheckedArray[position];

            // Set CheckBox status
            holder.mCheckBox.setChecked(isChecked);

            // Change the background of the item to better show that an item has been checked off
            if (showRecipeTitles) {
                if (isChecked) {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.grey_300));
                } else {
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.transparent_white));
                }
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
            if (showRecipeTitles) {
                // If the shopping list contains recipe titles, then add the size of the list to the
                // Cursor's count
                return mCursor.getCount() + mRecipeTitlesList.size();
            }

            return mCursor.getCount();
        }
        return 0;
    }

    /**
     * Toggles the check status of all items in the Adapter
     */
    public void toggleChecked() {
        // Check what the toggle is currently set to and reverse it
        if (toggleChecked) {
            toggleChecked = false;
        } else {
            toggleChecked = true;
        }

        // Iterate through each item and change its check status
        for (int i = 0; i < mCheckedArray.length; i++) {
            mCheckedArray[i] = toggleChecked;
        }

        // Notify the Adapter of the change
        notifyDataSetChanged();
    }

    /**
     * Passes the value of toggleChecked
     * @return Boolean value for whether the toggle is set to true or false
     */
    public boolean getToggleStatus() {
        return toggleChecked;
    }

    /**
     * Listener for informing an observer when the number of items checked has changed and its count
     */
    public interface CheckListener {
        void onChecked(int itemsChecked);
    }

    /**
     * Setter for the CheckListener
     * @param listener CheckListener to be registered to the observer
     */
    public void setCheckListener(CheckListener listener) {
        mCheckListener = listener;
    }

    /**
     * Counts the number of items checked off in the shopping list and returns the count
     * @return Number of items that are checked
     */
    public int getItemsCheckedCount() {
        int itemsChecked = 0;
        for (boolean checked : mCheckedArray) {
            // Increment itemsChecks if checked is true
            if (checked) {
                itemsChecked++;
            }
        }

        return itemsChecked;
    }

    /**
     * Sets up the Adapter to add in Recipe Titles in the case of ActivityShoppingList
     */
    public void addRecipeTitles() {
        // Set boolean to true
        showRecipeTitles = true;

        if (mCursor != null) {


            // Initialize the Lists for holding recipe title information
            mRecipeTitlePositionList = new ArrayList<>();
            mRecipeTitlesList = new ArrayList<>();
            int position = 0;

            // Move the Cursor to the first position
            if (mCursor.moveToFirst()) {
                // Iterate through mCursor and check how many different recipes are included
                do {
                    // Retrieve the recipe's title
                    String recipeName = mCursor.getString(LinkIngredientEntry.IDX_RECIPE_NAME);

                    if (!mRecipeTitlesList.contains(recipeName)) {
                        // If the List does not already contain the recipe title, add it and set the
                        // position in which it will show
                        mRecipeTitlePositionList.add(position + mRecipeTitlesList.size());
                        mRecipeTitlesList.add(recipeName);
                    }

                    // Increment the position to maintain parity with the Adapter's position
                    position++;
                } while (mCursor.moveToNext());
            }

            // Make sure mCheckedArray reflects values from database
            getCheckedValuesFromDatabase();
        }
    }

    /**
     * Sets mCheckedArray to use the values from database instead of the default "true" value
     */
    private void getCheckedValuesFromDatabase() {
        for (int i = 0; i < mCheckedArray.length; i++) {
            // Check all ingredients to start
            mCursor.moveToPosition(i);

            // Retrieve the checked value from database
            mCheckedArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_LINK_CHECKED) == 1;
        }
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
            mCheckedArray = new boolean[mCursor.getCount()];
            if (showRecipeTitles) {
                getCheckedValuesFromDatabase();

            } else {
                for (int i = 0; i < mCheckedArray.length; i++) {
                    mCheckedArray[i] = true;
                }
            }
        }
    }

    /**
     * Retrieves the Array holding the boolean Checked values for each item on the shopping list;
     * @return
     */
    public boolean[] getListCheckedArray() {
        // Return mCheckedArray if the Adapter is being used as a shopping list
        if (isShoppingList) {
            return mCheckedArray;
        } else {
            throw new UnsupportedOperationException("This is not a shopping list!");
        }
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

            if (isShoppingList && mCursor != null) {
                mCheckedArray = new boolean[mCursor.getCount()];
                if (showRecipeTitles) {
                    getCheckedValuesFromDatabase();

                } else {
                    for (int i = 0; i < mCheckedArray.length; i++) {
                        mCheckedArray[i] = true;
                    }
                }
            }

            // Notify the Adapter that the data has changed
            notifyDataSetChanged();
        }
        return mCursor;
    }

    @Override
    public int getItemViewType(int position) {
        if (!isShoppingList) {
            return INGREDIENT_VIEW;
        } else {
            if (showRecipeTitles) {
                for (int recipeTitlePosition : mRecipeTitlePositionList) {
                    if (position == recipeTitlePosition) {
                        return RECIPE_TITLE_VIEW;
                    }
                }
            }

            return SHOPPING_LIST_VIEW;
        }
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        @Nullable @BindView(R.id.list_ingredient_quantity) TextView quantityText;
        @BindView(R.id.list_ingredient_name) TextView ingredientNameText;
        @Nullable @BindView(R.id.list_shopping_checkbox) CheckBox mCheckBox;

        @Optional
        @OnCheckedChanged(R.id.list_shopping_checkbox)
        void onCheckChanged(boolean isChecked) {
            // Retrieve the position of the ViewHolder
            int position = getAdapterPosition();

            if (showRecipeTitles) {
                // Retrieve the position modifier stored as the tag of the itemView
                position = position - (int) itemView.getTag();
            }

            boolean previousChecked = mCheckedArray[position];

            if (previousChecked != isChecked) {
                // Set whether the ingredient has been checked or unchecked
                mCheckedArray[position] = isChecked;

                // Set the background correctly.
                if (showRecipeTitles) {
                    if (isChecked) {
                        itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.grey_300));
                    } else {
                        itemView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.transparent_white));
                    }
                }
            }

            // Count the number of items that are checked
            int itemsChecked = 0;
            for (boolean checked : mCheckedArray) {
                if (checked) {
                    itemsChecked++;
                }
            }

            // Inform any registered observers of the number of items checked off
            if (mCheckListener != null) {
                mCheckListener.onChecked(itemsChecked);
            }
        }

        public IngredientViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
