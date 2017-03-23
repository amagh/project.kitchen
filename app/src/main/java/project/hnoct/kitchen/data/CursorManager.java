package project.hnoct.kitchen.data;

import android.annotation.SuppressLint;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hnoct on 3/22/2017.
 */

public class CursorManager {
    private Map<Integer, Cursor> mCursorMap;

    @SuppressLint("UseSparseArrays")
    public CursorManager() {
        mCursorMap = new HashMap<>();
    }

    public void addCursor(int position, Cursor cursor) {
        Cursor thisCursor = mCursorMap.get(position);
        if (thisCursor != null) {
            thisCursor.close();
        }

        mCursorMap.put(position, cursor);
    }

    public boolean closeCursor(int position) {
        Cursor thisCursor = mCursorMap.get(position);

        if (thisCursor != null) {
            thisCursor.close();
            return true;
        }

        return false;
    }

    public Cursor getCursor(int position) {
        return mCursorMap.get(position);
    }

    public int closeAllCursors() {
        int cursorsClosed = 0;
        for (Cursor cursor : mCursorMap.values()) {
            cursor.close();
            cursorsClosed++;
        }
        return cursorsClosed;
    }

    public int getCursorCount() {
        return mCursorMap.size();
    }
}
