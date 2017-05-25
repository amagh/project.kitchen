package project.kitchen.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/26/2017.
 */

public class NutrientDialogPreference extends DialogPreference {
    /** Constants **/

    /** Member Variables **/
    private Context mContext;                               // Interface for global context
    private String[] mDisplayValues;                        // Values to display in NumberPicker
    @RecipeEntry.NutrientType
    private int mNutrientType;    // Used for getting information from SharedPrefs

    // Views bound by ButterKnife
    @BindView(R.id.pref_dialog_nutrient_picker) NumberPicker mNumberPicker;

    public NutrientDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // Set the layout of the widget
        setDialogLayoutResource(R.layout.dialog_pref_nutrient);

        // Get the nutrient being selected based on title
        int nutrientTitleRes = getTitleRes();

        // Switch on nutrient String resource to set mNutrientType
        switch (nutrientTitleRes) {
            case R.string.nutrient_calories_title:
                mNutrientType = RecipeEntry.NUTRIENT_CALORIE;
                break;

            case R.string.nutrient_fat_title:
                mNutrientType = RecipeEntry.NUTRIENT_FAT;
                break;

            case R.string.nutrient_carbs_title:
                mNutrientType = RecipeEntry.NUTRIENT_CARB;
                break;

            case R.string.nutrient_protein_title:
                mNutrientType = RecipeEntry.NUTRIENT_PROTEIN;
                break;

            case R.string.nutrient_cholesterol_title:
                mNutrientType = RecipeEntry.NUTRIENT_CHOLESTEROL;
                break;

            case R.string.nutrient_sodium_title:
                mNutrientType = RecipeEntry.NUTRIENT_SODIUM;
                break;

            default:
                throw new UnsupportedOperationException("Unknown nutrient type: " + mContext.getString(nutrientTitleRes));
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        /** Variables **/
        int nutrientInt = -1;
        float nutrientFloat = -1;

        // Binds Views with ButterKnife
        ButterKnife.bind(this, view);

        // Initialize SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Get nutrient value based on mNutrientType and create DisplayValues for NumberPicker
        switch (mNutrientType) {
            case RecipeEntry.NUTRIENT_CALORIE: {
                nutrientInt = prefs.getInt(
                        mContext.getString(R.string.pref_calorie_key),
                        Integer.parseInt(mContext.getString(R.string.calories_default))
                );
                mDisplayValues = createCalorieDisplayValues();
                break;
            }
            case RecipeEntry.NUTRIENT_FAT: {
                nutrientFloat = prefs.getFloat(
                        mContext.getString(R.string.pref_fat_key),
                        Float.parseFloat(mContext.getString(R.string.fat_percent_float_default))
                );
                break;
            }
            case RecipeEntry.NUTRIENT_CARB: {
                nutrientFloat = prefs.getFloat(
                        mContext.getString(R.string.pref_carb_key),
                        Float.parseFloat(mContext.getString(R.string.carb_percent_float_default))
                );
                break;
            }
            case RecipeEntry.NUTRIENT_PROTEIN: {
                nutrientFloat = prefs.getFloat(
                        mContext.getString(R.string.pref_protein_key),
                        Float.parseFloat(mContext.getString(R.string.protein_percent_float_default))
                );
                break;
            }
            case RecipeEntry.NUTRIENT_CHOLESTEROL: {
                nutrientInt = prefs.getInt(
                        mContext.getString(R.string.pref_cholesterol_key),
                        Integer.parseInt(mContext.getString(R.string.cholesterol_mg_default))
                );
                break;
            }
            case RecipeEntry.NUTRIENT_SODIUM:
                nutrientInt = prefs.getInt(
                        mContext.getString(R.string.pref_sodium_key),
                        Integer.parseInt(mContext.getString(R.string.sodium_mg_default))
                );
                break;
        }

        int setValue = -1;

        // Generate the value to set the NumberPicker utilizing the retrieved nutrient value
        if (nutrientFloat > -1) {
            setValue = getSetValueFromDisplay(nutrientFloat);
        } else if (nutrientInt > -1) {
            setValue = getSetValueFromDisplay(nutrientInt);
        }

        // Set the  min/max and the DisplayValues for the NumberPicker
        mNumberPicker.setMaxValue(mDisplayValues.length - 1);
        mNumberPicker.setMinValue(0);
        mNumberPicker.setDisplayedValues(mDisplayValues);

        mNumberPicker.setValue(setValue);

        super.onBindDialogView(view);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult) {
            return;
        }
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
        editor.putInt(
                mContext.getString(R.string.pref_calorie_key),
                Integer.parseInt(mDisplayValues[mNumberPicker.getValue()])
        ).apply();;

        super.onDialogClosed(positiveResult);
    }

    private int getSetValueFromDisplay(int displayValue) {
        for (int i = 0; i < mDisplayValues.length; i++ ) {
            if (mDisplayValues[i].equals(String.valueOf(displayValue))) {
                return i;
            }
        }
        return -1;
    }

    private int getSetValueFromDisplay(float displayValue) {
        for (int i = 0; i < mDisplayValues.length; i++ ) {
            if (mDisplayValues[i].equals(String.valueOf(displayValue * 100))) {
                return i;
            }
        }
        return -1;
    }

    /**
     *
     * @return
     */
    private String[] createCalorieDisplayValues(){
        int calorieMax = 5000;
        int calorieMin = 1000;
        int increment = 100;

        String[] calorieDisplayValues = new String[(calorieMax - calorieMin) / increment];
        for (int i = 0; i < calorieDisplayValues.length; i++) {
            calorieDisplayValues[i] = String.valueOf((i + 1) * 100);
        }

        return calorieDisplayValues;
    }

    private String[] createPercentageDisplayValues() {
        int percentMax = 100;
        int percentMin = 0;
        int increment = 5;

        // Create the length of the String[] by casting the float to an int
        String[] percentageDisplayValues = new String[(percentMax - percentMin) / increment];
        for (int i = 0; i < percentageDisplayValues.length; i++) {
            percentageDisplayValues[i] = String.valueOf(i * 5);
        }

        return percentageDisplayValues;
    }

    private String[] createCholesterolDisplayValues() {
        int cholesterolMax = 300;
        int cholesterolMin = 0;
        int increment = 10;

        String[] cholesterolDisplayValues = new String[(cholesterolMax -cholesterolMin) / increment];
        return cholesterolDisplayValues;
    }

}
