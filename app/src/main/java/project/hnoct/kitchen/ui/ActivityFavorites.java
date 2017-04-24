package project.hnoct.kitchen.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import project.hnoct.kitchen.R;

public class ActivityFavorites extends AppCompatActivity implements FragmentFavorites.RecipeCallBack {

    @BindView(R.id.toolbar) Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onItemSelected(String recipeUrl, AdapterRecipe.RecipeViewHolder viewHolder) {
        // Start the ActivityRecipeDetails utilizing the URL of the recipe
        Intent intent = new Intent(this, ActivityRecipeDetails.class);
        intent.setData(Uri.parse(recipeUrl));
        startActivity(intent);
    }
}
