package project.kitchen.materialdesign;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;

/**
 * Copied from {@see ahref=https://github.com/navasmdc/MaterialDesignLibrary/blob/master/MaterialDesignLibrary/MaterialDesign/src/main/java/com/gc/materialdesign/utils/Utils.java}
 * because the library could not be imported due to incompatibilities with MaterialDesign Library
 */

public class Utils {


    /**
     * Convert Dp to Pixel
     */
    public static int dpToPx(float dp, Resources resources){
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
        return (int) px;
    }

    public static int getRelativeTop(View myView) {
//	    if (myView.getParent() == myView.getRootView())
        if(myView.getId() == android.R.id.content)
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) myView.getParent());
    }

    public static int getRelativeLeft(View myView) {
//	    if (myView.getParent() == myView.getRootView())
        if(myView.getId() == android.R.id.content)
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) myView.getParent());
    }

}