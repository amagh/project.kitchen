package project.hnoct.kitchen.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.RecipeContract;
import project.hnoct.kitchen.data.RecipeContract.*;
import project.hnoct.kitchen.ui.adapter.AdapterIngredient;

/**
 * A placeholder fragment containing a simple view.
 */
public class FragmentShoppingList extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    // Constants
    private static final String LOG_TAG = FragmentShoppingList.class.getSimpleName();
    private final int SHOPPING_LOADER = 5;

    // Member Variables
    private Context mContext;
    private Cursor mCursor;
    private AdapterIngredient mAdapter;
    private LinearLayoutManager mLayoutManager;

    private boolean animateCard = false;
    private boolean showFab = false;

    // ButterKnife Bounds Views
    @BindView(R.id.shopping_list_recyclerView) RecyclerView mRecyclerView;
    @BindView(R.id.shopping_list_info_cardview) CardView mCardView;

    public FragmentShoppingList() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_list, container, false);
        ButterKnife.bind(this, rootView);

        // Initialize member variables
        mContext = getActivity();

        // Setup the AdapterIngredient
        mAdapter = new AdapterIngredient(mContext);
        mAdapter.setHasStableIds(true);     // Allows animatios to occur correctly when calling notifyDatasetChanged
        mAdapter.useAsShoppingList();
        mAdapter.setCheckListener(new AdapterIngredient.CheckListener() {
            @Override
            public void onChecked(int itemsChecked) {
                // Show the FAB if there is at least one item checked off
                if (itemsChecked > 0) {
                    if (!((ActivityShoppingList)getActivity()).mDeleteFab.isShown()) {
                        ((ActivityShoppingList)getActivity()).mDeleteFab.show();
                    }

                    // Set the member variable to true to inform the ScrollListener to show the
                    // FAB when finished scrolling
                    showFab = true;

                } else {
                    if (((ActivityShoppingList)getActivity()).mDeleteFab.isShown()) {
                        ((ActivityShoppingList)getActivity()).mDeleteFab.hide();
                    }

                    // Set the member variable so the ScrollListener does not show the FAB
                    showFab = false;
                }
            }
        });

        mLayoutManager = new LinearLayoutManager(mContext);

        // Set the Adapter and LayoutManager for the RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);

        // Set a ScrollListener to show the FAB when the user finishes scrolling
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && ((ActivityShoppingList)getActivity()).mDeleteFab.isShown()) {
                    // Hide the FAB while the user is scrolling
                    ((ActivityShoppingList)getActivity()).mDeleteFab.hide();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE && showFab) {
                    // Show the FAB when the user finishes scrolling
                    ((ActivityShoppingList)getActivity()).mDeleteFab.show();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Set up parameters for creating the CursorLoader
        Uri linkUri = LinkIngredientEntry.CONTENT_URI;
        String[] projection = LinkIngredientEntry.LINK_PROJECTION;
        String selection = LinkIngredientEntry.COLUMN_SHOPPING + " = ?";
        String[] selectionArgs = new String[] {"1"};
        String sortOrder = RecipeEntry.COLUMN_RECIPE_NAME + " ASC, " +
                LinkIngredientEntry.COLUMN_INGREDIENT_ORDER + " ASC";

        // Create and return the CursorLoader
        return new CursorLoader(mContext,
                linkUri,
                projection,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            // Set the member variable as the returned Cursor
            mCursor = cursor;

            // Swap mCursor into mAdapter
            mAdapter.swapCursor(mCursor);

            if (mAdapter.getToggleStatus()) {
                mAdapter.toggleChecked();
            }

            mAdapter.addRecipeTitles();

            // Check if the Cursor returned any rows
            if (cursor.moveToFirst()) {
                // Hide the Card with information for user
                mCardView.setVisibility(View.GONE);

                // Set the boolean to animate the card when all items are checked off shopping list
                animateCard = true;
            } else {
                // Check whether to play animation for the Card, then show card w/wo animation
                if (animateCard) {
                    animateCard();
                } else {
                    // *Animation does not play smoothly when Activity is first started so the
                    // CardView is set to visible to work around it.
                    mCardView.setVisibility(View.VISIBLE);
                }
            }

            // Show the FAB is there is at least one item already checked
            if (mAdapter.getItemsCheckedCount() > 0) {
                ((ActivityShoppingList)getActivity()).mDeleteFab.show();
            } else {
                showFab = false;
                ((ActivityShoppingList)getActivity()).mDeleteFab.hide();
            }
        }
    }

    /**
     * Animation for the intro of the CardView informing the user that the shopping list is empty
     */
    private void animateCard() {
        // Initialize the interpolater to be used for the animation
        AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();

        // Instantiate the AnimatorSet
        AnimatorSet animSet = new AnimatorSet();

        // Animate the X-scale
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(mCardView, "scaleX", 0.1f, 1.0f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(interpolator);

        // Animate the Y-scale
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(mCardView, "scaleY", 0.1f, 1.0f);
        scaleXAnim.setDuration(300);
        scaleXAnim.setInterpolator(interpolator);

        // Set the animations to play together
        animSet.playTogether(scaleXAnim, scaleYAnim);
        animSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Ensure the CardView is visible
                mCardView.setVisibility(View.VISIBLE);
            }
        });

        // Start the animation
        animSet.start();
    }

    @Override
    public void onPause() {
        // Save checked items to database
        saveCheckedItems();

        super.onPause();
    }

    /**
     * Save all checked items to the database so their values can be retrieved later
     */
    private void saveCheckedItems() {
        // Initialize the Array that will hold the corresponding recipe and ingredient IDs
        boolean[] checkedArray = mAdapter.getListCheckedArray();
        int[] recipeIdArray = new int[checkedArray.length];
        int[] ingredientIdArray = new int[checkedArray.length];

        // Create an Array of matching recipe and ingredient IDs
        for (int i = 0; i < recipeIdArray.length; i++) {
            mCursor.moveToPosition(i);
            recipeIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_ID);
            ingredientIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_INGREDIENT_ID);
        }

        // Run the AsyncTask that will update the database with the new checked status of the items
        ModifyDatabaseChecked checkedAsyncTask = new ModifyDatabaseChecked(recipeIdArray, ingredientIdArray, checkedArray);
        checkedAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * AsyncTask for storing the checkedArray value of an ingredient
     */
    private class ModifyDatabaseChecked extends AsyncTask<Void, Void, Void> {
        // Member Variables
        int[] recipeIdArray;
        int[] ingredientIdArray;
        boolean[] checkedArray;

        public ModifyDatabaseChecked(int[] recipeIdArray, int[] ingredientIdArray, boolean[] checkedArray) {
            // Set up the parameters for modifying the database
            this.recipeIdArray = recipeIdArray;
            this.ingredientIdArray = ingredientIdArray;
            this.checkedArray = checkedArray;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            // Set up the update operation parameters
            String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";

            // Build a ContentProviderOperation for each item in the Array
            for (int i = 0; i < recipeIdArray.length; i++) {
                // Initialize the selection arguments
                String[] selectionArgs = new String[]{Integer.toString(recipeIdArray[i]),
                        Integer.toString(ingredientIdArray[i])};

                // Builder the update operation
                ContentProviderOperation operation =
                        ContentProviderOperation.newUpdate(LinkIngredientEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING_CHECKED, checkedArray[i] ? 1 : 0)
                                .build();

                // Add the operation to the List of operations to be performed on the database
                operations.add(operation);
            }

            // Batch apply all update operations
            try {
                mContext.getContentResolver().applyBatch(
                        RecipeContract.CONTENT_AUTHORITY,
                        operations
                );
            } catch (OperationApplicationException | RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Swap in a null Cursor
        mAdapter.swapCursor(null);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Initiailize the CursorLoader
        getLoaderManager().initLoader(SHOPPING_LOADER, null, this);
    }

    /**
     * Sets up the AsyncTask to modify items in the database so that they are removed from mAdapter
     */
    public void deleteCheckedItems() {
        // Initialize Arrays to be passed to ModifyDatabaseDeleteChecked
        boolean[] checkedArray = mAdapter.getListCheckedArray();
        int[] recipeIdArray = new int[checkedArray.length];
        int[] ingredientIdArray = new int[checkedArray.length];

        // Create an Array of matching recipe and ingredient IDs
        for (int i = 0; i < recipeIdArray.length; i++) {
            mCursor.moveToPosition(i);
            recipeIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_RECIPE_ID);
            ingredientIdArray[i] = mCursor.getInt(LinkIngredientEntry.IDX_INGREDIENT_ID);
        }

        // Call the AsyncTask
        ModifyDatabaseDeleteChecked deleteAsyncTask = new ModifyDatabaseDeleteChecked(recipeIdArray, ingredientIdArray, checkedArray);
        deleteAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * AsyncTask for modifying the database in the background to flip the bit used to hold the value
     * determining whether AdapterIngredient should show the item
     */
    private class ModifyDatabaseDeleteChecked extends AsyncTask<Void, Void, Void> {
        // Member Variables
        int[] recipeIdArray;
        int[] ingredientIdArray;
        boolean[] checkedArray;


        public ModifyDatabaseDeleteChecked(int[] recipeIdArray, int[] ingredientIdArray, boolean[] checkedArray) {
            // Set up the parameters for modifying the database
            this.recipeIdArray = recipeIdArray;
            this.ingredientIdArray = ingredientIdArray;
            this.checkedArray = checkedArray;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Instantiate the ArrayList that will hold the update operations
            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            // Set up the update operation parameters
            String selection = RecipeEntry.COLUMN_RECIPE_ID + " = ? AND " +
                    IngredientEntry.COLUMN_INGREDIENT_ID + " = ?";

            // Build a ContentProviderOperation for each item in the Array
            for (int i = 0; i < recipeIdArray.length; i++) {
                // Initialize the selection arguments
                String[] selectionArgs = new String[]{Integer.toString(recipeIdArray[i]),
                        Integer.toString(ingredientIdArray[i])};

                // Builder the update operation
                ContentProviderOperation operation =
                        ContentProviderOperation.newUpdate(LinkIngredientEntry.CONTENT_URI)
                                .withSelection(selection, selectionArgs)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING_CHECKED, 0)
                                .withValue(LinkIngredientEntry.COLUMN_SHOPPING, checkedArray[i] ? 0 : 1)
                                .build();

                // Add the operation to the List of operations to be performed on the database
                operations.add(operation);
            }

            // Batch apply all update operations
            try {
                mContext.getContentResolver().applyBatch(
                        RecipeContract.CONTENT_AUTHORITY,
                        operations
                );
            } catch (OperationApplicationException | RemoteException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (mCursor.getCount() == 0) {
                animateCard();
            }
        }
    }
}
