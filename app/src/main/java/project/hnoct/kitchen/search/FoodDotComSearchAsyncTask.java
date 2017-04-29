package project.hnoct.kitchen.search;

import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;
import java.util.Map;

/**
 * Created by hnoct on 4/28/2017.
 */

public class FoodDotComSearchAsyncTask extends AsyncTask<Object, Void, List<Map<String, Object>>> {
    @Override
    protected List<Map<String, Object>> doInBackground(Object... params) {
        Document document = Jsoup.parse((String) params[0]);
        return null;
    }
}
