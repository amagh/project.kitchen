package project.hnoct.kitchen.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by hnoct on 5/3/2017.
 */

public abstract class RecipeSyncService extends IntentService {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SYNC_SUCCESS, SYNC_SERVER_DOWN, SYNC_INVALID})
    public @interface SyncStatus{};

    public static final int SYNC_SUCCESS = 0;
    public static final int SYNC_SERVER_DOWN = 1;
    public static final int SYNC_INVALID = 2;

    protected RecipeSyncService(String name) {
        super(name);
    }

    abstract protected void onHandleIntent(@Nullable Intent intent);
}
