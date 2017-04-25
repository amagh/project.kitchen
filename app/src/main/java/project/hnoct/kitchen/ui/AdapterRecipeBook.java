package project.hnoct.kitchen.ui;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.inputmethodservice.InputMethodService;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.Optional;
import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.CursorManager;
import project.hnoct.kitchen.data.RecipeContract.*;

/**
 * Created by hnoct on 3/25/2017.
 */

public class AdapterRecipeBook extends RecyclerView.Adapter<AdapterRecipeBook.RecipeBookViewHolder> {
    /** Constants **/
    private static final String LOG_TAG = AdapterRecipeBook.class.getSimpleName();
    private static final int RECIPE_BOOK_VIEW = 0;
    private static final int RECIPE_BOOK_VIEW_EDIT = 1;

    /** Member Variables **/
    private Context mContext;
    private Cursor mCursor;
    private CursorManager mCursorManager;
    private RecipeBookAdapterOnClickHandler mClickHandler;
    private int editBook = -1;

    public AdapterRecipeBook(Context context, RecipeBookAdapterOnClickHandler clickHandler, CursorManager cursorManager) {
        // Initialize member variables
        mContext = context;
        mClickHandler = clickHandler;
        mCursorManager = cursorManager;

        // Register a listener to mCursorManager to listen for changes in its Cursors
        mCursorManager.setCursorChangeListener(new CursorManager.CursorChangeListener() {
            @Override
            public void onCursorChanged(int position) {
                // Notify the correct ViewHolder of a change in its data
                notifyItemChanged(position);
            }
        });
    }

    public AdapterRecipeBook(Context context, RecipeBookAdapterOnClickHandler clickHandler) {
        mContext = context;
        mClickHandler = clickHandler;
    }

    @Override
    public RecipeBookViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (parent instanceof RecyclerView) {
            View view;
            switch (viewType) {
                case RECIPE_BOOK_VIEW: {
                    // Inflate the layout for the recipe book list items
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_recipebook, parent, false);
                    view.setFocusable(true);
                    break;
                }

                case RECIPE_BOOK_VIEW_EDIT: {
                    // Inflate the layout for the recipe book to be edited
                    view = LayoutInflater.from(mContext).inflate(R.layout.list_item_recipebook_edit, parent, false);
                    view.setFocusable(true);
                    break;
                }

                default: throw new UnsupportedOperationException("Unknown view type: " + viewType);
            }

