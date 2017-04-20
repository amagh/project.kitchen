package project.hnoct.kitchen.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hnoct on 3/22/2017.
 *
 * Manages Cursors for Adapters that have RecyclerViews within them. Automatically closes duplicate
 * Cursors, listens for changes in the database for the Cursors it is managing, replaces Cursors in
 * the CursorManager on changes, and notifies the Adapter of changes in Cursors so they can be
 * properly swapped
 */

public class CursorManager {
    /** Constants **/
    private static final String LOG_TAG = CursorManager.class.getSimpleName();

    /** Member Variables **/
    private Map<Integer, Cursor> mCursorMap;    // Map that correlates the position of a ViewHolder with the correct Cursor
    private Map<Cursor, Integer> mReverseMap;   // Map that correlates a given Cursor with its position
    private Context mContext;                   // Interface to global Context
    private CursorChangeListener mListener;     // Listener for notifying registered observers of a change in Cursors

    @SuppressLint("UseSparseArrays")
    public CursorManager(Context context) {
        // Initialize member variables
        mContext = context;
        mCursorMap = new HashMap<>();
        mReverseMap = new HashMap<>();
    }

    /**
     * Adds a position-Cursor pair to the Map used to manage the Cursors for an Adapter
     * @param position Position of the ViewHolder requesting the Cursor
     * @param cursor Cursor to be managed
     * @param notificationUri URI to the data that needs to be observed for changes
     * @param projection column projection to for query
     * @param sortOrder column sort order for query
     */
    private void addCursor(int position, final Cursor cursor, final Uri notificationUri, final String[] projection, final String sortOrder) {
        // Get a reference to the Cursor in the map to be replaced
        Cursor thisCursor = mCursorMap.get(position);

        // Set ContentObserver to listen for changes in the database for the referenced URI
        cursor.setNotificationUri(mContext.getContentResolver(), notificationUri);
        cursor.registerContentObserver(new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);

                // Re-query the database with the initial parameters passed from the Adapter
                Cursor newCursor = mContext.getContentResolver().query(
                        notificationUri,
                        projection,
                        null,
                        null,
                        sortOrder
                );

                // Get the position of the ViewHolder of the Cursor that is being replaced
                int position = mReverseMap.get(cursor);

                // Recursively add the new Cursor to the Manager
                addCursor(position, newCursor, notificationUri, projection, sortOrder);

                // If there is a registered observer, notify the Adapter there is a change in a given position
                if (mListener != null) {
                    mListener.onCursorChanged(position);
                }
            }
        });

        // Add the Cursor to the Map as well as a reference to its position in mReverseMap
        mCursorMap.put(position, cursor);
        mReverseMap.put(cursor, position);

        if (thisCursor != null) {
            // Close the Cursor that is being replaced
            thisCursor.close();
        }
    }

    /**
     * @see #addCursor(int, Cursor, Uri, String[], String)
     * @param position position of the ViewHolder that requesting the Cursor
     * @param cursor Cursor that is being managed
     */
    public void addCursor(int position, Cursor cursor) {
        Cursor thisCursor = mCursorMap.get(position);
        if (thisCursor != null) {
            thisCursor.close();
        }

        mCursorMap.put(position, cursor);
        mReverseMap.put(cursor, position);
    }

    /**
     * For adding Cursors that are already being managed by a CursorLoader
     * @param position position of the ViewHolder requesting the Cursor
     * @param cursor Cursor to be managed
     */
    public void addManagedCursor(int position, Cursor cursor) {
//        Log.d(LOG_TAG, "Cursor has been added to the Map!");
        mCursorMap.put(position, cursor);
        mReverseMap.put(cursor, position);

        // If there is a registered observer, notify the Adapter there is a change in a given position
        if (mListener != null) {
            mListener.onCursorChanged(mReverseMap.get(cursor));
        }
    }

    /**
     * Closes a Cursor of a given ViewHolder
     * @param position Position of the ViewHolder that requested the Cursor
     * @return boolean true if a Cursor was closed, false if no corresponding Cursor was found
     */
    public boolean closeCursor(int position) {
        Cursor thisCursor = mCursorMap.get(position);

        if (thisCursor != null) {
            thisCursor.close();
            return true;
        }

        return false;
    }

    /**
     * Returns a Cursor for a given ViewHolder's position
     * @param position position of the ViewHolder requesting the Cursor
     * @return Cursor for the ViewHolder
     */
    public Cursor getCursor(int position) {
        return mCursorMap.get(position);
    }

    /**
     * Closes all Cursors being managed
     * @return int number of Cursors that were closed
     */
    public int closeAllCursors() {
        int cursorsClosed = 0;
        for (Cursor cursor : mCursorMap.values()) {
            cursor.close();
            cursorsClosed++;
        }
        return cursorsClosed;
    }

    /**
     * Returns the number of Cursors being managed
     * @return int number of Cursor being managed
     */
    public int getCursorCount() {
        return mCursorMap.size();
    }

    /**
     * Interface for an Observer a listener for when a Cursor has been changed
     */
    public interface CursorChangeListener {
        void onCursorChanged(int position);
    }

    /**
     * Sets the CursorChangeListener for the CursorManager Object
     * @param listener
     */
    public void setCursorChangeListener(CursorChangeListener listener) {
        // Set the member variable listener to the registered listener
        mListener = listener;
    }
}
