package project.hnoct.kitchen.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by hnoct on 2/26/2017.
 */

public class NutritionAdapter extends RecyclerView.Adapter<NutritionAdapter.NutritionViewHolder> {
    @Override
    public NutritionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(NutritionViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class NutritionViewHolder extends RecyclerView.ViewHolder {
        public NutritionViewHolder(View itemView) {
            super(itemView);
        }
    }
}
