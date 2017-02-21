package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Created by hnoct on 2/20/2017.
 *
 * @see <a href="http://stackoverflow.com/questions/18813296/non-scrollable-listview-inside-scrollview">StackOverflow.com</a>
 *
 */

public class NonScrollingRecyclerView extends RecyclerView {

    /** Default constructors **/
    public NonScrollingRecyclerView(Context context) {
        super(context);
    }

    public NonScrollingRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NonScrollingRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int customHeightSpec = MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2,
                MeasureSpec.UNSPECIFIED
        );

        super.onMeasure(widthSpec, customHeightSpec);
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = getMeasuredHeight();
    }
}
