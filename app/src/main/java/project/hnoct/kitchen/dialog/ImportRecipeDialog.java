package project.hnoct.kitchen.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

/**
 * Created by hnoct on 3/4/2017.
 */

public class ImportRecipeDialog extends DialogFragment {
    /** Constants **/

    /** Member Variables **/
    private ImportRecipeDialogListener mListener;

    // Bound by ButterKnife
    @BindView(R.id.dialog_import_text_input) EditText mEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Inflate layout used for dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_import_recipe, null);

        // Bind views with ButterKnife
        ButterKnife.bind(this, view);

        // Set the new layout as the dialog's view
        builder.setView(view)
                .setPositiveButton(R.string.button_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Send a Callback to the MainActivity so that the new recipe can be added
                        if (mEditText != null) {
                            // Get the URL from the EditText
                            String inputText = mEditText.getText().toString();

                            if (!inputText.trim().isEmpty()) {
                                // If user did not input any text, dismiss the Dialog
                                mListener.onDialogPositiveClick(inputText);
                            }
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

    /**
     * Callback interface to pass information back to Activity
     */
    public interface ImportRecipeDialogListener {
        public void onDialogPositiveClick(String recipeUrl);
    }

    public void addImportRecipeDialogListener(ImportRecipeDialogListener listener) {
        mListener = listener;
    }
}
