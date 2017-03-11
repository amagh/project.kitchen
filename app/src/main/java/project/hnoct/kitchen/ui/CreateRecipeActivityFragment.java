package project.hnoct.kitchen.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;

/**
 * A placeholder fragment containing a simple view.
 */
public class CreateRecipeActivityFragment extends Fragment {
    /** Constants **/
    private static final String LOG_TAG = CreateRecipeActivity.class.getSimpleName();
    private final int SELECT_PHOTO = 25687;


    /** Member Variables **/
    Context mContext;
    long mRecipeId;
    private Uri mRecipeImageUri;
    private String mRecipeDescription;
    private String mRecipeName;

    // Required to be added to database
    private boolean mFavorite = false;
    private long mDateAdded = Utilities.getCurrentTime();
    private String mSource = "user-added";
    private String mRecipeUrl = "user.custom/";

    List<Pair<String, String>> mIngredientList;
    List<String> mDirectionList;

    AddIngredientAdapter mIngredientAdapter;
    AddDirectionAdapter mDirectionAdapter;

    boolean mSaved = false;     // Check if the user has saved the data manually

    Bitmap mImageBitmap;

    // ButterKnife Binding
    @BindView(R.id.create_recipe_name_edit_text) EditText mRecipeNameEditText;
    @BindView(R.id.create_recipe_description_edit_text) EditText mRecipeDescriptionEditText;
    @BindView(R.id.create_recipe_image) ImageView mRecipeImage;
    @BindView(R.id.create_recipe_ingredient_recycler_view) NonScrollingRecyclerView mIngredientRecyclerView;
    @BindView(R.id.create_recipe_direction_recycler_view) NonScrollingRecyclerView mDirectionRecyclerView;
    @BindView(R.id.create_recipe_clear_image) ImageView mClearImageButton;

    /**
     * Opens Activity that allows for selection of photo to be used for recipe
     */
    @OnClick(R.id.create_recipe_image)
    public void selectImage() {
        // Set the intent for ACTION_GET_CONTENT with image type
        Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");

        // Start activity and wait for result
        startActivityForResult(photoPickerIntent, SELECT_PHOTO);
    }

    /**
     * Removes the image from File and sets the image URI to null
     */
    @OnClick(R.id.create_recipe_clear_image)
    public void clearImage() {
        // Delete the image from File
        Utilities.deleteImageFromFile(mContext, mRecipeImageUri);

        // Set the image URI to null
        mRecipeImageUri = null;

        // Load a null image into the ImageView to remove the previous image loaded
        Glide.with(mContext).load(mRecipeImageUri).into(mRecipeImage);
    }

    public CreateRecipeActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        // Attempt to retrieve saved data
        getSavedData();

        if (mRecipeId == 0) {
            // If no saved data exists, generate a new recipeId
            mRecipeId = Utilities.generateNewId(mContext, 10000, Utilities.RECIPE_TYPE);
        } else {
            // Insert saved data into EditText
            mRecipeNameEditText.setText(mRecipeName, TextView.BufferType.EDITABLE);
            mRecipeDescriptionEditText.setText(mRecipeDescription, TextView.BufferType.EDITABLE);
            Log.d(LOG_TAG, "Why does this exist? : " + mRecipeImageUri);
            Glide.with(mContext)
                    .load(mRecipeImageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(mRecipeImage);

            // Delete the autosaved data so it will not show up again after they've completed this
            // recipe
            deleteAutosavedData();
        }

        // Instantiate the RecyclerAdapters
        mIngredientAdapter = new AddIngredientAdapter(mContext);
        mDirectionAdapter = new AddDirectionAdapter(mContext);

        if (mIngredientList != null) {
            mIngredientAdapter.setIngredientList(mIngredientList);
        }
        if (mDirectionList != null) {
            mDirectionAdapter.setDirectionList(mDirectionList);
        }

        // Instantiate the LinearLayoutManagers and override scrolling behavior to allow for smooth
        // scrolling
        LinearLayoutManager llm = new LinearLayoutManager(mContext) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        LinearLayoutManager llm2 = new LinearLayoutManager(mContext) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };

        // Set the LLM and Adapters to the RecyclerViews
        mIngredientRecyclerView.setLayoutManager(llm);
        mDirectionRecyclerView.setLayoutManager(llm2);

        mIngredientRecyclerView.setAdapter(mIngredientAdapter);
        mDirectionRecyclerView.setAdapter(mDirectionAdapter);

