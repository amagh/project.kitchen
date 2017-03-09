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

import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 3/7/2017.
 */

public class AddDirectionAdapter extends RecyclerView.Adapter<AddDirectionAdapter.AddDirectionViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = AddDirectionAdapter.class.getSimpleName();

    /** Member Variables **/
    private Context mContext;
    private List<String> mDirectionList;
    private List<Boolean> mAddList;

    public AddDirectionAdapter(Context context) {
        mContext = context;
        mDirectionList = new LinkedList<>();
        mAddList = new LinkedList<>();
        addDirection();
    }

    @Override
    public AddDirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_add_direction, parent, false);
        return new AddDirectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AddDirectionViewHolder holder, int position) {
        String direction  = mDirectionList.get(position);
        boolean add = mAddList.get(position);

        if (direction!= null) {
            holder.directionEditText.setText(direction, TextView.BufferType.EDITABLE);
        } else {
            holder.directionEditText.setText("", TextView.BufferType.EDITABLE);
        }

        holder.directionStepText.setText(mContext.getString(R.string.direction_step, position + 1));

        if (add) {
            holder.directionStepText.setVisibility(View.GONE);
            holder.directionEditText.setVisibility(View.GONE);
            holder.addDirectionButton.setImageResource(R.drawable.ic_menu_add_custom);
        } else {
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

    private void addDirection() {
        mDirectionList.add(null);
        mAddList.add(true);
        for (int i = 0; i < mDirectionList.size(); i++) {
            String direction = mDirectionList.get(i);
            Log.d(LOG_TAG, "Position: " + i + ": " + direction);
        }
        notifyDataSetChanged();
    }

    private void removeDirection(int position) {
        mDirectionList.remove(position);
        mAddList.remove(position);
        for (int i = 0; i < mDirectionList.size(); i++) {
            String direction = mDirectionList.get(i);
            Log.d(LOG_TAG, "Position: " + i + ": " + direction);
        }
        notifyDataSetChanged();
    }

    public List<String> getDirectionList() {
        boolean nullList = true;

        List<String> workingList = new LinkedList<>();
        workingList.addAll(mDirectionList);

        for (String direction : mDirectionList) {
            if (direction != null && !direction.trim().equals("")) {
                nullList = false;
            } else {
                workingList.remove(direction);
            }
        }

        if (!nullList) {
            return workingList;
        } else {
            return null;
        }
    }

    public void setDirectionList(List<String> directionList) {
        mDirectionList = directionList;

        for (int i = 0; i < mDirectionList.size(); i++) {
            mAddList.add(true);
            mAddList.set(i, false);
        }

        addDirection();
    }

    public class AddDirectionViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.list_add_direction_step_text) TextView directionStepText;
        @BindView(R.id.list_add_direction_edit_text) EditText directionEditText;
        @BindView(R.id.list_add_direction_button) ImageView addDirectionButton;

        @OnClick(R.id.list_add_direction_button)
        void onAddDirection() {
            int position = getAdapterPosition();
            boolean add = mAddList.get(position);
            if (add) {
                directionStepText.setVisibility(View.VISIBLE);
                directionEditText.setVisibility(View.VISIBLE);
                addDirectionButton.setImageResource(R.drawable.ic_menu_close_clear_cancel);
                addDirection();
                mAddList.set(position, false);
            } else {
                directionStepText.setVisibility(View.GONE);
                directionEditText.setVisibility(View.GONE);
                addDirectionButton.setImageResource(R.drawable.ic_menu_add_custom);
                removeDirection(position);
            }
        }

        @OnTextChanged(R.id.list_add_direction_edit_text)
        void directionTextChanged(CharSequence text) {
            int position = getAdapterPosition();
            mDirectionList.set(position, text.toString());
        }

        AddDirectionViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
