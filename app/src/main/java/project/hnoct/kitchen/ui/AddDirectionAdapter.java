package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 3/7/2017.
 */

public class AddDirectionAdapter extends RecyclerView.Adapter<AddDirectionAdapter.AddDirectionViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = AddDirectionAdapter.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;               // Interface to global Context
    private List<String> mDirectionList;    // List of all directions
    private List<Boolean> mAddList;         // Holds the boolean value for what the button in the ViewHolder should do: add or remove
    private RecyclerView mRecyclerView;     // References the Recycler containing this adapter for use in searching other ViewHolders

    public AddDirectionAdapter(Context context) {
        // Initialize member variables
        mContext = context;
        mDirectionList = new LinkedList<>();
        mAddList = new LinkedList<>();

        // Add a new direction to allow for user input
        addDirection();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        // Set the member variable equal to the RecyclerView the Adapter is attached to
        mRecyclerView = recyclerView;
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public AddDirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout to be used for adding directions
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_add_direction, parent, false);

        // Return a new AddDirectionViewHolder containing the inflated view
        return new AddDirectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AddDirectionViewHolder holder, int position) {
        // Get the stored direction from mDirectionList
        String direction  = mDirectionList.get(position);

        // Check whether the resources should be set to allow for user input or add another direction
        boolean add = mAddList.get(position);

        if (direction!= null) {
            // Set the text to the retrieved value from mDirectionList
            holder.directionEditText.setText(direction, TextView.BufferType.EDITABLE);
        } else {
            // If no data exists for this position, set it to an empty String so prevent text from
            // memory being loaded
            holder.directionEditText.setText("", TextView.BufferType.EDITABLE);
        }

        // Set the text indicating what step the user is inputting
        holder.directionStepText.setText(mContext.getString(R.string.direction_step, position + 1));

        // Check whether the list item should be ready to accept user input or add another direction
        if (add) {
            // Set layout resources to allow for adding another direction
            holder.directionStepText.setVisibility(View.GONE);
            holder.directionEditText.setVisibility(View.GONE);
            holder.addDirectionButton.setImageResource(R.drawable.ic_menu_add_custom);
        } else {
            // Set layout resources to accept user input
            holder.directionStepText.setVisibility(View.VISIBLE);
            holder.directionEditText.setVisibility(View.VISIBLE);
            holder.addDirectionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
        }
    }

    @Override
    public int getItemCount() {
        if (mDirectionList != null) {
            return mDirectionList.size();
        } else {
            return 0;
        }
    }

    /**
     * Adds a direction
     */
    private void addDirection() {
        // Add null value to mDirectionList
        mDirectionList.add(null);

        // Set the action of the newly created list item to allow for adding another direction
        mAddList.add(true);

        // Notify Adapter of the change in data
        notifyItemChanged(mDirectionList.size() - 1);
    }

    /**
     * Remove a direction at a given position in mDirectionList
     * @param position Position of the item to be removed
     */
    private void removeDirection(int position) {
        // Remove the item from mDirectionList
        mDirectionList.remove(position);

        // Remove the button value from mAddList
        mAddList.remove(position);

        // Notify the Adapter of the change
        notifyDataSetChanged();
    }

    /**
     * Returns the user-added directions
     * @return All non-null & non-empty values in List form
     */
    public List<String> getDirectionList() {
        // Final check for whether to return a null List or the values contained
        boolean nullList = true;

        // Create a new list to prevent ConcurrentModificationError and copy mDirectionList values
        List<String> workingList = new LinkedList<>();
        workingList.addAll(mDirectionList);

        // Iterate through all values in mDirectionList
        for (String direction : mDirectionList) {
            if (direction != null && !direction.trim().equals("")) {
                // If even one value exists, set the final check to return the value
                nullList = false;
            } else {
                // If the entry is empty or null, then remove the value from the List
                workingList.remove(direction);
            }
        }

        if (!nullList) {
            // If values exist, return the List
            return workingList;
        } else {
            return null;
        }
    }

    /**
     * Set mDirectionList to the inputString
     * @param directionList List of Directions to set as mDirectionList
     */
    public void setDirectionList(List<String> directionList) {
        // Set mDirectionList equal to the input direction list
        mDirectionList = new LinkedList<>();
        mDirectionList.addAll(directionList);

        for (int i = 0; i < mDirectionList.size(); i++) {
            // For each value in the new direction list, add a button value
            mAddList.add(true);

            // Set the button value to false to ensure that the data is shown
            mAddList.set(i, false);
        }

        // Add a null value to allow for additional input
        addDirection();
    }

    class AddDirectionViewHolder extends RecyclerView.ViewHolder {
        // ButterKnife bound views
        @BindView(R.id.list_add_direction_step_text) TextView directionStepText;
        @BindView(R.id.list_add_direction_edit_text) EditText directionEditText;
        @BindView(R.id.list_add_direction_button) ImageView addDirectionButton;

        @OnClick(R.id.list_add_direction_button)
        void onAddDirection() {
            // Get the Adapter's position
            int position = getAdapterPosition();

            // Check if layout should allow for addition of another direction or user input
            boolean add = mAddList.get(position);

            if (add) {
                // Set the layout to receive user-input
                directionStepText.setVisibility(View.VISIBLE);
                directionEditText.setVisibility(View.VISIBLE);
                addDirectionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);

                // Add another direction to be ready set to allow for addition of another direction
                addDirection();

                // Set the button value for the newly added direction
                mAddList.set(position, false);

                // Set the focus on the EditText and open the virtual keyboard
                directionEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(directionEditText, InputMethodManager.SHOW_IMPLICIT);
            } else {
                // Set the layout to allow for addition of another direction
                directionStepText.setVisibility(View.GONE);
                directionEditText.setVisibility(View.GONE);
                addDirectionButton.setImageResource(R.drawable.ic_menu_add_custom);

                // Remove the direction from mDirectionList
                removeDirection(position);
            }
        }

        @OnTextChanged(R.id.list_add_direction_edit_text)
        void directionTextChanged(CharSequence text) {
            // Get the adapter's position
            int position = getAdapterPosition();

            // Set the direction in mDirectionList
            mDirectionList.set(position, text.toString());
        }

        @OnEditorAction(R.id.list_add_direction_edit_text)
        protected boolean onFinishDirection(int actionId) {
            Log.d(LOG_TAG, "actionId: " + actionId);
            // Get the Adapter's position
            int position = getAdapterPosition();
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // Add a new direction
                addDirection();

                // Get the ViewHolder of the newly created item
                AddDirectionViewHolder viewHolder =
                        (AddDirectionViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position + 1);

                // Set the view of the newly created direction item to be ready for user input
                viewHolder.directionStepText.setVisibility(View.VISIBLE);
                viewHolder.directionEditText.setVisibility(View.VISIBLE);
                viewHolder.addDirectionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);

                mAddList.set(position + 1, false);

                // Set the focus on the direction EditText of the new item
                viewHolder.directionEditText.requestFocus();
                return true;
            }
            return false;
        }

        AddDirectionViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            directionEditText.setHorizontallyScrolling(false);
            directionEditText.setMaxLines(99);
        }
    }
}
