package project.hnoct.kitchen.ui.adapter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;

import java.util.List;

import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 4/27/2017.
 */

public class RecipeItemAnimator extends DefaultItemAnimator {
    // Constants
    private static final String LOG_TAG = RecipeItemAnimator.class.getSimpleName();
    private AccelerateInterpolator ACCELERATE_INTERPOLATOR = new AccelerateInterpolator();
    private AccelerateDecelerateInterpolator ACCELERATE_DECELERATE_INTERPOLATOR = new AccelerateDecelerateInterpolator();

    // Member Variables
    private Context mContext;
    private RecipeAnimatorListener mListener;

    public RecipeItemAnimator(Context context) {
        mContext = context;
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
        // Allow re-use of ViewHolders
        return true;
    }

    @NonNull
    @Override
    public ItemHolderInfo recordPreLayoutInformation(@NonNull RecyclerView.State state,
                                                     @NonNull RecyclerView.ViewHolder viewHolder,
                                                     int changeFlags,
                                                     @NonNull List<Object> payloads) {

        // Check what kind of action is being performed
        if (changeFlags == FLAG_CHANGED) {
            // If item is being modified, check the tag
            for (Object payload : payloads) {
                if (payload instanceof String) {
                    // Instantiate a new RecipeItemHolderInfo and pass the payload as a String
                    return new RecipeItemHolderInfo((String) payload);
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

            switch (holderInfo.changeFlag) {
                case AdapterRecipe.ACTION_FAVORITE: {
                    animateFavorite(newRecipeHolder);
                    break;
                }
                case AdapterRecipe.ACTION_UNFAVORITE: {
                    animateUnfavorite(newRecipeHolder);
                    break;
                }
            }
        }

//        dispatchAnimationFinished(newHolder);
        return false;
    }

    /**
     * Animates favorite button to show a yellow star
     * @param holder ViewHolder who's favorite button was clicked
     */
    private void animateFavorite(final AdapterRecipe.RecipeViewHolder holder) {
        // Initialize the AnimatorSet so that the animations can play simultaneously
        AnimatorSet animSet = new AnimatorSet();

        // Scale the star's x-value
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(holder.favoriteButtonOn, "scaleX", 0.1f, 1f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        // Scale the star's y-value
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(holder.favoriteButtonOn, "scaleY", 0.1f, 1f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        // Set animations to play together
        animSet.playTogether(scaleXAnim, scaleYAnim);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Set the yellow star to visible
                holder.favoriteButtonOn.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Change the favorite status of the item in the database
                Utilities.setRecipeFavorite(mContext, (long) holder.itemView.getTag());
            }
        });

        animSet.start();
    }

    /**
     * Animates favorite button to remove yellow star
     * @see #animateFavorite
     * @param holder ViewHolder who's favorite button was clicked
     */
    private void animateUnfavorite(final AdapterRecipe.RecipeViewHolder holder) {
        AnimatorSet animSet = new AnimatorSet();

        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(holder.favoriteButtonOn, "scaleX", 1f, 0.1f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(holder.favoriteButtonOn, "scaleY", 1f, 0.1f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(ACCELERATE_INTERPOLATOR);

        animSet.playTogether(scaleXAnim, scaleYAnim);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                holder.favoriteButtonOn.setVisibility(View.INVISIBLE);
                Utilities.setRecipeFavorite(mContext, (long) holder.itemView.getTag());
            }
        });

        animSet.start();
    }

    private void animateDetails(AdapterRecipe.RecipeViewHolder holder) {
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
            }
        });
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (holder.getItemViewType() == AdapterRecipe.RECIPE_VIEW_DETAIL ||
                holder.getItemViewType() == AdapterRecipe.RECIPE_VIEW_DETAIL_LAST) {

            animateDetails((AdapterRecipe.RecipeViewHolder) holder);
            return false;
        }

        dispatchAddFinished(holder);
        return false;
    }

    private class RecipeItemHolderInfo extends ItemHolderInfo {
        String changeFlag;

        RecipeItemHolderInfo(String changeFlag) {
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
