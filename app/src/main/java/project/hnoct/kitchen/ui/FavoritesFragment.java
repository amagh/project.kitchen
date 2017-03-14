package project.hnoct.kitchen.ui;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

/**
 * A placeholder fragment containing a simple view.
 */
public class FavoritesFragment extends Fragment {
    /** Constants **/
    private static final String LOG_TAG = FavoritesFragment.class.getSimpleName();

    /** Member Variables **/
    Context mContext;
    LayoutInflater mInflater;

    @BindView(R.id.favorites_index) LinearLayout mIndex;

    public FavoritesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, rootView);
        mContext = getActivity();
        mInflater = inflater;

        populateIndex();

        return rootView;
    }

    private void populateIndex() {
        String alphabet = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        for (int i = 0; i < alphabet.length(); i++) {
            TextView textView = (TextView) mInflater.inflate(R.layout.list_item_alphabet_index, null);
            textView.setText(Character.toString(alphabet.charAt(i)));
            textView.setLayoutParams(params);
            mIndex.addView(textView);
        }
    }
}
