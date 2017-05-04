package project.hnoct.kitchen.sync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import project.hnoct.kitchen.R;
import project.hnoct.kitchen.data.Utilities;

/**
 * Created by hnoct on 5/3/2017.
 */

public class RecipeGcmService extends GcmTaskService {
    @Override
    public int onRunTask(TaskParams taskParams) {
        // Retrieve current time to use as the time to seed all recipe additions
        long seedTime = Utilities.getCurrentTime();

        // Start the recipe sync Services and pass along seedTime
        Intent allRecipesIntent = new Intent(this, AllRecipesService.class);
        allRecipesIntent.putExtra(getString(R.string.extra_time), seedTime);
        startService(allRecipesIntent);

        Intent epicuriousIntent = new Intent(this, EpicuriousService.class);
        epicuriousIntent.putExtra(getString(R.string.extra_time), seedTime);
        startService(epicuriousIntent);

        Intent foodIntent = new Intent(this, FoodDotComService.class);
        foodIntent.putExtra(getString(R.string.extra_time), seedTime);
        startService(foodIntent);

        Intent seriousIntent = new Intent(this, SeriousEatsService.class);
        seriousIntent.putExtra(getString(R.string.extra_time), seedTime);
        startService(seriousIntent);



        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
