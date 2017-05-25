package project.kitchen.ui.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;
import project.kitchen.data.Utilities;

/**
 * Created by hnoct on 2/21/2017.
 */

public class AdapterDirection extends RecyclerView.Adapter<AdapterDirection.DirectionViewHolder> {
    /** Constants **/

    /** Member Variables **/
    private Context mContext;               // Interface for global context
    private List<String> mDirectionList;    // Stores all directions that need to be loaded into views

    public AdapterDirection(Context context) {
        mContext = context;
    }

    @Override
    public AdapterDirection.DirectionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout used for direction items
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item_direction, parent, false);
        view.setFocusable(true);

        return new DirectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AdapterDirection.DirectionViewHolder holder, int position) {
        String direction = Utilities.convertToUnicodeFractionForDirections(
                mContext,
                mDirectionList.get(position)
        );
        holder.directionStepText.setText(mContext.getString(R.string.direction_step, position + 1));
        holder.directionText.setText(direction);
    }

    @Override
    public int getItemCount() {
        if (mDirectionList != null) {
            return mDirectionList.size();
        }
        return 0;
    }

    /**
     * Sets the list of directions that the Adapter will be working with
     * @param directionList List of all directions for a recipe
     */
    public void setDirectionList(List<String> directionList) {
        if (mDirectionList == null) {
            mDirectionList = directionList;
            notifyDataSetChanged();
        }
    }

    public class DirectionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        @BindView(R.id.list_direction_step_text) TextView directionStepText;
        @BindView(R.id.list_direction_text) TextView directionText;
        @BindView(R.id.list_direction_overlay) ImageView directionOverlay;

        /** Might be able to do this with touch selector instead **/
        @Override
        public void onClick(View view) {
//            int colorEnabled = mContext.getResources().getColor(R.color.direction_overlay_enabled);
//            int colorDisabled = mContext.getResources().getColor(R.color.direction_overlay_disabled);
//
//            if (!overlayEnabled) {
//                GradientDrawable gd = new GradientDrawable(
//                        GradientDrawable.Orientation.TOP_BOTTOM,
//                        new int[] {colorEnabled, colorEnabled}
//                );
//                directionOverlay.setBackground(gd);
//                overlayEnabled = true;
//            } else {
//                GradientDrawable gd = new GradientDrawable(
//                        GradientDrawable.Orientation.TOP_BOTTOM,
//                        new int[] {colorDisabled, colorDisabled}
//                );
//                directionOverlay.setBackground(gd);
//                overlayEnabled = false;
//            }
//            notifyItemChanged(getAdapterPosition());
        }

        public DirectionViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }
    }
}
