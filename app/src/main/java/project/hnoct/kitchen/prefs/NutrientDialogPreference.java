package project.hnoct.kitchen.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 2/26/2017.
 */

public class NutrientDialogPreference extends DialogPreference {
    /** Constants **/

    /** Member Variables **/
    Context mContext;
    String[] mDisplayValues;
    @RecipeEntry.NutrientType int mNutrientType;

    // Views bound by ButterKnife
    @BindView(R.id.pref_dialog_nutrient_picker) NumberPicker mNumberPicker;

    public NutrientDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        // Set the layout of the widget
        setDialogLayoutResource(R.layout.pref_nutrient_dialog);

        // Get the nutrient being selected based on title
        String nutrientType = getTitle().toString();

        // Instantiate the different nutrient types for the switch statement
        String calorieType = mContext.getString(R.string.nutrient_calories_title);
        String fatType = mContext.getString(R.string.nutrient_fat_title);
        String carbType = mContext.getString(R.string.nutrient_carbs_title);
        String proteinType = mContext.getString(R.string.nutrient_protein_title);
        String cholesterolType = mContext.getString(R.string.nutrient_cholesterol_title);
        String sodiumType = mContext.getString(R.string.nutrient_sodium_title);

        switch (nutrientType) {
            case calorieType: {
                mNutrientType = RecipeEntry.NUTRIENT_CALORIE;
                break;
            }
            case mContext.getString(R.string.nutrient_fat_title) {

            }
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        ButterKnife.bind(this, view);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        final int userCalories = prefs.getInt(
                mContext.getString(R.string.pref_calorie_key),
                Integer.parseInt(mContext.getString(R.string.calories_default))
        );
//        mNumberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
//            @Override
//            public void onValueChange(NumberPicker numberPicker, int oldValue, int newValue) {
//
//            }
//        });

        mDisplayValues = createCalorieDisplayValues();
        mNumberPicker.setMaxValue(mDisplayValues.length - 1);
        mNumberPicker.setMinValue(0);
        mNumberPicker.setDisplayedValues(mDisplayValues);

        int setValue = getSetValueFromDisplay(userCalories);

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
        return 0;
    }

    private int getSetValueFromDisplay(double displayValue) {
        for (int i = 0; i < mDisplayValues.length; i++ ) {
            if (mDisplayValues[i].equals(String.valueOf(displayValue))) {
                return i;
            }
        }
        return 0;
    }

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


}
