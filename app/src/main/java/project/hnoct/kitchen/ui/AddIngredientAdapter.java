package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 3/5/2017.
 */

public class AddIngredientAdapter extends RecyclerView.Adapter<AddIngredientAdapter.AddIngredientViewHolder> {
    /** Constants **/

    /** Member Variables **/
    private Context mContext;
    private List<Pair<String, String>> mIngredientList;
    private List<Boolean> mAddList;
    private RecyclerView mRecyclerView;

    public AddIngredientAdapter(Context context) {
        mContext = context;
        mIngredientList = new LinkedList<>();
        mAddList = new LinkedList<>();
        addIngredient();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public AddIngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_add_ingredient, parent, false);
        return new AddIngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AddIngredientViewHolder holder, int position) {
        holder.add = mAddList.get(position);
        Pair<String, String> ingredientPair = mIngredientList.get(position);
        if (holder.add) {
            holder.addIngredientButton.setImageResource(R.drawable.ic_menu_add_custom);
            holder.addQuantityEditText.setVisibility(View.GONE);
            holder.addIngredientNameEditText.setVisibility(View.GONE);
        } else {
            holder.addIngredientButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
            holder.addQuantityEditText.setVisibility(View.VISIBLE);
            holder.addIngredientNameEditText.setVisibility(View.VISIBLE);
        }

        if (ingredientPair != null && ingredientPair.first != null) {
            holder.addQuantityEditText.setText(ingredientPair.first, TextView.BufferType.EDITABLE);
        }
        if (ingredientPair != null && ingredientPair.second != null) {
            holder.addIngredientNameEditText.setText(ingredientPair.second, TextView.BufferType.EDITABLE);
        }
    }

    /**
     * Retrieves the ingredient list and removes empty values
     * @return A non-empty List of ingredients or null
     */
    public List<Pair<String, String>> getIngredientList() {
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
     * Sets the member mIngredientList
     * @param ingredientList List of Ingredients
     */
    public void setIngredientList(List<Pair<String, String>> ingredientList) {
        mIngredientList = new LinkedList<>();
        mIngredientList.addAll(ingredientList);

        for (int i = 0; i < ingredientList.size(); i++) {
            mAddList.add(true);
            mAddList.set(i, false);
        }

        addIngredient();

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mIngredientList.size();
    }

    private void addIngredient() {
        mIngredientList.add(null);
        mAddList.add(true);
//        Toast.makeText(mContext, "New ingredient added!", Toast.LENGTH_SHORT).show();
        notifyItemChanged(mIngredientList.size() - 1);
    }

    private void deleteIngredient(int position) {
        mIngredientList.remove(position);
        mAddList.remove(position);
//        Toast.makeText(mContext, "Ingredient " + position + " removed!", Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    public class AddIngredientViewHolder extends RecyclerView.ViewHolder {
        /** Member Variables **/
        boolean add;

        // ButterKnife Bound Views
        @BindView(R.id.list_add_ingredient_quantity_edit_text) EditText addQuantityEditText;
        @BindView(R.id.list_add_ingredient_name_edit_text) EditText addIngredientNameEditText;
        @BindView(R.id.list_add_ingredient_button) ImageView addIngredientButton;

        @OnTextChanged(R.id.list_add_ingredient_quantity_edit_text)
        void onQuantityChanged(CharSequence text) {
            int position = getAdapterPosition();
            String quantity = text.toString();
            String ingredient = addIngredientNameEditText.getText().toString();
            mIngredientList.set(position, new Pair<>(quantity, ingredient));
        }

        @OnTextChanged(R.id.list_add_ingredient_name_edit_text)
        void onNameChanged(CharSequence text) {
            int position = getAdapterPosition();
            String quantity = addQuantityEditText.getText().toString();
            String ingredient = text.toString();
            mIngredientList.set(position, new Pair<>(quantity, ingredient));
        }

        @OnEditorAction(R.id.list_add_ingredient_quantity_edit_text)
        boolean onEditorAction(int actionId, KeyEvent event) {
            Log.d("TEST", "called it!");
            if (actionId == EditorInfo.IME_ACTION_DONE && event.getAction() == KeyEvent.ACTION_DOWN) {
                addIngredientNameEditText.requestFocus();
            }
            return true;
        }

        AddIngredientViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            addIngredientButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position;
                    if (mAddList.get(position = getAdapterPosition())) {
                        addIngredient();
                        addIngredientButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
                        addQuantityEditText.setVisibility(View.VISIBLE);
                        addIngredientNameEditText.setVisibility(View.VISIBLE);
                        mAddList.set(position, false);
                        addQuantityEditText.requestFocus();
                        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(addQuantityEditText, InputMethodManager.SHOW_IMPLICIT);
//                        Log.d("TEST", "Size: " + mAddList.size() + " | Position: " + position);
                    } else {
                        deleteIngredient(getAdapterPosition());
                        addIngredientButton.setImageResource(R.drawable.ic_menu_add_custom);
                        addQuantityEditText.setVisibility(View.GONE);
                        addIngredientNameEditText.setVisibility(View.GONE);
//                        Log.d("TEST", "Size: " + mAddList.size() + " | Position: " + position);
                    }
                }
            });
        }
    }
}
