package project.hnoct.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.ui.adapter.AdapterNutrition;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class ActivityRecipeDetails extends AppCompatActivity {
    /** Constants **/

    /** Member Variables **/
    long mRecipeId;
    MenuItem mMenuFavorite;
    private FragmentRecipeDetails mDetailsFragment;
    private AdapterNutrition mNutritionAdapter;

    // Views bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.nutrition_drawer_serving_disclosure_text) TextView mNutritionServingDisclosureText;
    @BindView(R.id.nutrition_drawer_calorie_disclosure_text) TextView mNutritionCalorieDiscloureText;
    @BindView(R.id.nutrition_drawer_recycler_view) RecyclerView mNutrientRecyclerView;
    @BindView(R.id.nutrition_drawerlayout) DrawerLayout mNutritionDrawer;
//    @BindView(R.id.details_nutrient_drawer) RecyclerView mNutrientDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_details);
        ButterKnife.bind(this);

        // Set Toolbar parameters
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Retrieve the URI passed from the ActivityRecipeList
        Uri recipeUrl = getIntent().getData();

        // Add the URI as part of a Bundle to attach to the FragmentRecipeDetails
        Bundle args = new Bundle();
        args.putParcelable(FragmentRecipeDetails.RECIPE_DETAILS_URL, recipeUrl);

        // Instantiate the fragment and attach the Bundle containing the recipe URI
        mDetailsFragment = new FragmentRecipeDetails();
        mDetailsFragment.setArguments(args);;

        final Context context = this;
        // Set CursorLoaderListener to be informed when Cursor is finished loading so the
        // nutrition drawer can be populated
        mDetailsFragment.setCursorLoaderListener(new FragmentRecipeDetails.CursorLoaderListener() {
            @Override
            public void onCursorLoaded(Cursor cursor, int recipeServings) {
                // Set the text for the serving and calorie disclosure in the NutritionDrawer
                mNutritionServingDisclosureText.setText(
                        getString(R.string.nutrition_info_serving_disclosure, recipeServings)
                );
                mNutritionCalorieDiscloureText.setText(
                        getString(R.string.nutrition_info_disclosure, Utilities.getUserCalories(context))
                );

                // Set the nutrition list for the AdapterNutrition for the slide out drawer
                mNutritionAdapter.setNutritionList(getNutritionList(cursor));
            }
        });

        if (savedInstanceState == null) {
            // Add the FragmentRecipeDetails to the container layout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.recipe_detail_container, mDetailsFragment)
                    .commit();
        }

        // Instantiate the AdapterNutrition
        mNutritionAdapter = new AdapterNutrition(this);

        // Instantiate the LinearLayoutManager
        LinearLayoutManager llm3 = new LinearLayoutManager(this);

        // Set the LayoutManager for the RecyclerView within the NutritionDrawer
        mNutrientRecyclerView.setLayoutManager(llm3);

        // Set the Adapter for the NutrientRecyclerView
        mNutrientRecyclerView.setAdapter(mNutritionAdapter);

        // Change the size of the touch field for the NutritionDrawer based on orientation
        if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
            extendDrawerTouchMargin();
        }
    }

    /**
     * Doubles the touch field for dragging out the DrawerLayout when in landscape for easier
     * access for phones with on-screen keys
     */
    private void extendDrawerTouchMargin() {
        try {
            // Get the Field referring to the Dragger of the DrawerLayout
            Field mDragger = mNutritionDrawer.getClass().getDeclaredField("mRightDragger");

            // Get access to the Field
            mDragger.setAccessible(true);

            // Get the ViewDragHelper utilized for synchronizing the dragging and the movement
            // of the Drawer
            ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(mNutritionDrawer);

            // Get the Field for the edge size
            Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");

            // Get access to the Field
            mEdgeSize.setAccessible(true);

            // Get the current size of the touch area
            int edge = mEdgeSize.getInt(draggerObj);

            // Set the touch area to double its original size
            mEdgeSize.setInt(draggerObj, edge * 2);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_details, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }



    /**
     * Retrieves the information regarding the recipe's nutrition from the Cursor so that it can
     * be utilized to populate the NutritionDrawer
     * @param cursor Cursor referencing the recipe's information
     * @return List containing Pairs of of NutrientType and their values
     */
    private List<Pair<Integer, Double>> getNutritionList(Cursor cursor) {
        // Use linked list to hold nutrition information to keep order the same
        List<Pair<Integer, Double>> nutritionList = new LinkedList<>();

        // Retrieve nutrition information from database utilizing Cursor from CursorLoader
        double calories = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_CALORIES);
        double fatGrams = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_FAT);
        double carbGrams = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_CARBS);
        double proteinGrams = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_PROTEIN);
        double cholesterolMg = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_CHOLESTEROL);
        double sodiumMg = cursor.getDouble(RecipeContract.LinkIngredientEntry.IDX_RECIPE_SODIUM);

        @RecipeContract.RecipeEntry.NutrientType int calorieType = RecipeContract.RecipeEntry.NUTRIENT_CALORIE;
        @RecipeContract.RecipeEntry.NutrientType int fatType = RecipeContract.RecipeEntry.NUTRIENT_FAT;
        @RecipeContract.RecipeEntry.NutrientType int carbType = RecipeContract.RecipeEntry.NUTRIENT_CARB;
        @RecipeContract.RecipeEntry.NutrientType int proteinType = RecipeContract.RecipeEntry.NUTRIENT_PROTEIN;
        @RecipeContract.RecipeEntry.NutrientType int cholesterolType = RecipeContract.RecipeEntry.NUTRIENT_CHOLESTEROL;
        @RecipeContract.RecipeEntry.NutrientType int sodiumType = RecipeContract.RecipeEntry.NUTRIENT_SODIUM;

        nutritionList.add(new Pair<>(calorieType, calories));
        nutritionList.add(new Pair<>(fatType, fatGrams));
        nutritionList.add(new Pair<>(carbType, carbGrams));
        nutritionList.add(new Pair<>(proteinType, proteinGrams));
        nutritionList.add(new Pair<>(cholesterolType, cholesterolMg));
        nutritionList.add(new Pair<>(sodiumType, sodiumMg));

        return nutritionList;
    }
}
