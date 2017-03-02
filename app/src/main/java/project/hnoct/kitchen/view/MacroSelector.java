package project.hnoct.kitchen.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 2/28/2017.
 * TODO: Create the sliding indicator for altering the values {@see ahref=https://github.com/navasmdc/MaterialDesignLibrary/blob/master/MaterialDesignLibrary/MaterialDesign/src/main/java/com/gc/materialdesign/views/Slider.java}
 * IDEA: Draw an invisible ring with a line from the center to a point on the ring. Make the point touchable and rotate around the axis. Use the line connecting to the point to calculate the angle. Use angle to set new values.
 */

public class MacroSelector extends View {
    /** Constants **/
    private static final String LOG_TAG = MacroSelector.class.getSimpleName();

    /** Member Variables **/
    private Paint mPaint;
    private Paint mShadowPaint;
    private RectF mBounds;
    private RectF mShadowBounds;

    private float mFat;
    private float mCarb;
    private float mProtein;

    private boolean mShowText;

    public MacroSelector(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!this.isInEditMode() && Build.VERSION.SDK_INT >= 11) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        // Instantiate the Paint objects to be used in the onDraw
        // Used for painting the pie chart
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.FILL);

        // Used for painting the shadow of the chart
        mShadowPaint = new Paint(0);
        mShadowPaint.setColor(getResources().getColor(R.color.tertiary_grey_text));
        mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));

        // Obtain attributes if they are set in the xml
        TypedArray array = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.MacroSelector,
                0, 0);

        try {
            mShowText = array.getBoolean(R.styleable.MacroSelector_showText, false);
            mFat = array.getFloat(R.styleable.MacroSelector_fat, 0.25f);
            mCarb = array.getFloat(R.styleable.MacroSelector_carb, 0.5f);
            mProtein = array.getFloat(R.styleable.MacroSelector_protein, 0.25f);
        } finally {
            array.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Try for width based on minimum
        int minW = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minW, widthMeasureSpec, 1);

        // Ask for height so that pie chart can be as large as possible
        int minh = getPaddingTop() + getPaddingBottom() + MeasureSpec.getSize(w);
        int h = resolveSizeAndState(MeasureSpec.getSize(w), heightMeasureSpec, 0);

        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Set dimensions for pie chart, accounting for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        float ww = (float) w - xpad;
        float hh = (float) h - ypad;

        // Set the diameter of the pie chart to the minimum of the width and height
        float diameter = Math.min(ww, hh);

        // Set the bounds of the pie chart
        mBounds = new RectF(
                0.0f,
                0.0f,
                diameter,
                diameter
        );
        mBounds.offsetTo(getPaddingLeft(), getPaddingTop());

        // Set the bounds for the shadow
        mShadowBounds = new RectF(
                mBounds.left,
                mBounds.top + 5,
                mBounds.right + 5,
                mBounds.bottom + 5
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the shadow
        canvas.drawOval(mShadowBounds, mShadowPaint);

        // Draw the pie chart
        float segStartPoint = 0;
        int colorRed = Color.RED;
        int colorBlue = Color.BLUE;
        int colorGreen = Color.GREEN;

        mPaint.setColor(colorRed);
        canvas.drawArc(mBounds, segStartPoint, mFat * 360, true, mPaint);
        segStartPoint += mFat * 360;
        mPaint.setColor(colorBlue);
        canvas.drawArc(mBounds, segStartPoint, mCarb * 360, true, mPaint);
        segStartPoint += (mCarb * 360);
        mPaint.setColor(colorGreen);
        canvas.drawArc(mBounds, segStartPoint, mProtein * 360, true, mPaint);
    }

    public void setNutrientValues(float fat, float carb, float protein) {
        if (fat + carb + protein == 1) {
            mFat = fat;
            mCarb = carb;
            mProtein = protein;
            invalidate();
        } else {
            Log.e(LOG_TAG, "Nutrient values do not add up to 1");
        }
    }

    public boolean isShowText() {
        return mShowText;
    }

    public void setShowText(boolean showText) {
        mShowText = showText;
        invalidate();
        requestLayout();
    }

    private boolean validateNutrientValues() {
        if (mFat + mCarb + mProtein == 1) {
            return true;
        } else {
            return false;
        }
    }
}
