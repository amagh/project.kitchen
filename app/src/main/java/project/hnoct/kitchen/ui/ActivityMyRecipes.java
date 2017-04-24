package project.hnoct.kitchen.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import project.hnoct.kitchen.R;

public class ActivityMyRecipes extends AppCompatActivity implements FragmentMyRecipes.RecipeCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
