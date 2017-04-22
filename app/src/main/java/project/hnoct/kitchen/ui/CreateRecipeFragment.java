package project.hnoct.kitchen.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.view.NonScrollingRecyclerView;

/**
 * A placeholder fragment containing a simple view.
 */
public class CreateRecipeFragment extends Fragment implements CreateRecipeActivity.MenuButtonClicked {
    /** Constants **/
    private static final String LOG_TAG = CreateRecipeActivity.class.getSimpleName();
    static final String RECIPE_URI = "recipe_uri";
    private final int SELECT_PHOTO = 25687;


    /** Member Variables **/
    private Context mContext;
    private long mRecipeId;
    private Uri mRecipeImageUri;
    private String mRecipeDescription;
    private String mRecipeName;
    private String mRecipeAuthor = "testUser";
    private String mSource;
    private ItemTouchHelper mIngredientIth;
    private ItemTouchHelper mDirectionIth;

    // Required to be added to database
    private boolean mFavorite = false;
    private long mDateAdded = Utilities.getCurrentTime();

    private List<Pair<String, String>> mIngredientList;
    private List<String> mDirectionList;

    private AddIngredientAdapter mIngredientAdapter;
    private AddDirectionAdapter mDirectionAdapter;

    private boolean mSaved = false;     // Check if the user has saved the data manually

    private Bitmap mImageBitmap;

    // ButterKnife Binding
    @BindView(R.id.create_recipe_name_edit_text) EditText mRecipeNameEditText;
    @BindView(R.id.create_recipe_description_edit_text) EditText mRecipeDescriptionEditText;
    @BindView(R.id.create_recipe_image) ImageView mRecipeImage;
    @BindView(R.id.create_recipe_ingredient_recycler_view) NonScrollingRecyclerView mIngredientRecyclerView;
    @BindView(R.id.create_recipe_direction_recycler_view) NonScrollingRecyclerView mDirectionRecyclerView;
    @BindView(R.id.create_recipe_clear_image) ImageView mClearImageButton;
    @BindView(R.id.create_recipe_add_ingredient) LinearLayout addIngredientButton;

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
    void clearImage() {
        // Delete the image from File
        Utilities.deleteImageFromFile(mContext, mRecipeImageUri);

        // Set the image URI to null
        mRecipeImageUri = null;

        // Load a null image into the ImageView to remove the previous image loaded
        Glide.with(mContext).load(mRecipeImageUri).into(mRecipeImage);
    }

    public CreateRecipeFragment() {
    }

    @OnClick(R.id.create_recipe_add_ingredient)
    void addIngredient() {
        mIngredientAdapter.addIngredient();
    }

