package project.hnoct.kitchen.view;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;

/**
 * Created by hnoct on 4/27/2017.
 */

public class StaggeredGridLayoutManagerWithSmoothScroll extends StaggeredGridLayoutManager {

    public StaggeredGridLayoutManagerWithSmoothScroll(int spanCount, int orientation) {
        super(spanCount, orientation);
    }

    public StaggeredGridLayoutManagerWithSmoothScroll(Context context, AttributeSet attrs,
                                                      int defStyleAttrs, int defStyleRes) {
        super (context, attrs, defStyleAttrs, defStyleRes);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
                                       int position) {

        RecyclerView.SmoothScroller smoothScroller =
                new TopSnappedSmoothScroller(recyclerView.getContext());
        smoothScroller.setTargetPosition(position);
        startSmoothScroll(smoothScroller);
    }

    private class TopSnappedSmoothScroller extends LinearSmoothScroller {
        public TopSnappedSmoothScroller(Context context) {
            super(context);
        }

        @Nullable
        @Override
        public PointF computeScrollVectorForPosition(int targetPosition) {
            return StaggeredGridLayoutManagerWithSmoothScroll.this
                    .computeScrollVectorForPosition(targetPosition);
        }

        @Override
        protected int getVerticalSnapPreference() {
            return SNAP_TO_START;
        }
    }
}
