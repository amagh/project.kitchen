package project.hnoct.kitchen.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;

import java.util.List;

/**
 * Created by hnoct on 4/27/2017.
 */

public class RecipeItemAnimator extends DefaultItemAnimator {
    // Constants
    private static final String LOG_TAG = RecipeItemAnimator.class.getSimpleName();
    private AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    // Member Variables
    private RecipeAnimatorListener mListener;

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {

//        Log.v(LOG_TAG, "changeFlags: " + changeFlags);
        if (changeFlags == FLAG_CHANGED) {
            for (Object payload : payloads) {
                if (payload instanceof Integer) {
                    if (payload == AdapterRecipe.ACTION_OPEN_DETAILS) {
                        return new RecipeItemHolderInfo((Integer) payload);
                    }
                }
            }
        }
        return super.recordPreLayoutInformation(state, viewHolder, changeFlags, payloads);
    }

    @Override
    public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder,
                                 @NonNull RecyclerView.ViewHolder newHolder,
                                 @NonNull ItemHolderInfo preInfo,
                                 @NonNull ItemHolderInfo postInfo) {

        if (preInfo instanceof  RecipeItemHolderInfo) {
            RecipeItemHolderInfo holderInfo = (RecipeItemHolderInfo) preInfo;
            AdapterRecipe.RecipeViewHolder oldRecipeHolder = (AdapterRecipe.RecipeViewHolder) oldHolder;
            AdapterRecipe.RecipeViewHolder newRecipeHolder = (AdapterRecipe.RecipeViewHolder) newHolder;

//            Log.d(LOG_TAG, oldRecipeHolder.itemView.getHeight() + " test");
//            Log.d(LOG_TAG, oldRecipeHolder.itemView.getWidth() + " test");
        }

        return false;
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (holder.getItemViewType() == AdapterRecipe.RECIPE_VIEW_DETAIL) {
            holder.itemView.setPivotY(0);

            ObjectAnimator animXStretch = ObjectAnimator.ofFloat(holder.itemView, "scaleX", 0.1f, 1f);
            animXStretch.setDuration(300);
            animXStretch.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);

            ObjectAnimator pauseYStretch = ObjectAnimator.ofFloat(holder.itemView, "scaleY", 0.05f, 0.05f);
            pauseYStretch.setDuration(300);

            ObjectAnimator animYStretch = ObjectAnimator.ofFloat(holder.itemView, "scaleY", 0.05f, 1f);
            animYStretch.setDuration(300);
            animYStretch.setInterpolator(ACCELERATE_DECELERATE_INTERPOLATOR);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(animXStretch).with(pauseYStretch).before(animYStretch);
            animatorSet.start();
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mListener != null) {
                        mListener.onFinishAnimateDetail();
                    }
                    super.onAnimationEnd(animation);
                }
            });
        }

        return super.animateAdd(holder);
    }

    private class RecipeItemHolderInfo extends ItemHolderInfo {
        Integer changeFlag;

        RecipeItemHolderInfo(Integer changeFlag) {
            this.changeFlag = changeFlag;
        }
    }

    public interface RecipeAnimatorListener {
        void onFinishAnimateDetail();
    }

    public void setRecipeAnimatorListener(RecipeAnimatorListener listener) {
        mListener = listener;
    }
}