    @OnClick(R.id.create_recipe_add_direction)
    void addDirection() {
        mDirectionAdapter.addDirection();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_create_recipe, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        // Attempt to load information from Bundle
        if (getArguments() != null) {
            // Get the URI for the recipe in the database from the Bundle
            Uri recipeUri = getArguments().getParcelable(RECIPE_URI);

            if (recipeUri != null && !recipeUri.equals("")) {
                // If it exists, load information from database into the Activity
                // Query the database for recipe information
                Cursor cursor = mContext.getContentResolver().query(
                        RecipeEntry.CONTENT_URI,
                        RecipeEntry.RECIPE_PROJECTION,
                        RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                        new String[] {Long.toString(RecipeEntry.getRecipeIdFromUri(recipeUri))},
                        null
                );

                if (cursor != null && cursor.moveToFirst()) {
                    // Retrieve information from database and load into member variables
                    mRecipeName = cursor.getString(RecipeEntry.IDX_RECIPE_NAME);
                    mRecipeDescription = cursor.getString(RecipeEntry.IDX_SHORT_DESCRIPTION);
                    mRecipeAuthor = cursor.getString(RecipeEntry.IDX_RECIPE_AUTHOR);
                    mFavorite = cursor.getInt(RecipeEntry.IDX_FAVORITE) == 1;
                    mRecipeImageUri = Uri.parse(cursor.getString(RecipeEntry.IDX_IMG_URL));
                    mSource = cursor.getString(RecipeEntry.IDX_RECIPE_SOURCE);

                    // If the recipe is not user added, set the recipeId as the negative of the recipe
                    // so the original can be easily referenced
                    mRecipeId = mSource.equals(getString(R.string.attribution_custom))
                            ? cursor.getLong(RecipeEntry.IDX_RECIPE_ID)
                            : -cursor.getLong(RecipeEntry.IDX_RECIPE_ID);

                    // Retrieve the directions in String form
                    String directions = cursor.getString(RecipeEntry.IDX_RECIPE_DIRECTIONS);

                    // Split the directions at every new line
                    String[] directionArray = directions.split("\n");

                    // Set the member direction list by converting the Array to a List
                    mDirectionList = new LinkedList<>(Arrays.asList(directionArray));
                }

                // Close the Cursor
                if (cursor != null) cursor.close();

                // Set the Cursor to query all tables and filtering by recipeId and recipe source
                cursor = mContext.getContentResolver().query(
                        LinkIngredientEntry.CONTENT_URI,
                        LinkIngredientEntry.LINK_PROJECTION,
                        RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " + RecipeEntry.TABLE_NAME + "." + RecipeEntry.COLUMN_SOURCE + " = ?",
                        new String[] {Long.toString(mSource.equals(mContext.getString(R.string.attribution_custom))? mRecipeId : -mRecipeId), mSource},
                        LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " ASC"  // Sort by ingredient order to maintain order of ingredients
                );

                if (cursor != null && cursor.moveToFirst()) {
                    // Instantiate mIngredientList
                    mIngredientList = new LinkedList<>();

                    // Get ingredient name and quantity and add it as a new Pair to mIngredientList
                    do {
                        String ingredient = cursor.getString(LinkIngredientEntry.IDX_INGREDIENT_NAME);
                        String quantity = cursor.getString(LinkIngredientEntry.IDX_LINK_QUANTITY);
                        Pair<String, String> ingredientPair = new Pair<>(quantity, ingredient);
                        mIngredientList.add(ingredientPair);
                    } while (cursor.moveToNext());
                }

                // Close the Cursor
                if (cursor != null) cursor.close();

            } else {
                // Bundle does not exist or is empty, attempt to retrieve saved data
                getSavedData();
            }
        } else {
            // Bundle does not exist or is empty, attempt to retrieve saved data
            getSavedData();

        }

        if (mRecipeId == 0) {
            // If no saved data exists, generate a new recipeId
            mRecipeId = Utilities.generateNewId(mContext, Utilities.RECIPE_TYPE);

        } else {
            // Insert saved data into EditText
            mRecipeNameEditText.setText(mRecipeName, TextView.BufferType.EDITABLE);
            mRecipeDescriptionEditText.setText(mRecipeDescription, TextView.BufferType.EDITABLE);

            // Load the image into the ImageView
            Glide.with(mContext)
                    .load(mRecipeImageUri)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(mRecipeImage);

            // Delete the autosaved data so it will not show up again after they've completed this
            // recipe
            deleteAutosavedData();
        }

        if (mSource == null) {
            // If the source wasn't loaded from auto-save or Bundle, set the source from default
            mSource = mContext.getString(R.string.attribution_custom);
        }

        // Instantiate the RecyclerAdapters
        mIngredientAdapter = new AddIngredientAdapter(mContext, new AddIngredientAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(AddIngredientAdapter.AddIngredientViewHolder holder) {
                // Begin listening for drag events initiated from the ViewHolder's handler
                mIngredientIth.startDrag(holder);
            }
        });