        return rootView;
    }

    @Override
    public void onPause() {
        if (!mSaved) {
            // Save all user-input whenever the Activity is paused
            saveUserInput();
        }
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Check to make sure that data was received correctly and has the correct request code
        if (requestCode == SELECT_PHOTO && resultCode == Activity.RESULT_OK && data != null) {
            // Get the URI of the image selected
            Uri selectedImageUri = data.getData();

            try {
                // Create a bitmap by turning the bitmap into an InputStream
                InputStream inputStream = mContext.getContentResolver().openInputStream(selectedImageUri);

                // Decode the bitmap from the stream
                mImageBitmap = BitmapFactory.decodeStream(inputStream);

                // Save the bitmap to file in the private directory
                /** @see Utilities#saveImageToFile(Context, long, Bitmap) **/
                mRecipeImageUri = Utilities.saveImageToFile(mContext, mRecipeId, mImageBitmap);

                // Load the image into the ImageView
                Glide.with(mContext)
                        .load(mRecipeImageUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)      // Prevent Glide from caching image
                        .skipMemoryCache(true)                          // Prevent Glide from caching image
                        .into(mRecipeImage);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Retrieves saved data from SharedPreferences from when user last left this Activity
     */
    private void getSavedData() {
        // Initialize SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Get the stored data
        mRecipeName = prefs.getString(mContext.getString(R.string.edit_recipe_name_key), null);
        mRecipeDescription = prefs.getString(mContext.getString(R.string.edit_recipe_description_key), null);
        if (prefs.getString(mContext.getString(R.string.edit_recipe_image_key), null) != null) {
            mRecipeImageUri = Uri.parse(prefs.getString(mContext.getString(R.string.edit_recipe_image_key), null));
        } else {
            mRecipeImageUri = null;
        }
        String ingredientString = prefs.getString(mContext.getString(R.string.edit_recipe_ingredients_key), null);
        String directionList = prefs.getString(mContext.getString(R.string.edit_recipe_directions_key), null);

        // Convert the stored List in String form back into a List<Pair>
        if (ingredientString != null) {
            // Split the String into an Array of Pairs-in-String-form
            String[] ingredientArray = ingredientString.split("\n");

            // Instantiate mIngredientList
            mIngredientList = new LinkedList<>();

            // Iterate through each Pair-in-String-form in the Array and create a Pair from the values
            for (String ingredientPairString : ingredientArray) {
                String first = ingredientPairString.substring(0, ingredientPairString.indexOf("|"));
                String second = ingredientPairString.substring(ingredientPairString.indexOf("|") + 1);

                // Add the Pair into mIngredientList
                mIngredientList.add(new Pair<>(first, second));
            }
        }

        // Convert the stored List-in-String form of directions back into a List<String>
        if (directionList != null) {
            String[] directionArray = directionList.split("\n");

            // Instantiate mDirectionList
            mDirectionList = new LinkedList<>();

            // Add information back into mDirectionList
            mDirectionList.addAll(Arrays.asList(directionArray));
        }

    if (mRecipeName != null || mRecipeDescription != null || mRecipeImageUri != null ||
            mIngredientList != null || mDirectionList != null) {
            // If at least one piece of data was retrieved, then get the recipeId
            mRecipeId = prefs.getLong(mContext.getString(R.string.edit_recipe_id_key), 0);
        }
    }

    /**
     * Saves user input to SharedPreferences in case application is accidentally exited or the
     * user accidentally leaves the activity
     */
    private void saveUserInput() {
        // Save the user's input when leaving the activity so they can pick up where they left off
        // Get the String entered by the user into the EditText Views
        mRecipeName = mRecipeNameEditText.getText().toString();
        mRecipeDescription = mRecipeDescriptionEditText.getText().toString();
        mIngredientList = mIngredientAdapter.getIngredientList();
        mDirectionList = mDirectionAdapter.getDirectionList();

        // Initialize SharedPreferences and its Editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        // Save data to SharedPreferences if it exists
        // Save recipe name
        editor.putString(
                mContext.getString(R.string.edit_recipe_name_key),
                mRecipeName
        );

        // Save recipe description
        editor.putString(
                mContext.getString(R.string.edit_recipe_description_key),
                mRecipeDescription
        );

        // Save recipe image URI
        if (mRecipeImageUri != null) {
            editor.putString(
                    mContext.getString(R.string.edit_recipe_image_key),
                    mRecipeImageUri.toString()
            );
        } else {
            editor.putString(
                    mContext.getString(R.string.edit_recipe_image_key),
                    null
            );
        }

        // Save ingredient list
        if (mIngredientList != null) {
            // Initialize the StringBuilder that will be used to create the String of ingredient
            // info to be saved
            StringBuilder builder = new StringBuilder();
            for (Pair<String, String> ingredientPair : mIngredientList) {
                // Pair information is held using unique characters as separators so the text can
                // be split back into pairs when being read from SharedPreferences
                builder.append(ingredientPair.first)
                        .append("|")    // Separator between first and second
                        .append(ingredientPair.second)
                        .append("\n");  // Separator between Pairs
            }

            // Output the builder to String and trim the last "\n" appended
            String ingredientStringList = builder.toString().trim();

            // Save ingredients to SharedPreferences
            editor.putString(
                    mContext.getString(R.string.edit_recipe_ingredients_key),
                    ingredientStringList
            );
        } else {
            // Skip the iterator and save a null value
            editor.putString(
                    mContext.getString(R.string.edit_recipe_ingredients_key),
                    null
            );
        }

        // Save direction list
        if (mDirectionList != null) {
            // Initialize the StringBuilder that will be used to create the String of direction
            // info to be saved
            StringBuilder builder = new StringBuilder();
            for (String direction : mDirectionList) {
                // Separate individual directions by appending a new line
                builder.append(direction)
                        .append("\n");
            }

            // Output the builder to String
            String directionList = builder.toString().trim();

            // Save the direction information to SharedPreferences
            editor.putString(
                    mContext.getString(R.string.edit_recipe_directions_key),
                    directionList
            );
        }

        if (mRecipeName != null || mRecipeDescription != null || mRecipeImageUri != null ||
                mIngredientList != null || mDirectionList != null) {
            // RecipeId only needs to be saved if other data was saved as well.
            editor.putLong(
                    mContext.getString(R.string.edit_recipe_id_key),
                    mRecipeId
            );
        }
        // Apply the changes
        editor.apply();
    }

    /**
     * Wipes data in SharedPreferences after it has been loaded into memory
     */
    private void deleteAutosavedData() {
        // Instantiate SharedPreferences and its Editor
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();

        // Place null values in the SharedPreferences
        editor.putString(
                mContext.getString(R.string.edit_recipe_name_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_description_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_image_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_ingredients_key),
                null
        );
        editor.putString(
                mContext.getString(R.string.edit_recipe_directions_key),
                null
        );
        editor.putLong(
                mContext.getString(R.string.edit_recipe_id_key),
                0
        );

        editor.apply();
    }
}
