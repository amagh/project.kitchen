package project.hnoct.kitchen.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.materialdesign.Utils;

/**
 * Created by hnoct on 3/15/2017.
 */

public class SlidingAlphabeticalIndex extends LinearLayout {
    /** Constants **/
    private static String mAlphabet = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private int max = 26;
    private int min = 0;

    /** Member Variables **/
    private int viewHeight = 0;
    private int value = 0;
    private Ball ball;
    private Context mContext;
    private Map<String, Integer> mIndex = new HashMap<>();

    private OnValueChangedListener listener;
    private LetterIndicator letterIndicator;

    public SlidingAlphabeticalIndex(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Instantiate member variables
        mContext = context;
        ball = new Ball(context);
        letterIndicator = new LetterIndicator(context);

        // Add the ball as a view to SlidingAlphabeticalIndex
        addView(ball);

        // Set the View to GONE so it does not affect the other Views
        ball.setVisibility(View.GONE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (viewHeight == 0) {
            // Instantiate viewHeight (This must be done after the view has been drawn)
            viewHeight = getHeight();
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN ||
                event.getAction() == MotionEvent.ACTION_MOVE) {
            // Show the LetterIndicator so that it can go through onCreate
            letterIndicator.show();

            if (event.getY() < getHeight() && event.getY() >= 0) {
                // If TouchEvent is within bounds of the View...
                // Set the ratio to divide by to ascertain which letter the touch is on (i.e.(x/y) = (a/b))
                float division = viewHeight / (max + 1);

                // Instantiate the value to be calculated
                int newValue = 0;
                if (event.getY() >= viewHeight) {
                    // If greater than the height of the View, then set to max
                    newValue = max;
                } else if (event.getY() < 0) {
                    // If less than the bottom of the View, then set to min
                    newValue = min;
                } else {
                    // Set by getting the y-position of the TouchEvent, divided by the ratio above
                    newValue = (int) ((event.getY()) / division);
                }

                if (value != newValue && newValue <= max) {
                    value = newValue;
                    // CallBack to the Activity implementing the View
                    listener.onValueChanged(value);
                }

                // Set the y-value of Ball
                float y = event.getY();
                y = (y > viewHeight) ? viewHeight : y;
                y = (y < 0) ? 0 : y;
                ball.setY(y);

                // Set the position of the LetterIndicator
                letterIndicator.indicator.y = y;
                letterIndicator.indicator.x = Utils.getRelativeLeft(this) - letterIndicator.indicator.size / 3 * 2;
                letterIndicator.text.setText("");
            }
        }

        if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            // Dismiss LetterIndicator when TouchEvent is canceled
            letterIndicator.dismiss();
        }
        return true;
    }

    public Map<String, Integer> getIndex() {
        return mIndex;
    }

    /**
     * Sets the alphabet to be used for display and scrolling values
     * @param alphabet Alphabet to be used
     */
    public void setAlphabet(String alphabet) {
        // Set the member alphabet to the user-input String
        mAlphabet = alphabet;

        // Set the max
        max = mAlphabet.length() - 1;

        // Remove all other TextViews previously added
        removeAllViews();

        // Re-add the ball
        addView(ball);

        // Generate the TextViews for displaying the values used
        populateIndex();
    }

    /**
     * Initializes the index on the right side of the screen to be used for fast scrolling through
     * the list of favorites
     */
    private void populateIndex() {
        // Set LayoutParams so that height is set to 0 and uses layout weight instead to evenly
        // distribute index the entire length of mIndex
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 0, 1.0f);

        for (int i = 0; i < mAlphabet.length(); i++) {
            // Inflate the view from the layout file
            TextView textView = new TextView(mContext);

            // Set the character as the text to show
            textView.setText(Character.toString(mAlphabet.charAt(i)));
            textView.setTag(Character.toString(mAlphabet.charAt(i)));
            textView.setLayoutParams(params);

            // Add the View to mIndex
            addView(textView);
            mIndex.put(Character.toString(mAlphabet.charAt(i)), -1);
        }
    }

    // CallBack to the Activity implementing this View
    public interface OnValueChangedListener {
        public void onValueChanged(int value);
    }

    // Setting for the Listener
    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.listener = listener;
    }

    /**
     * Workaround for getting the RelativeTop of the SlidingAlphabeticalIndex by setting it as a
     * View within it
     */
    class Ball extends View {
        public Ball(Context context) {
            super(context);
        }
    }

    /**
     * The Sliding Indicator that will follow the user's finger and display the letter that they
     * are currently selecting
     */
    class Indicator extends RelativeLayout {
        /** Constants **/
        float size = Utilities.convertDpToPixels(48);   // Set to 48dp for now

        /** Member Variables **/
        float x = 0;    // x-position of the indicator
        float y = 0;    // y-position of the indicator
        Paint paint;    // Paint used to drawing the Canvas

        public Indicator(Context context) {
            super(context);

            // Make the background of the View transparent
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));

            // Set the Color of the Paint used to draw the indicator
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimaryDark));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            // Set the parameters of the text of the indicator
            LayoutParams params = (LayoutParams) letterIndicator.text.getLayoutParams();

            // Set to the same size as the indicator to make it easy to center
            params.height = (int) size;
            params.width = (int) size;
            letterIndicator.text.setLayoutParams(params);

            // Draw the Circle for the background of the indicator
            canvas.drawCircle(
                    x,              // Set by the TouchEvent as 2/3 the size of the indicator away from the left edge of the SlidingAlphabetIndex
                    ball.getY() + Utils.getRelativeTop((View) ball.getParent()) - size / 2,
                    size / 2,       // Radius is half the total size
                    paint           // Paint with the Color specified above
            );

            // Set the text of the letter indicator with the same parameters as the background Circle
            letterIndicator.text.setY(ball.getY() + Utils.getRelativeTop((View) ball.getParent()) - size);
            letterIndicator.text.setX(x - size / 2);
            letterIndicator.text.setText(Character.toString(mAlphabet.charAt(value))); // Text should be updated as the indicator is drawn

            invalidate();
        }
    }

    /**
     * Holder of the text of the indicator and the background. Dialog is used to project a
     * transparent dialog the size of the screen within which the indicator is drawn
     */
    class LetterIndicator extends Dialog {
        /** Member Variables **/
        Indicator indicator;
        TextView text;

        public LetterIndicator(Context context) {
            super(context, android.R.style.Theme_Translucent);
        }

        @Override
        public void onBackPressed() {
            // Overridden to prevent the dialog from being accidentally dismissed
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            // Set the Dialog to have no Title
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            super.onCreate(savedInstanceState);

            // Utilize a RelativeLayout the width and height of the screen
            setContentView(R.layout.index_spinner);

            // Prevent accidental dismissal of the Dialog
            setCanceledOnTouchOutside(false);

            // Get the reference for the RelativeLayout
            RelativeLayout content = (RelativeLayout) this.findViewById(R.id.index_spinner_content);

            // Instantiate the background of the indicator and add the View
            indicator = new Indicator(getContext());
            content.addView(indicator);

            // Instantiate the text of the indicator and add the View
            text = new TextView(getContext());
            text.setTextColor(Color.WHITE);
            text.setGravity(Gravity.CENTER);
            content.addView(text);

            indicator.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            ));
        }
    }
}
