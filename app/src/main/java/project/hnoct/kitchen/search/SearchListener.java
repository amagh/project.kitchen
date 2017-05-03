package project.hnoct.kitchen.search;

import java.util.List;
import java.util.Map;

/**
 * Created by hnoct on 5/3/2017.
 */

public interface SearchListener {
    void onSearchFinished(List<Map<String, Object>> recipeList);
}
