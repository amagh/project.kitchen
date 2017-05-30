package project.kitchen.prefs;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Query;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import project.kitchen.R;
import project.kitchen.data.RecipeContract;
import project.kitchen.data.RecipeDbHelper;

/**
 * Created by hnoct on 5/24/2017.
 */

public class BackupRestoreActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {
    // Constants
    private static final String LOG_TAG = BackupRestoreActivity.class.getSimpleName();

    private final int RESOLVE_CONNECTION_REQUEST_CODE = 2;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private java.io.File EXPORT_DB_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    private java.io.File EXPORT_IMAGE_PATH;
    private File IMAGE_DIRECTORY;

    private String EXPORT_DB_FILE_NAME = "recipe.db";
    private GoogleApiClient mGoogleApiClient;

    // Member Variables
    Activity activity = this;
    Context mContext;
    RecipeDbHelper dbHelper;
    List<File> imageFiles;
    List<String> fileTitleStrings;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BACKUP, RESTORE})
    private @interface BackupRestore{}
    private static final int BACKUP = 0;
    private static final int RESTORE = 1;

    @BackupRestoreActivity.BackupRestore  private int mProcedure;

    DriveId dbDriveId;

    @BindView(R.id.backup_local) TextView mLocalBackup;
    @BindView(R.id.restore_local) TextView mLocalRestore;
    @BindView(R.id.backup_drive) TextView mDriveBackup;
    @BindView(R.id.restore_drive) TextView mDriveRestore;

    @OnClick(R.id.backup_local)
    void onClickLocalBackup(View view) {
        backup();
    }

    @OnClick(R.id.restore_local)
    void onClickLocalRestore(View view) {
        restore();
    }

    @OnClick(R.id.backup_drive)
    void onClickDriveBackup(View view) {
        queryDriveForDelete();
    }

    @OnClick(R.id.restore_drive)
    void onClickDriveRestore(View view) {
        queryDriveForRestore();
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);
        ButterKnife.bind(this);

        mContext = this;
        dbHelper = new RecipeDbHelper(this);

        EXPORT_DB_PATH = new File(EXPORT_DB_PATH, "project.kitchen");
        EXPORT_IMAGE_PATH = new File(EXPORT_DB_PATH, "images");
        IMAGE_DIRECTORY = getDir(
                getString(R.string.food_image_dir),
                Context.MODE_PRIVATE
        );
    }

    /**
     * Creates a copy of the database file and saves it to the public Download folder
     */
    public void backup() {
        // Check to ensure the app has sufficient permissions to save the backed up file to the
        // external storage
        if (!checkStoragePermissions(activity)) {
            Toast.makeText(this, getString(R.string.toast_local_failed), Toast.LENGTH_LONG).show();
            return;
        }

        // Init the src and dst files
        java.io.File exportDbFile;
        java.io.File currentDbFile = getDatabasePath(dbHelper.getDatabaseName());

        try {
            // Create the directory if it doesn't exist
            EXPORT_DB_PATH.mkdirs();

            // Get a reference to the output file
            exportDbFile = new java.io.File(EXPORT_DB_PATH, EXPORT_DB_FILE_NAME);

            if (exportDbFile.exists()) {
                // Delete the file it it already exists
                exportDbFile.delete();
            }

            if (currentDbFile.exists()) {
                // Open a FileChannel to the source and the destination
                FileChannel src = new FileInputStream(currentDbFile).getChannel();
                FileChannel dst = new FileOutputStream(exportDbFile).getChannel();

                // Copy the file to the external directory
                dst.transferFrom(src, 0, src.size());

                // Close the FileChannels
                src.close();
                dst.close();
            }

            Toast.makeText(this, getString(R.string.toast_backup_local_success), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.toast_local_failed), Toast.LENGTH_LONG).show();
        }

        // Save image files to a subdirectory on the the user's internal storage
        File[] imageList = IMAGE_DIRECTORY.listFiles();
        EXPORT_IMAGE_PATH.mkdir();

        for (File file : imageList) {
            java.io.File backupImage = new File(EXPORT_IMAGE_PATH, file.getName());

            try {
                FileChannel src = new FileInputStream(file).getChannel();
                FileChannel dst = new FileOutputStream(backupImage).getChannel();

                dst.transferFrom(src, 0, src.size());

                src.close();
                dst.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Restores a previously backed up database file from the public Download folder
     */
    public void restore() {
        // Check that the application has sufficient permissions to read from storage
        if (!checkStoragePermissions(activity)) {
            Toast.makeText(this, getString(R.string.toast_local_failed), Toast.LENGTH_LONG).show();
            return;
        }

        // Obtain a File reference to the backed up file and the current DB
        java.io.File restoreDbFile = new java.io.File(EXPORT_DB_PATH, EXPORT_DB_FILE_NAME);
        java.io.File currentDbFile = getDatabasePath(dbHelper.getDatabaseName());

        if (!restoreDbFile.exists()) {
            // If there is no back up to restore, inform the user
            Toast.makeText(this,getString(R.string.toast_local_no_backup), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // Open a FileChannel to the source and destination
            FileChannel src = new FileInputStream(restoreDbFile).getChannel();
            FileChannel dst = new FileOutputStream(currentDbFile).getChannel();

            // Transfer the file
            dst.transferFrom(src, 0, src.size());

            // Close the FileChannels
            src.close();
            dst.close();


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.toast_local_failed), Toast.LENGTH_LONG).show();
        }

        // Get reference to directory containing backed up resources
        File backupImageDirectory = EXPORT_IMAGE_PATH;
        File[] imageList = backupImageDirectory.listFiles();

        // Restore all backed up images
        for (File imageFile : imageList) {

            if (imageFile.getName().equals(dbHelper.getDatabaseName())) {
                // Do not restore database File
                continue;
            }

            // Create the image file in the app's private directory
            java.io.File restoreImage = new File(IMAGE_DIRECTORY, imageFile.getName());

            try {
                // Copy File
                FileChannel src = new FileInputStream(imageFile).getChannel();
                FileChannel dst = new FileOutputStream(restoreImage).getChannel();

                dst.transferFrom(src, 0, src.size());

                src.close();
                dst.close();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, getString(R.string.toast_local_failed), Toast.LENGTH_LONG).show();
            }
        }

        // Inform the user of the successful restore
        Toast.makeText(this, getString(R.string.toast_restore_local_success), Toast.LENGTH_LONG).show();

        // Edit SharedPreferences so number of deleted recipes matches the number deleted in the
        // restored database File
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        int oldDeleted = prefs.getInt(getString(R.string.recipes_deleted_key), 0);

        // Query the database and use the difference between the last recipeId and the total count
        // as the number deleted
        Cursor cursor = getContentResolver().query(
                RecipeContract.RecipeEntry.CONTENT_URI,
                RecipeContract.RecipeEntry.RECIPE_PROJECTION,
                null,
                null,
                RecipeContract.RecipeEntry.COLUMN_RECIPE_ID + " DESC"
        );

        if (cursor == null) {
            return;
        }

        cursor.moveToFirst();
        int lastId = cursor.getInt(RecipeContract.RecipeEntry.IDX_RECIPE_ID);
        int count = cursor.getCount();

        int deleted = lastId - count;

        editor.putInt(getString(R.string.recipes_deleted_key), deleted);
        editor.apply();

        cursor.close();
    }

    /**
     * Checks whether the app has the correction read/write permissions to the storage
     * @param activity Interface to global Context
     */
    private boolean checkStoragePermissions(Activity activity) {
        // Check if the application has write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // Prompt the user to give permissions if it hasn't been granted yet
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        } else {
            return true;
        }
    }

    /**
     * Queries Google Drive's appDataFolder to see if the database has been previously saved. If so,
     * it deletes the file so that only one copy of the database remains
     */
    public void queryDriveForDelete() {
        // Build the Client to connect to GoogleDrive
        buildGoogleApiClient();

        // Set the boolean to indicate the backup procedure
        mProcedure = BACKUP;

        // Register/Unregister ConnectionCallbacks
        mGoogleApiClient.unregisterConnectionCallbacks(backupCallbacks);
        mGoogleApiClient.registerConnectionCallbacks(queryCallbacks);

        // Connect
        mGoogleApiClient.connect();
    }

    public void backupToDrive() {
        // Build the Client to connect to GoogleDrive
        buildGoogleApiClient();

        // Register/Unregister ConnectionCallbacks
        mGoogleApiClient.unregisterConnectionCallbacks(queryCallbacks);
        mGoogleApiClient.registerConnectionCallbacks(backupCallbacks);

        // Connect
        mGoogleApiClient.connect();
    }

    public void queryDriveForRestore() {
        // Build the Client to connect to GoogleDrive
        buildGoogleApiClient();

        // Set the boolean to indicate the restore procedure
        mProcedure = RESTORE;

        // Register/Unregister ConnectionCallbacks
        mGoogleApiClient.unregisterConnectionCallbacks(backupCallbacks);
        mGoogleApiClient.registerConnectionCallbacks(queryCallbacks);

        // Connect
        mGoogleApiClient.connect();
    }

    /**
     * Builds the GoogleApiClient and references the member variable mGoogleApiClient to it
     */
    private void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_APPFOLDER)        // Required to access the appDataFolder
                    .addOnConnectionFailedListener(this)
                    .build();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESOLVE_CONNECTION_REQUEST_CODE: {
                if (resultCode == RESULT_OK) {
                    // If user grants permission to access Google Drive, then attempt to re-connect
                    mGoogleApiClient.connect();
                }
                break;
            }
        }
    }

    /**
     * Uploads a copy of the current database to the user's Google Drive under the application's
     * appDataFolder
     */
    final private ResultCallbacks<DriveApi.DriveContentsResult> backupDriveContentsCallbacks =
            new ResultCallbacks<DriveApi.DriveContentsResult>() {
        @Override
        public void onSuccess(@NonNull DriveApi.DriveContentsResult result) {
            // Retrieve the DriveContents
            DriveContents contents = result.getDriveContents();

            if (contents != null) {
                // Obtain a reference to the database to be uploaded
                java.io.File dbFile = getDatabasePath(dbHelper.getDatabaseName());

                // Get a reference to the OutputStream to upload to GoogleDrive from the connection
                // to the DriveContents
                OutputStream outStream = contents.getOutputStream();;
                try {
                    // Create InputStream from the database File
                    InputStream inStream = new FileInputStream(dbFile);

                    // Init a buffer to read/write data
                    byte[] buffer = new byte[4096];
                    int c;

                    // Iterate through the File and write all the data to the OutputStream in
                    // chunks using the buffer
                    while ((c = inStream.read(buffer, 0, buffer.length)) > 0) {
                        outStream.write(buffer, 0, c);
                        outStream.flush();
                    }

                    // Close the input/output Streams
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Init the Metadata to be used for the DriveFile
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(dbHelper.getDatabaseName())
                    .setMimeType("application/x-sqlite3")
                    .build();

            // Obtain a reference to the Drive appDataFolder
            DriveFolder folder = Drive.DriveApi.getAppFolder(mGoogleApiClient);

            // Create the DriveFile within the DriveFolder with the Metadata
            folder.createFile(mGoogleApiClient, changeSet, contents).setResultCallback(fileCallback);
        }

        @Override
        public void onFailure(@NonNull Status status) {
            Toast.makeText(mContext, getString(R.string.toast_drive_failed), Toast.LENGTH_LONG).show();
        }
    };

    final private ResultCallbacks<DriveApi.DriveContentsResult> backupImageDriveCallback =
            new ResultCallbacks<DriveApi.DriveContentsResult>() {
        @Override
        public void onSuccess(@NonNull DriveApi.DriveContentsResult driveContentsResult) {
            DriveContents contents = driveContentsResult.getDriveContents();

            // Obtain a reference to the database to be uploaded
            java.io.File imageFile = imageFiles.get(0);

            if (contents != null) {
                // Get a reference to the OutputStream to upload to GoogleDrive from the connection
                // to the DriveContents
                OutputStream outStream = contents.getOutputStream();;
                try {
                    // Create InputStream from the database File
                    InputStream inStream = new FileInputStream(imageFile);

                    // Init a buffer to read/write data
                    byte[] buffer = new byte[4096];
                    int c;

                    // Iterate through the File and write all the data to the OutputStream in
                    // chunks using the buffer
                    while ((c = inStream.read(buffer, 0, buffer.length)) > 0) {
                        outStream.write(buffer, 0, c);
                        outStream.flush();
                    }

                    // Close the input/output Streams
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            // Init the Metadata to be used for the DriveFile
            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(imageFile.getName())
                    .setMimeType("image/jpeg")
                    .build();

            // Obtain a reference to the Drive appDataFolder
            DriveFolder folder = Drive.DriveApi.getAppFolder(mGoogleApiClient);

            // Create the DriveFile within the DriveFolder with the Metadata
            folder.createFile(mGoogleApiClient, changeSet, contents).setResultCallback(imageFileCallback);
        }

        @Override
        public void onFailure(@NonNull Status status) {

        }
    };

    /**
     * Queries the user's Google Drive appDataFolder for the presence of a database file, and then
     * depending on the procedure (mProcedure), either deletes the DriveFile or downloads and
     * restores it
     */
    final private ResultCallbacks<DriveApi.MetadataBufferResult> queryMetadataCallbacks =
            new ResultCallbacks<DriveApi.MetadataBufferResult>() {
        @Override
        public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
            // Obtain the Metadata of the Files in the DriveFolder
            MetadataBuffer metadatas = metadataBufferResult.getMetadataBuffer();

            // Check what procedure is to be done
            switch (mProcedure) {
                case BACKUP: {
                    // Init a List to hold the DriveIds of all the DriveFolders that are a database
                    List<DriveId> driveIdList = new ArrayList<>();

                    // Iterate through and add all DriveFiles to the List that match the database
                    // name
                    for (Metadata data : metadatas) {
                        driveIdList.add(data.getDriveId());
                    }

                    // Check to see if there were any DriveFiles to delete
                    if (driveIdList.size() > 0) {
                        // Create an Array from the List
                        DriveId[] driveIds = new DriveId[driveIdList.size()];
                        driveIdList.toArray(driveIds);

                        // Pass the List to the DeletePreviousBackup AsyncTask to delete the DriveFiles
                        DeletePreviousBackup asyncTask = new DeletePreviousBackup();
                        asyncTask.execute(driveIds);
                    } else {
                        // If there is nothing to delete, proceed directly to uploading the database
                        backupToDrive();
                    }

                    break;
                }
                case RESTORE: {
                    // Obtain a reference to the DriveId of the matching DriveFile
//                    dbDriveId = metadatas.get(0).getDriveId();
                    List<DriveId> driveIdList = new ArrayList<>();

                    // Init the List to store the file names of the items to be restored
                    fileTitleStrings = new ArrayList<>();

                    for (Metadata metadata : metadatas) {
                        // Add the DriveId and the file name to the appropriate Lists
                        driveIdList.add(metadata.getDriveId());
                        fileTitleStrings.add(metadata.getTitle());
                    }

                    // Check to make sure a matching DriveFile was identified
                    if (driveIdList.size() > 0) {
                        // Init and execute the AsyncTask for downloading and restore the database
                        DriveId[] driveIds = new DriveId[driveIdList.size()];
                        driveIdList.toArray(driveIds);

                        RestoreAsyncTask asyncTask = new RestoreAsyncTask(mContext);
                        asyncTask.execute(driveIds);
                    } else {
                        Log.d(LOG_TAG, "No database file found on Drive");
                        Toast.makeText(mContext, getString(R.string.toast_drive_no_backup), Toast.LENGTH_LONG).show();
                    }

                    break;
                }
            }

            // Release the resource to prevent memory leaks
            metadatas.release();
        }

        @Override
        public void onFailure(@NonNull Status status) {
            Toast.makeText(mContext, getString(R.string.toast_drive_failed), Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Checks to ensure that the database File was successfully uploaded to Google Drive
     */
    final private ResultCallbacks<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallbacks<DriveFolder.DriveFileResult>() {
        @Override
        public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult) {
            // Get the DriveFile
            DriveFile driveFile = driveFileResult.getDriveFile();

            // Check to see if there are image files to backup
            if (imageFiles.size() > 0) {
                // If there are image files, then start backing them up to Google Drive
                mGoogleApiClient.unregisterConnectionCallbacks(backupCallbacks);
                mGoogleApiClient.registerConnectionCallbacks(imageBackupCallbacks);
                mGoogleApiClient.connect();
            } else if (driveFile != null) {
                Log.d(LOG_TAG, "Success! DriveId: " + driveFile.getDriveId());
                Toast.makeText(mContext, getString(R.string.toast_backup_drive_success), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(@NonNull Status status) {
            Log.d(LOG_TAG, "Failed to transfer database to Google Drive.");
            Toast.makeText(mContext, getString(R.string.toast_drive_failed), Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Checks to ensure that the database File was successfully uploaded to Google Drive
     */
    final private ResultCallbacks<DriveFolder.DriveFileResult> imageFileCallback = new
            ResultCallbacks<DriveFolder.DriveFileResult>() {
        @Override
        public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult) {
            // Remove the backed up image File from the List of images to back up
            imageFiles.remove(0);

            // If there are remaining image Files to back up, then back them up
            if (imageFiles.size() > 0) {
                mGoogleApiClient.unregisterConnectionCallbacks(backupCallbacks);
                mGoogleApiClient.registerConnectionCallbacks(imageBackupCallbacks);
                mGoogleApiClient.connect();
            } else {
                Toast.makeText(mContext, getString(R.string.toast_backup_drive_success), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onFailure(@NonNull Status status) {
            Log.d(LOG_TAG, "Failed to transfer database to Google Drive.");
            Toast.makeText(mContext, getString(R.string.toast_drive_failed), Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Callback for backing up the user's database File to Google Drive if successfully connected
     */
    private GoogleApiClient.ConnectionCallbacks backupCallbacks =
            new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(backupDriveContentsCallbacks);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    /**
     * Callback for backing up the user's database File to Google Drive if successfully connected
     */
    private GoogleApiClient.ConnectionCallbacks imageBackupCallbacks =
            new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Drive.DriveApi.newDriveContents(mGoogleApiClient).setResultCallback(backupImageDriveCallback);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    /**
     * Callback for querying the contents of the user's appDataFolder on Google Drive if
     * successfully connected
     */
    private GoogleApiClient.ConnectionCallbacks queryCallbacks =
            new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            // Create a Query and filter only for database files
            Query query = new Query.Builder().build();

            // Obtain a reference to the appDataFolder on Google Drive and only query its contents
            // for the File
            DriveFolder appDataFolder = Drive.DriveApi.getAppFolder(mGoogleApiClient);
            appDataFolder.queryChildren(mGoogleApiClient, query)
                    .setResultCallback(queryMetadataCallbacks);
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity, RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0).show();
        }
    }

    /**
     * Restores the database File by downloading it from the user's Google Drive appDataFolder
     */
    private class RestoreAsyncTask extends AsyncTask<DriveId, Void, Boolean> {
        // Member Variables
        Context mContext;

        public RestoreAsyncTask(Context context) {
            // Init mem vars
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(DriveId... driveIds) {
            // Creates the image directory for the app if it does not exist
            IMAGE_DIRECTORY.mkdir();

            // Iterate through the DriveFiles and restore them to the local storage
            for (int i = 0; i < driveIds.length; i++) {
                // Retrieve the name of the file to be downloaded from Google Drive
                String fileTitle = fileTitleStrings.get(i);

                // Retrieve the DriveContent
                DriveFile driveFile = driveIds[i].asDriveFile();
                DriveApi.DriveContentsResult driveContentsResult = driveFile.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();

                if (!driveContentsResult.getStatus().isSuccess()) {
                    // If unable to connect, then do nothing
                    return false;
                }

                DriveContents driveContents = driveContentsResult.getDriveContents();

                // Init the File to save to
                File file;

                // Check to see if the file to be restored is the database or image file
                if (fileTitle.equals(dbHelper.getDatabaseName())) {
                    file = getDatabasePath(dbHelper.getDatabaseName());
                } else {
                    file = new File(IMAGE_DIRECTORY, fileTitleStrings.get(i));
                }

                // Download and save the file to local storage
                saveToFile(driveContents, file);
            }

            return true;
        }

        private boolean saveToFile(DriveContents driveContents, File file) {
            // Init the inputStream from the DriveContents
            InputStream inStream = driveContents.getInputStream();
            try {
                // Init the outputStream to the database File
                OutputStream outStream = new FileOutputStream(file);

                try {
                    // Init the buffer for read/writing the database File
                    byte[] buffer = new byte[4096];
                    int c;

                    // Write the new database File in chunks using the buffer
                    while ((c = inStream.read(buffer, 0, buffer.length)) > 0) {
                        outStream.write(buffer);
                        outStream.flush();
                    }

                    // Close the inputStream
                    inStream.close();
                } finally {
                    // Close the outputStream
                    outStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            // Close the Connection to the DriveContents
            driveContents.discard(mGoogleApiClient);

            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool) {
                Toast.makeText(mContext, getString(R.string.toast_restore_drive_success), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(mContext, getString(R.string.toast_drive_failed), Toast.LENGTH_LONG).show();
                Log.d(LOG_TAG, "Error restoring database from Google Drive!");
            }
        }
    }

    /**
     * Deletes DriveFiles from Google Drive
     */
    private class DeletePreviousBackup extends AsyncTask<DriveId, Void, Boolean> {
        @Override
        protected Boolean doInBackground(DriveId... driveIds) {
            // Iterate through each input parameter DriveId and delete the corresponding DriveContent
            for (DriveId driveid : driveIds) {
                DriveFile file = driveid.asDriveFile();

                // Delete the DriveFile
                com.google.android.gms.common.api.Status deleteStatus =
                        file.delete(mGoogleApiClient).await();

                if (!deleteStatus.isSuccess()) {
                    // If failed to delete a DriveFile, do not proceed
                    Log.d(LOG_TAG, "Error deleting " + driveid);
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool) {
                // If all database copies on the Drive were successfully removed, create a backup of
                // the database on Google Drive
                backupToDrive();
            } else {
                Log.d(LOG_TAG, "Error cleaning up previous database files");
            }
        }
    }
}
