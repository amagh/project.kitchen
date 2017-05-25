package project.kitchen.dialog;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.kitchen.R;

/**
 * Created by hnoct on 5/9/2017.
 */

public class SetAuthorDialog extends DialogFragment {
    // Member Variables
    private PositiveClickListener mListener;

    @BindView(R.id.dialog_recipe_author) EditText mAuthorEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_recipe_author, null);
        ButterKnife.bind(this, view);

        builder.setView(view);

        builder.setPositiveButton(getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String author = mAuthorEditText.getText().toString();

                if (mListener != null) {
                    mListener.onPositiveClick(author);
                }

                dismiss();
            }
        });

        return builder.create();
    }

    public interface PositiveClickListener {
        void onPositiveClick(String author);
    }

    public void setPositiveClickListener(PositiveClickListener listener) {
        mListener = listener;
    }
}
