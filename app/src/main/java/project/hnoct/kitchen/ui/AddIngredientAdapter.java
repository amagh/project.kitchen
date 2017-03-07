package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
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

    public AddIngredientAdapter(Context context) {
        mContext = context;
        mIngredientList = new LinkedList<>();
        mAddList = new LinkedList<>();
        addIngredient();
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

        if (ingredientPair.first != null) {
            holder.addQuantityEditText.setText(ingredientPair.first, TextView.BufferType.EDITABLE);
        }
        if (ingredientPair.second != null) {
            holder.addIngredientNameEditText.setText(ingredientPair.second, TextView.BufferType.EDITABLE);
        }
    }

    @Override
    public int getItemCount() {
        return mIngredientList.size();
    }

    private void addIngredient() {
        mIngredientList.add(new Pair<String, String>(null, null));
        mAddList.add(true);
        Toast.makeText(mContext, "New ingredient added!", Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    private void deleteIngredient(int position) {
        mIngredientList.remove(position);
        mAddList.remove(position);
        Toast.makeText(mContext, "Ingredient removed!", Toast.LENGTH_SHORT).show();
        notifyDataSetChanged();
    }

    public class AddIngredientViewHolder extends RecyclerView.ViewHolder {
        /** Member Variables **/
        boolean add;

        // ButterKnife Bound Views
        @BindView(R.id.list_add_ingredient_quantity_edit_text) EditText addQuantityEditText;
        @BindView(R.id.list_add_ingredient_name_edit_text) EditText addIngredientNameEditText;
        @BindView(R.id.list_add_ingredient_button) ImageView addIngredientButton;

//        @OnFocusChange(R.id.list_add_ingredient_quantity_edit_text)
//        void onQuantityChanged(boolean inFocus) {
//            String quantity = addQuantityEditText.getText().toString();
//            String ingredient = addIngredientNameEditText.getText().toString();
//        }
//
//        @OnFocusChange(R.id.list_add_ingredient_name_edit_text)
//        void onNameChanged(boolean inFocus) {
//            String quantity = addQuantityEditText.getText().toString();
//            String ingredient = addIngredientNameEditText.getText().toString();
//        }

        @OnClick(R.id.list_add_ingredient_button)
        void onClick() {
            if (add) {
                addIngredient();
                addIngredientButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
                addQuantityEditText.setVisibility(View.VISIBLE);
                addIngredientNameEditText.setVisibility(View.VISIBLE);
                Log.d("TEST", "Size: " + mAddList.size() + " | Position: " + getAdapterPosition());
//                mAddList.set(getAdapterPosition(), add = false);
            } else {
                deleteIngredient(getAdapterPosition());
                addIngredientButton.setImageResource(R.drawable.ic_menu_add_custom);
                addQuantityEditText.setVisibility(View.GONE);
                addIngredientNameEditText.setVisibility(View.GONE);
                mAddList.set(getAdapterPosition(), add = true);
            }
        }

        AddIngredientViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            Log.d("TEST2", "Position: " + getAdapterPosition());
        }
    }
}