            // Set the inflated view to the view holder
            return new RecipeBookViewHolder(view);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecipeBookViewHolder holder, int position) {
        // Move the cursor to the correct entry in the database
        mCursor.moveToPosition(position);

        // Retrieve the information from database to populate the view holder
        long bookId = mCursor.getLong(RecipeBookEntry.IDX_BOOK_ID);
        String bookTitle = mCursor.getString(RecipeBookEntry.IDX_BOOK_NAME);
        String bookDescription = mCursor.getString(RecipeBookEntry.IDX_BOOK_DESCRIPTION);

        // Populate the views of the view holder
        holder.recipeBookTitleText.setText(bookTitle);
        holder.recipeBookDescriptionText.setText(bookDescription);

        // Retrieve the Cursor from mCursorManager
        Cursor cursor = mCursorManager != null ? mCursorManager.getCursor(position) : null;

        if (cursor != null && cursor.moveToFirst()) {
            // Set the first recipe image of each chapter to the correct thumbnail in the recipe
            // book layout
            String image0Url = cursor.getString(LinkRecipeBookEntry.IDX_IMG_URL);
            String image1Url, image2Url, image3Url;
            holder.image0.setVisibility(View.VISIBLE);
            holder.gradient.setVisibility(View.VISIBLE);
            Glide.with(mContext)
                    .load(image0Url)
                    .into(holder.image0);
            if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER) <= 0) {
                image1Url = cursor.getString(LinkRecipeBookEntry.IDX_IMG_URL);
                holder.image1.setVisibility(View.VISIBLE);
                Glide.with(mContext)
                        .load(image1Url)
                        .into(holder.image1);
                if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER) <= 0) {
                    image2Url = cursor.getString(LinkRecipeBookEntry.IDX_IMG_URL);
                    holder.image2.setVisibility(View.VISIBLE);
                    Glide.with(mContext)
                            .load(image2Url)
                            .into(holder.image2);
                    if (cursor.moveToNext() && cursor.getInt(LinkRecipeBookEntry.IDX_RECIPE_ORDER) <= 0) {
                        image3Url = cursor.getString(LinkRecipeBookEntry.IDX_IMG_URL);
                        holder.image3.setVisibility(View.VISIBLE);
                        Glide.with(mContext)
                                .load(image3Url)
                                .into(holder.image3);
                    } else {
                        // Set any unused ImageViews to GONE
                        holder.image3.setVisibility(View.GONE);
                    }
                } else {
                    holder.image2.setVisibility(View.GONE);
                    holder.image3.setVisibility(View.GONE);
                }
            } else {
                holder.image1.setVisibility(View.GONE);
                holder.image2.setVisibility(View.GONE);
                holder.image3.setVisibility(View.GONE);
            }
        } else {
            Log.d(LOG_TAG, "Hiding all views for position " + position);
            holder.image0.setVisibility(View.GONE);
            holder.image1.setVisibility(View.GONE);
            holder.image2.setVisibility(View.GONE);
            holder.image3.setVisibility(View.GONE);
            holder.gradient.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position != editBook) {
            return RECIPE_BOOK_VIEW;
        } else {
            return RECIPE_BOOK_VIEW_EDIT;
        }
    }

    /**
     * Callback interface sending information about which recipe book as been selected so the
     * ChapterActivity can be correctly called
     */
    public interface RecipeBookAdapterOnClickHandler {
        void onClick(RecipeBookViewHolder viewHolder, long bookId);
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public void swapCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

    public class RecipeBookViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.list_recipebook_image_0) ImageView image0;
        @BindView(R.id.list_recipebook_image_1) ImageView image1;
        @BindView(R.id.list_recipebook_image_2) ImageView image2;
        @BindView(R.id.list_recipebook_image_3) ImageView image3;
        @BindView(R.id.list_recipebook_gradient) ImageView gradient;
        @BindView(R.id.list_recipebook_title) TextView recipeBookTitleText;
        @BindView(R.id.list_recipebook_description_text) TextView recipeBookDescriptionText;
        @Nullable @BindView(R.id.list_recipebook_edit) ImageView editButton;

        @Optional
        @OnClick (R.id.list_recipebook_edit)
        void onEditClicked(View view) {
            int position = getAdapterPosition();
            if (position != editBook) {
                editBook = position;
            } else {
                // Disable editing of the recipe book
                editBook = -1;

                // Retrieve the new recipe book informatin
                String bookTitle = recipeBookTitleText.getText().toString();
                String bookDescription = recipeBookDescriptionText.getText().toString();

                // Create ContentValues containing the new information
                ContentValues bookValues = new ContentValues();
                bookValues.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_NAME, bookTitle);
                bookValues.put(RecipeBookEntry.COLUMN_RECIPE_BOOK_DESCRIPTION, bookDescription);

                // Retrieve the recipe book ID so the values can be updated properly
                mCursor.moveToPosition(position);
                long bookId = mCursor.getLong(RecipeBookEntry.IDX_BOOK_ID);

                // Selection and selection arguments
                String selection = RecipeBookEntry.COLUMN_RECIPE_BOOK_ID + " = ?";
                String[] selectionArgs = new String[] {Long.toString(bookId)};

                // Update the database with the new values
                mContext.getContentResolver().update(
                        RecipeBookEntry.CONTENT_URI,
                        bookValues,
                        selection,
                        selectionArgs
                );

                // Hide the soft keyboard if showing
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            // Notify the adapter of the change
            notifyItemChanged(position);
        }

        @Optional
        @OnEditorAction(R.id.list_recipebook_title)
        boolean onEditorAction(int actionId) {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                recipeBookDescriptionText.requestFocus();
                return true;
            }
            return false;
        }

        public RecipeBookViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            // Pass the recipe book ID of the clicked item to the click handler
            int position = getAdapterPosition();

            // Prevent user from accidentally opening the book while in edit-mode
            if (position != editBook) {
                mCursor.moveToPosition(position);
                long bookId = mCursor.getLong(RecipeBookEntry.IDX_BOOK_ID);
                mClickHandler.onClick(this, bookId);
            }

        }
    }
}
