package project.hnoct.kitchen.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.data.RecipeProvider;

/**
 * Created by hnoct on 3/5/2017.
 */

public class AddIngredientAdapter extends RecyclerView.Adapter<AddIngredientAdapter.AddIngredientViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = AddIngredientAdapter.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;                               // Interface for global Context
    private List<Pair<String, String>> mIngredientList;     // List of all quantities and ingredient names
    private List<Boolean> mAddList;                         // Holds the boolean value for what the button in the ViewHolder should do: add or remove
    private RecyclerView mRecyclerView;                     // References the Recycler containing this adapter for use in searching other ViewHolders
    private OnStartDragListener mDragListener;              // Listens for when a drag even has initiated from a viewHolder's handle

    public AddIngredientAdapter(Context context, OnStartDragListener dragListener) {
        // Initialize the member variables
        mContext = context;
        mIngredientList = new LinkedList<>();
        mAddList = new LinkedList<>();
        mDragListener = dragListener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        // Set the member variable to the RecyclerView the Adapter is attached to
        mRecyclerView = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public AddIngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for adding ingredients
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_add_ingredient, parent, false);

        // Return a new AddIngredientViewHolder containing the inflated view
        return new AddIngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AddIngredientViewHolder holder, int position) {
        // Check whether the add button should be set to add or remove
        holder.add = mAddList.get(position);

        // Get the values of the ingredient from mIngredientList
        Pair<String, String> ingredientPair = mIngredientList.get(position);

        // Set the values of the EditText to previously entered data
        if (ingredientPair != null && ingredientPair.first != null) {
            holder.addQuantityEditText.setText(ingredientPair.first, TextView.BufferType.EDITABLE);
        } else {
            // If no data exists, set to an empty String otherwise something loaded in memory will show
            holder.addQuantityEditText.setText("", TextView.BufferType.EDITABLE);
        }
        if (ingredientPair != null && ingredientPair.second != null) {
            holder.addIngredientNameEditText.setText(ingredientPair.second, TextView.BufferType.EDITABLE);
        } else {
            // If no data exists, set to an empty String otherwise something loaded in memory will show
            holder.addIngredientNameEditText.setText("", TextView.BufferType.EDITABLE);
        }

        if (ingredientPair == null) {
            holder.addQuantityEditText.requestFocus();
        }
    }

    /**
     * Retrieves the ingredient list and removes empty values
     * @return A non-empty List of ingredients or null
     */
    public List<Pair<String, String>> getCorrectedIngredientList() {
        boolean nullList = true;    // Final check for whether to return a List or null
        List<Pair<String, String>> workingList = new LinkedList<>();    // List that will be modified in the iterator and returned if nullList = false

        // Create a copy of the ingredient list
        workingList.addAll(mIngredientList);

        // Iterate through and ensure at least one Pair has a non-null value
        for (Pair<String, String> ingredientPair : mIngredientList) {
            if (ingredientPair != null &&
                    ((ingredientPair.first != null && !ingredientPair.first.trim().equals("")) ||
                    (ingredientPair.second != null && !ingredientPair.second.trim().equals("")))) {
                // At least one of the Pair is not null and not an empty string
                nullList = false;
            } else {
                // Otherwise remove the item from the list to be returned
                workingList.remove(ingredientPair);
            }
        }
        if (!nullList) {
            // List contains at least one value, return list
            return workingList;
        } else {
            // List is empty, return null
            return null;
        }
    }

    /**
     * Retrieves the ingredient list and removes empty values
     * @return A non-empty List of ingredients or null
     */
    public List<Pair<String, String>> getRawIngredientList() {
        return mIngredientList;
    }

    /**
     * Sets the member mIngredientList
     * @param ingredientList List of Ingredients
     */
    public void setIngredientList(List<Pair<String, String>> ingredientList) {
        if (ingredientList == null || ingredientList.isEmpty()) {
            // If a null or empty list is added, reset the Adapter to initial conditions
            mIngredientList = new LinkedList<>();
            mAddList = new LinkedList<>();
            notifyDataSetChanged();
            return;
        }

        // Copy all values from the input list to mIngredientList (Might be faster to just set equal)
        mIngredientList = new LinkedList<>();
        mIngredientList.addAll(ingredientList);

        for (int i = 0; i < ingredientList.size(); i++) {
            // For each member in the new ingredient list, add a corresponding entry in mAddList
            mAddList.add(true);

            // Set the value to false to allow for user input
            mAddList.set(i, false);
        }

//        // Add a null ingredient to allow for additional input
//        addIngredient();

        // Notify Adapter of change in data
        notifyDataSetChanged();
    }

    public void setIngredientListWithoutNotify(List<Pair<String, String>> ingredientList) {
        if (ingredientList == null || ingredientList.isEmpty()) {
            // If a null or empty list is added, reset the Adapter to initial conditions
            mIngredientList = new LinkedList<>();
            mAddList = new LinkedList<>();
//            addIngredient();
            notifyDataSetChanged();
            return;
        }

        // Copy all values from the input list to mIngredientList (Might be faster to just set equal)
        mIngredientList = new LinkedList<>();
        mIngredientList.addAll(ingredientList);

//        for (int i = 0; i < ingredientList.size(); i++) {
//            // For each member in the new ingredient list, add a corresponding entry in mAddList
//            mAddList.add(true);
//
//            // Set the value to false to allow for user input
//            mAddList.set(i, false);
//        }

        // Add a null ingredient to allow for additional input
//        addIngredient();
    }

    @Override
    public int getItemCount() {
        return mIngredientList.size();
    }

    /**
     * Add an ingredient to mIngredientList
     */
    public void addIngredient() {
        // Add new placeholder to the mIngredientList
        mIngredientList.add(null);

        // Set the action of the button in the ViewHolder to "add"
        mAddList.add(true);

        // Notify adapter of the change
//        notifyItemChanged(mIngredientList.size() - 1);
        notifyItemInserted(mIngredientList.size() - 1);

        // Set the focus to the quantity EditText of the newly added item
        AddIngredientViewHolder viewHolder =
                (AddIngredientViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mIngredientList.size() - 1);

        test();
    }

    private void test() {
        for (int i = 0; i < mIngredientList.size(); i++) {
            Log.d(LOG_TAG, "This fucking thing: " + mRecyclerView.findViewHolderForAdapterPosition(i));
        }
    }

    /**
     * Remove an ingredient from mIngredientList
     * @param position Position of the item to be removed
     */
    void deleteIngredient(int position) {
        // Remove the ingredient from mIngredientList
        mIngredientList.remove(position);

        // Remove the button value from mAddList
        mAddList.remove(position);

        // Notify the Adapter of the change
        notifyItemRemoved(position);
    }

    interface OnStartDragListener {
        /**
         * Called when a view is requesting a start of a drag
         *
         * @param holder The holder of the view to drag
         */
        void onStartDrag(AddIngredientViewHolder holder);
    }

    public class AddIngredientViewHolder extends RecyclerView.ViewHolder {
        /** Member Variables **/
        boolean add;

        // ButterKnife Bound Views
        @BindView(R.id.list_add_ingredient_quantity_edit_text) EditText addQuantityEditText;
        @BindView(R.id.list_add_ingredient_name_edit_text) AutoCompleteTextView addIngredientNameEditText;
        @BindView(R.id.list_add_ingredient_touchpad) ImageView addIngredientTouchpad;

        @OnTouch(R.id.list_add_ingredient_touchpad)
        public boolean onTouch(View v, MotionEvent event) {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragListener.onStartDrag(this);
                return true;
            }
            return false;
        }

        /**
         * Saves the text to the mIngredientList as the user types in the quantity EditText field
         * @param text User-entered text
         */
        @OnTextChanged(R.id.list_add_ingredient_quantity_edit_text)
        void onQuantityChanged(CharSequence text) {
            // Get the adapter's position
            int position = getAdapterPosition();

            // Get the text entered as well as the text from the ingredient EditText
            String quantity = text.toString();
            String ingredient = addIngredientNameEditText.getText().toString();

            // Set the Pair in the mIngredientList
            mIngredientList.set(position, new Pair<>(quantity, ingredient));
        }

        /**
         * Saves the text to the mIngredientList as the user types in the ingredient name EditText field
         * @param text User-entered text
         */
        @OnTextChanged(R.id.list_add_ingredient_name_edit_text)
        void onNameChanged(CharSequence text) {
            Uri ingredientUri = IngredientEntry.CONTENT_URI;
            String[] projection = new String[] {"DISTINCT " + IngredientEntry.COLUMN_INGREDIENT_NAME};
            String selection = "instr(" + IngredientEntry.COLUMN_INGREDIENT_NAME + ", '" + text + "')"; //GROUP BY " + IngredientEntry.COLUMN_INGREDIENT_NAME;
            String[] selectionArgs = new String[] {text + "'"};
            String sortOrder = IngredientEntry.COLUMN_INGREDIENT_NAME + " ASC";

            Cursor cursor = mContext.getContentResolver().query(
                    ingredientUri,
                    projection,
                    selection,
                    null,
                    sortOrder
            );

            String[] resultsArray = new String[cursor.getCount()];
            if (cursor.moveToFirst()) {
                for (int i = 0; i < resultsArray.length; i++) {
                    resultsArray[i] = cursor.getString(0);
                    cursor.moveToNext();
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext, android.R.layout.simple_dropdown_item_1line, resultsArray);
            addIngredientNameEditText.setAdapter(adapter);

            // Get the adapter's position
            int position = getAdapterPosition();

            // Get the text entered as well as the text from the quantity EditText
            String quantity = addQuantityEditText.getText().toString();
            String ingredient = text.toString();

            // Set the Pair in the mIngredientList
            mIngredientList.set(position, new Pair<>(quantity, ingredient));
        }

        /**
         * Capture the "enter" button from the IME and focus on the next EditText field
         * @param actionId Id of the button that was pressed on the IME
         * @return true if action captured, false otherwise
         */
        @OnEditorAction(R.id.list_add_ingredient_quantity_edit_text)
        protected boolean onEditorAction(int actionId) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // If action pressed matches the "enter" button, focus on the next EditText field
                addIngredientNameEditText.requestFocus();
                return true;
            }
            return false;
        }

        /**
         * Capture the "enter" button from the IME, create a new ingredient list item, and focus on
         * the next EditText field
         * @param actionId Id of the button that was pressed on the IME
         * @return true if action captured, false otherwise
         */
        @OnEditorAction(R.id.list_add_ingredient_name_edit_text)
        protected boolean onFinishIngredientName(int actionId) {

            int position = getAdapterPosition();
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // If action pressed matches the "enter" button, add a new ingredient
                addIngredient();

                // Get the ViewHolder of the newly created ingredient
//                AddIngredientViewHolder viewHolder =
//                        (AddIngredientViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position + 1);

                // Set the resources of the new ViewHolder's layout to allow for input from the user
//                viewHolder.addQuantityEditText.setVisibility(View.VISIBLE);
//                viewHolder.addIngredientNameEditText.setVisibility(View.VISIBLE);

                // Set action for the button to remove
//                mAddList.set(position + 1, false);

                // Focus on the quantity EditText
//                viewHolder.addQuantityEditText.requestFocus();
                return true;
            }
            return false;
        }

        AddIngredientViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            // Work around for allowing multiple lines of text to show in the EditText fields while
            // still capturing the "enter" action
            addQuantityEditText.setHorizontallyScrolling(false);
            addQuantityEditText.setMaxLines(3);
            addIngredientNameEditText.setHorizontallyScrolling(false);
            addIngredientNameEditText.setMaxLines(3);

        }
    }
}
