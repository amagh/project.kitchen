package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import butterknife.OnTouch;
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
    private OnStartDragListener mDragListener;

    public AddDirectionAdapter(Context context, OnStartDragListener dragListener) {
        // Initialize member variables
        mContext = context;
        mDirectionList = new LinkedList<>();
        mDragListener = dragListener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        // Set the member variable equal to the RecyclerView the Adapter is attached to
        RecyclerView mRecyclerView = recyclerView;
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
        if (direction!= null) {
            // Set the text to the retrieved value from mDirectionList
            holder.directionEditText.setText(direction, TextView.BufferType.EDITABLE);
        } else {
            // If no data exists for this position, set it to an empty String so prevent text from
            // memory being loaded
            holder.directionEditText.setText("", TextView.BufferType.EDITABLE);
            holder.directionEditText.requestFocus();
        }

        // Set the text indicating what step the user is inputting
        holder.directionStepText.setText(mContext.getString(R.string.direction_step, position + 1));
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
    public void addDirection() {
        // Add null value to mDirectionList
        mDirectionList.add(null);

        // Notify Adapter of the change in data
        notifyItemInserted(mDirectionList.size() - 1);
    }

    /**
     * Remove a direction at a given position in mDirectionList
     * @param position Position of the item to be removed
     */
    public void removeDirection(int position) {
        // Remove the item from mDirectionList
        mDirectionList.remove(position);

        // Notify the Adapter of the change
        notifyItemRemoved(position);
    }

    /**
     * Returns the user-added directions
     * @return All non-null & non-empty values in List form
     */
    public List<String> getCorrectedDirectionList() {
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

    public List<String> getRawDirectionList() {
        return mDirectionList;
    }

    /**
     * Set mDirectionList to the inputString
     * @param directionList List of Directions to set as mDirectionList
     */
    public void setDirectionList(List<String> directionList) {
        if (directionList == null || directionList.isEmpty()) {
            // If a null or empty list is added, reset the Adapter to initial conditions
            mDirectionList = new LinkedList<>();
            notifyDataSetChanged();
            return;
        }

        // Set mDirectionList equal to the input direction list
        mDirectionList = new LinkedList<>();
        mDirectionList.addAll(directionList);

        // Notify Adapter of change in data
        notifyDataSetChanged();
    }

    public void setDirectionListWithoutNotify(List<String> directionList) {
        if (directionList == null || directionList.isEmpty()) {
            // If a null or empty list is added, reset the Adapter to initial conditions
            mDirectionList = new LinkedList<>();
            notifyDataSetChanged();
            return;
        }

        // Set mDirectionList equal to the input direction list
        mDirectionList = new LinkedList<>();
        mDirectionList.addAll(directionList);
    }

    interface OnStartDragListener {
        void onStartDrag(AddDirectionViewHolder viewHolder);
    }

    class AddDirectionViewHolder extends RecyclerView.ViewHolder {
        // ButterKnife bound views
        @BindView(R.id.list_add_direction_step_text) TextView directionStepText;
        @BindView(R.id.list_add_direction_edit_text) EditText directionEditText;
        @BindView(R.id.list_add_direction_touchpad) ImageView addDirectionTouchpad;

        @OnTouch(R.id.list_add_direction_touchpad)
        boolean onTouch(View view, MotionEvent event) {
            if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                mDragListener.onStartDrag(this);
                return true;
            }
            return false;
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
