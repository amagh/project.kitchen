package project.kitchen.ui;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;
import project.kitchen.data.RecipeContract.*;
import project.kitchen.data.Utilities;
import project.kitchen.ui.adapter.AdapterNutrition;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_GENERIC;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_IMAGE_URL;
import static project.kitchen.ui.FragmentRecipeDetails.BundleKeys.RECIPE_DETAILS_URL;

public class ActivityRecipeDetails extends AppCompatActivity {
    /** Constants **/

    /** Member Variables **/
    private FragmentRecipeDetails mDetailsFragment;
    private AdapterNutrition mNutritionAdapter;
    public boolean imageLoaded = false;
    private String mImageUrl;
    private Snackbar mSnackbar;

    // Views bound by ButterKnife
    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.nutrition_drawer_serving_disclosure_text) TextView mNutritionServingDisclosureText;
    @BindView(R.id.nutrition_drawer_calorie_disclosure_text) TextView mNutritionCalorieDiscloureText;
    @BindView(R.id.nutrition_drawer_recycler_view) RecyclerView mNutrientRecyclerView;
    @BindView(R.id.nutrition_drawerlayout) DrawerLayout mNutritionDrawer;
    @BindView(R.id.details_recipe_image) ImageView mRecipeImageView;

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

        // Retrieve the image URL passed as an extra
        mImageUrl = getIntent().getStringExtra(RECIPE_DETAILS_IMAGE_URL);

        if (mImageUrl != null) {
            supportPostponeEnterTransition();
        }

        boolean genericRecipe = getIntent().getBooleanExtra(RECIPE_DETAILS_GENERIC, false);

        // Add the URI as part of a Bundle to attach to the FragmentRecipeDetails
        Bundle args = new Bundle();
        args.putParcelable(RECIPE_DETAILS_URL, recipeUrl);
        args.putBoolean(RECIPE_DETAILS_GENERIC, genericRecipe);

        // Instantiate the fragment and attach the Bundle containing the recipe URI
        mDetailsFragment = new FragmentRecipeDetails();
        mDetailsFragment.setArguments(args);;

        // Initialize a Context that can be passed to the CursorLoaderListener
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

                // If image was not loaded previously, load the image from the imported recipe
                if (!imageLoaded) {
                    mImageUrl = cursor.getString(RecipeEntry.IDX_IMG_URL);
                    loadImageView();
                }
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

        loadImageView();
    }

    /**
     * Doubles the touch field for dragging out the DrawerLayout when in landscape for easier
     * access for phones with on-screen buttons
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
     * Loads an image into the ImageView and starts the transition animation
     */
    private void loadImageView() {
        // Set the default parameters for loading the image with Glide
        boolean skipMemCache = false;
        DiskCacheStrategy strategy = DiskCacheStrategy.SOURCE;

        // Check that an image URL was passed with the Intent
        if (mImageUrl != null) {
            // Determine the scheme of the URL
            String scheme = Uri.parse(mImageUrl).getScheme();

            if (scheme.contains("file")) {
                // If the image is a local image, then Glide needs to skip loading from cache
                // otherwise it will load the wrong image on occasion.
                skipMemCache = true;
                strategy = DiskCacheStrategy.NONE;
            }
        } else {
            // If no image URL was passed, immediately start the transition animation
            scheduleStartPostponedTransition(mRecipeImageView);
        }

        // Use Glide to load the image into the ImageView
        Glide.with(this)
                .load(mImageUrl)
                .diskCacheStrategy(strategy)
                .skipMemoryCache(skipMemCache)
                .listener(new RequestListener<String, GlideDrawable>() {
                    @Override
                    public boolean onException(Exception e, String model, Target<GlideDrawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GlideDrawable resource, String model, Target<GlideDrawable> target, boolean isFromMemoryCache, boolean isFirstResource) {
                        // When image has finished loading, load image into target
                        target.onResourceReady(resource, null);

                        // Once the resource has been loaded, then start the transition animation
                        scheduleStartPostponedTransition(mRecipeImageView);

                        // Set imageLoaded to true to prevent the Activity from reloading the image
                        // once the Cursor has finished

                        imageLoaded = true;
                        return false;

                    }
                })
                .into(mRecipeImageView);
    }

    /**
     * Initiates the transition animation after the ImageView's dimension have been measured
     * @param sharedElement
     */
    void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        // Remove the listener to free up resources
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);

                        // Start the transition animation
                        supportStartPostponedEnterTransition();
                        return true;
                    }
                });
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
        double calories = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_CALORIES);
        double fatGrams = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_FAT);
        double carbGrams = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_CARBS);
        double proteinGrams = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_PROTEIN);
        double cholesterolMg = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_CHOLESTEROL);
        double sodiumMg = cursor.getDouble(LinkIngredientEntry.IDX_RECIPE_SODIUM);

        @RecipeEntry.NutrientType int calorieType = RecipeEntry.NUTRIENT_CALORIE;
        @RecipeEntry.NutrientType int fatType = RecipeEntry.NUTRIENT_FAT;
        @RecipeEntry.NutrientType int carbType = RecipeEntry.NUTRIENT_CARB;
        @RecipeEntry.NutrientType int proteinType = RecipeEntry.NUTRIENT_PROTEIN;
        @RecipeEntry.NutrientType int cholesterolType = RecipeEntry.NUTRIENT_CHOLESTEROL;
        @RecipeEntry.NutrientType int sodiumType = RecipeEntry.NUTRIENT_SODIUM;

        nutritionList.add(new Pair<>(calorieType, calories));
        nutritionList.add(new Pair<>(fatType, fatGrams));
        nutritionList.add(new Pair<>(carbType, carbGrams));
        nutritionList.add(new Pair<>(proteinType, proteinGrams));
        nutritionList.add(new Pair<>(cholesterolType, cholesterolMg));
        nutritionList.add(new Pair<>(sodiumType, sodiumMg));

        return nutritionList;
    }
}