        mDirectionAdapter = new AddDirectionAdapter(mContext, new AddDirectionAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(AddDirectionAdapter.AddDirectionViewHolder viewHolder) {
                mDirectionIth.startDrag(viewHolder);
            }
        });

        // Set the Adapter's Lists if applicable
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

        mIngredientIth = new ItemTouchHelper(ingredientIthCallback);
        mIngredientIth.attachToRecyclerView(mIngredientRecyclerView);

        mDirectionIth = new ItemTouchHelper(directionIthCallback);
        mDirectionIth.attachToRecyclerView(mDirectionRecyclerView);

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
        mIngredientList = mIngredientAdapter.getCorrectedIngredientList();
        mDirectionList = mDirectionAdapter.getCorrectedDirectionList();

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

    /**
     * Saves the user content to the database
     */
    private void saveToDatabase() {
        // Variables
        boolean newRecipe = true;   // Check to see whether values need to be inserted or updated

        // Store data from EditText to member variables
        mRecipeDescription = mRecipeDescriptionEditText.getText().toString();
        mRecipeName = mRecipeNameEditText.getText().toString();
        mIngredientList = mIngredientAdapter.getCorrectedIngredientList();
        mDirectionList = mDirectionAdapter.getCorrectedDirectionList();

        // Check to make sure all database requirements are satisfied prior to attempting to save
        if (mRecipeAuthor == null || mRecipeAuthor.trim().equals("") ||
                mRecipeDescription == null || mRecipeDescription.trim().equals("") ||
                mRecipeImageUri == null ||
                mRecipeName == null || mRecipeName.equals("") ||
                mIngredientList == null || mIngredientList.isEmpty() ||
                mDirectionList == null || mDirectionList.isEmpty()) {

            if (mRecipeAuthor == null || mRecipeAuthor.trim().equals("")) {
                Toast.makeText(
                        mContext,
                        "Please add an author!",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            if (mRecipeDescription == null || mRecipeDescription.trim().equals("")) {
                Toast.makeText(
                        mContext,
                        "Please add a description!",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            if (mRecipeImageUri == null) {
                Toast.makeText(
                        mContext,
                        "Please add an image!",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            if (mIngredientList == null || mIngredientList.isEmpty()) {
                Toast.makeText(
                        mContext,
                        "Please add an ingredient!",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            if (mDirectionList == null || mDirectionList.isEmpty()) {
                Toast.makeText(
                        mContext,
                        "Please add a direction!",
                        Toast.LENGTH_SHORT)
                        .show();
            }

            // TODO: Inform user what information is still missing
//            Toast.makeText(
//                    mContext,
//                    "Please add required information before attempting to save!",
//                    Toast.LENGTH_LONG)
//                    .show();

            return;
        }

        // Query the database to check if recipe already exists in database
        Cursor cursor = mContext.getContentResolver().query(
                RecipeEntry.CONTENT_URI,
                null,
                RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                new String[] {Long.toString(mRecipeId)},
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // If Cursor finds a row, then values need to be updated
            newRecipe = false;
        }

        // Close the Cursor
        cursor.close();

        // Create ContentValues for Recipe that need to be inserted/updated
        ContentValues recipeValues = new ContentValues();
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_ID, mRecipeId);
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_NAME, mRecipeName);
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_AUTHOR, "TestUser");     // TODO: Add ability to set recipe-author
        recipeValues.put(RecipeEntry.COLUMN_IMG_URL, mRecipeImageUri.toString());
        recipeValues.put(RecipeEntry.COLUMN_SHORT_DESC, mRecipeDescription);
        recipeValues.put(RecipeEntry.COLUMN_SOURCE, mSource);
        String mRecipeUrl = "content://user.custom/";
        recipeValues.put(RecipeEntry.COLUMN_RECIPE_URL, mRecipeUrl + mRecipeId);
        recipeValues.put(RecipeEntry.COLUMN_FAVORITE, mFavorite);
        recipeValues.put(RecipeEntry.COLUMN_DATE_ADDED, mDateAdded);

        // Modify directions by appending new line in between separate instructions so it can be
        // appropriately separated when read from database
        String directions = null;
        if (mDirectionList != null && !mDirectionList.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (String direction : mDirectionList) {
                builder.append(direction)
                        .append("\n");
            }
            directions = builder.toString().trim();
        }

        // Add direction values to recipe ContentValues
        recipeValues.put(RecipeEntry.COLUMN_DIRECTIONS, directions);

        // Create Lists to hold ingredientIds, ingredient ContentValues, and link ContentValues
        @SuppressLint("UseSparseArrays") Map<Long, String> ingredientIdNameMap = new HashMap<>();
        List<ContentValues> ingredientCVList = new LinkedList<>();
        ContentValues[] linkValues = new ContentValues[mIngredientList.size()];

        if (mIngredientList != null) {
            // Iterate through each ingredient in the List and create ContentValues from the Pair values
            for (int i = 0; i < mIngredientList.size(); i++) {
                // Retrieve the Pair
                Pair<String, String> ingredientPair = mIngredientList.get(i);

                // Instantiate the ContentValues for ingredient and link tables
                ContentValues ingredientValue = new ContentValues();
                ContentValues linkValue = new ContentValues();

                // Get values for quantity and ingredient name from the Pair
                String quantity = ingredientPair.first;
                String ingredient = ingredientPair.second;

                // Check to see if ingredient already exists in database, if so, set the ingredientId correctly
                boolean newIngredient = true;
                long ingredientId = Utilities.getIngredientIdFromName(mContext, ingredient);

                if (ingredientId == -1) {
                    // If no ingredientId exists, then generate a new Id for the ingredient
                    ingredientId = Utilities.generateNewId(mContext, Utilities.INGREDIENT_TYPE);
                } else {
                    // If ingredient is already found in database, there is no need to add this
                    // ingredient to the ingredient table
                    newIngredient = false;
                }

                // Check to make sure ingredientId isn't already contained in this recipe's list of
                // ingredient IDs
                while (newIngredient && ingredientIdNameMap.keySet().contains(ingredientId) && !ingredient.equals(ingredientIdNameMap.get(ingredientId))) {
                    // If it does, iterate the ingredientId until a new one is generated
                    ingredientId++;
                }

                // Add the ingredientId to the Map of ingredientIds to check against subsequent ingredients
                ingredientIdNameMap.put(ingredientId, ingredient);

                // Add the values to the ContentValues for ingredients
                if (newIngredient) {
                    // If ingredient is not found in database, add it
                    ingredientValue.put(IngredientEntry.COLUMN_INGREDIENT_NAME, ingredient);
                    ingredientCVList.add(ingredientValue);
                }

                // Add the values to the ContentValues for the Link table
                linkValue.put(RecipeEntry.COLUMN_RECIPE_ID, mRecipeId);
                linkValue.put(RecipeEntry.COLUMN_SOURCE, mSource);
                linkValue.put(IngredientEntry.COLUMN_INGREDIENT_ID, ingredientId);
                linkValue.put(LinkIngredientEntry.COLUMN_INGREDIENT_ORDER, i);
                linkValue.put(LinkIngredientEntry.COLUMN_QUANTITY, quantity);
                linkValues[i] = linkValue;
            }
        }

        if (newRecipe) {
            // If recipe does not already exist in database, then recipe and link values can be bulk
            // inserted
            mContext.getContentResolver().insert(
                    RecipeEntry.CONTENT_URI,
                    recipeValues
            );

            int inserted = mContext.getContentResolver().bulkInsert(
                    LinkIngredientEntry.CONTENT_URI,
                    linkValues
            );

            Toast.makeText(mContext, "New recipe updated! " + inserted + " link values inserted!", Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, update the existing values in the database
            mContext.getContentResolver().update(
                    RecipeEntry.CONTENT_URI,
                    recipeValues,
                    RecipeEntry.COLUMN_RECIPE_ID + " = ?",
                    new String[] {Long.toString(mRecipeId)}
            );

            // Generate a List from the Array of ContentValues
            List<ContentValues> linkCVList = new LinkedList<>();
            linkCVList.addAll(Arrays.asList(linkValues));

            /** @see Utilities#insertAndUpdateLinkValues(Context, List) **/
            Pair<Integer, Integer> insertionUpdatePair = Utilities.insertAndUpdateLinkValues(mContext, linkCVList);
            Toast.makeText(mContext, "Recipe updated! " + insertionUpdatePair.first + " values inserted & " + insertionUpdatePair.second + " values updated!", Toast.LENGTH_SHORT).show();
        }

        // Insert missing ingredient values to the database
        /** @see Utilities#insertAndUpdateIngredientValues(Context, List) **/
        Utilities.insertAndUpdateIngredientValues(mContext, ingredientCVList);

        // TODO: Start the RecipeDetailsActivity???
        Toast.makeText(mContext, "Recipe saved!", Toast.LENGTH_SHORT).show();
        mSaved = true;
    }

    @Override
    public void onSaveClicked() {
        saveToDatabase();
    }

    @Override
    public void onClearClicked() {
        // Set the EditText boxes to empty
        mRecipeDescriptionEditText.setText("");
        mRecipeNameEditText.setText("");

        // Create a blank List for ingredients and directions
        mIngredientList = new LinkedList<>();
        mDirectionList = new LinkedList<>();

        // Set the ingredient and direction list to their respective Adapters
        mIngredientAdapter.setIngredientList(mIngredientList);
        mDirectionAdapter.setDirectionList(mDirectionList);

        // Clear the image from ImageView and set mRecipeImageUri to null
        clearImage();

        // Delete all auto-saved data
        deleteAutosavedData();
    }

    /**
     * A Callback for dragging and swiping items in mIngredientAdapter
     */
    private ItemTouchHelper.SimpleCallback ingredientIthCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

        /**
         * Long press to drag is disabled as it is handled through the ViewHolder's handle
         * @return Boolean value for whether it is enabled
         */
        @Override
        public boolean isLongPressDragEnabled() {
            return false;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // Get the positions of the view holder that is being moved and where its target location
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            if (fromPosition < toPosition) {
                // If the item is being moved downwards
                for (int i = fromPosition; i < toPosition; i++) {
                    // The item needs to be swapped through each item in the ingredient list until
                    // it reaches its new position
                    Collections.swap(mIngredientAdapter.getRawIngredientList(), i, i + 1);
                }
            } else {
                // If the item is being moved upwards
                for (int i = fromPosition; i > toPosition; i--) {
                    // The item needs to be swapped through each item in the ingredient list until
                    // it reaches its new position
                    Collections.swap(mIngredientAdapter.getRawIngredientList(), i, i - 1);
                }
            }

            // Set the modified ingredient list as the new list for mIngredientAdapter to use
//            mIngredientAdapter.setIngredientListWithoutNotify(newIngredientList);

            // Notify mIngredientAdapter of the change in position
            mIngredientAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Remove the item from the adapter that is being swiped
            int removedPosition = viewHolder.getAdapterPosition();
            mIngredientAdapter.deleteIngredient(removedPosition);
        }
    };

    private ItemTouchHelper.SimpleCallback directionIthCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            // Get the positions of the view holder's current position and its target position
            int fromPosition = viewHolder.getAdapterPosition();
            int toPosition = target.getAdapterPosition();

            if (fromPosition < toPosition) {
                // If item is moved downwards
                for (int i = fromPosition; i < toPosition; i++) {
                    // The item needs to be swapped through every item in the direction list until
                    // it reaches its new position
                    Collections.swap(mDirectionAdapter.getRawDirectionList(), i, i + 1);
                }
            } else {
                // If item is being moved upwards
                for (int i = fromPosition; i > toPosition; i--) {
                    Collections.swap(mDirectionAdapter.getRawDirectionList(), i, i - 1);
                }
            }

            // Notify mDirectionAdapter of the change in position
            mDirectionAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Remove the swiped item from mDirectionAdapter
            int removedPosition = viewHolder.getAdapterPosition();
            mDirectionAdapter.removeDirection(removedPosition);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            // Disable long press to drag
            return false;
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            // When item is set down, notify mDirectionAdapter to refresh the data
            mDirectionAdapter.notifyDataSetChanged();
        }
    };
}
