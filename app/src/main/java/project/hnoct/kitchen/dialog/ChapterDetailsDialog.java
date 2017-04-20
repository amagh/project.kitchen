package project.hnoct.kitchen.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 4/4/2017.
 */

public class ChapterDetailsDialog extends DialogFragment {
    /** Constants **/

    /** Member Variables **/
    private ChapterDetailsListener mListener;

    @BindView(R.id.dialog_chapter_title_edit_text) EditText titleEditText;
    @BindView(R.id.dialog_chapter_description_edit_text) EditText descriptionEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Build the dialog to show the user
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Set the layout to be used by the dialog
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_chapter_details, null);

        ButterKnife.bind(this, view);

        builder.setView(view)
                .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Send a Callback to the MainActivity so that the new recipe can be added
                        String titleInputText = null, descriptionInputText = null;
                        if (titleEditText != null) {
                            // Get the title from user input
                            titleInputText = titleEditText.getText().toString();
                        }

                        if (descriptionEditText != null) {
                            // Get the description from user input
                            descriptionInputText = descriptionEditText.getText().toString();
                        }

                        if (titleInputText != null || descriptionInputText != null) {
                            // Send the Callback to the Activity that called the dialog
                            if (mListener != null) mListener.onPositiveDialogClick(
                                    ChapterDetailsDialog.this,
                                    titleInputText,
                                    descriptionInputText
                            );
                        }
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing. Will dismiss the dialog as expected
                    }
                });

        return builder.create();
    }

    public void setPositiveClickListener(ChapterDetailsListener listener) {
        mListener = listener;
    }

    /**
     * Callback interface to pass the entered information to the RecipeBookActivity
     */
    public interface ChapterDetailsListener {
        void onPositiveDialogClick(DialogFragment dialog, String titleText, String descriptionText);
    }
}
