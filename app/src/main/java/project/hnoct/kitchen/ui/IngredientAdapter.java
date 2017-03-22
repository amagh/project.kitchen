package project.hnoct.kitchen.ui;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 2/21/2017.
 */

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder> {
    /** Constants **/

    /** Member Variables **/
    Context mContext;                   // Interface for global context
    Cursor mCursor;
    ContentResolver mContentResolver;   // Reference to ContentResolver
    List<String> mIngredientList;       // TODO: For storing ingredients that need to be added to shopping list

    public IngredientAdapter(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public IngredientViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_ingredient, parent, false);
        view.setFocusable(true);

        return new IngredientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(IngredientViewHolder holder, int position) {
        // Move Cursor to correct position
        mCursor.moveToPosition(position);

        // Retrieve ingredient information
        String quantity = mCursor.getString(RecipeContract.LinkIngredientEntry.IDX_LINK_QUANTITY);
        String ingredient = mCursor.getString(RecipeContract.LinkIngredientEntry.IDX_INGREDIENT_NAME);

        // Check to see if ingredient is a header (headers are notated with a ":")
        Pattern pattern = Pattern.compile(".*:");
        Matcher match = pattern.matcher(ingredient);

        if (match.matches()) {
            // Ingredient is not an ingredient, but a header, so bold the text to make it stand out and remove the colon
            ingredient = ingredient.substring(0, ingredient.length() -1);
            holder.ingredientNameText.setText(ingredient);
            holder.ingredientNameText.setTypeface(holder.ingredientNameText.getTypeface(), Typeface.BOLD);
        } else {
            holder.ingredientNameText.setText(ingredient);
        }

        // Set the view parameters
        holder.quantityText.setText(Utilities.abbreviateMeasurements(quantity));
        holder.addIngredientButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public int getItemCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
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

            // Notify the Adapter that the data has changed
            notifyDataSetChanged();
        }
        return mCursor;
    }

    public class IngredientViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_ingredient_quantity) TextView quantityText;
        @BindView(R.id.list_ingredient_name) TextView ingredientNameText;
        @BindView(R.id.list_ingredient_add_button) ImageView addIngredientButton;

        @OnClick(R.id.list_ingredient_add_button) void addIngredientToList() {
            String ingredientName = (String) ingredientNameText.getText();
            if (mIngredientList == null) {
                mIngredientList = new LinkedList<>();
            }

            if (mIngredientList.contains(ingredientName)) {
                mIngredientList.remove(ingredientName);
            } else if (!mIngredientList.contains(ingredientName)) {
                mIngredientList.add(ingredientName);
            }
        }

        public IngredientViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
