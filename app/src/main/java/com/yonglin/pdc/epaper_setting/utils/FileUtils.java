package com.yonglin.pdc.epaper_setting.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.core.util.Pair;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.yonglin.pdc.epaper_setting.R;
import com.yonglin.pdc.epaper_setting.data.Constants;
import com.yonglin.pdc.epaper_setting.data.DeviceData;
import com.yonglin.pdc.epaper_setting.data.DeviceProfile;
import com.yonglin.pdc.epaper_setting.data.DeviceStatus;
import com.yonglin.pdc.epaper_setting.data.GlobalData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String PROFILE_FILENAME = "profile_v1"; // use file name for simple version control and migration

    private static String getFolder(Context context, UUID profileId) {
        final String destFolder = context.getFilesDir().getPath();
        // for debugging one can change the output folder here.
        // final String destFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        if(profileId == null) {
            profileId = UUID.fromString(Constants.ONE_TIME_PROFILE_ID);
            Log.w(TAG, "Empty UUID for cert file saving, using default ID.");
        }

        return destFolder + File.separator + profileId.toString();
    }

    /**
     * return display name and size from an uri.
     *
     * @param context context
     * @param uri     uri
     * @return a Pair class with display name and size
     */
    public static Pair<String, Long> getUriMetadata(Context context, Uri uri) {
        String displayName = null;
        long fileSize = 0;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                String size = null;
                if (!cursor.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    size = cursor.getString(sizeIndex);
                }
                Log.i(TAG, String.format("File: %s, size: %s", displayName, size));
                if (size != null) {
                    fileSize = Long.parseLong(size);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return new Pair<String, Long>(displayName, fileSize);
    }


    public static byte[] loadExternalFile(Context context, UUID profileID, String filename) {
        final String destFile = getFolder(context, profileID) + File.separator + filename;
        try {
            return Files.toByteArray(new File(destFile));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save an uri as an internal file.
     *
     * @param context context
     * @param uri     uri
     * @return returns errors if any, null if everything is ok.
     */
    public static String saveExternalFile(Context context, UUID profileID, String filename, Uri uri) {
        final String destFile = getFolder(context, profileID) + File.separator + filename;

        Pair<String, Long> data = getUriMetadata(context, uri);

        if (data.second > Constants.MAX_FILE_SIZE) {
            return context.getString(R.string.wifi_eap_file_max_exceeded);
        }

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            byte[] buffer = ByteStreams.toByteArray(inputStream);
            inputStream.close();

            Log.d(TAG, "Loaded bytes: " + buffer.length);

            File targetFile = new File(destFile);
            Files.createParentDirs(targetFile);
            Files.write(buffer, targetFile);
        } catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }
        return null;
    }

    public static void deleteProfileFolder(Context context, UUID profileID) {
        final String destFolder = getFolder(context, profileID);
        Log.d(TAG, "deleteProfileFolder: " + destFolder);
        File folder = new File(destFolder);
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                file.delete();
            }
        }
        folder.delete();
    }

    public static void saveProfiles(Context context, HashMap<String, DeviceProfile> map) {
        try {

            Gson gson = new GsonBuilder().create();

            String jsonString = gson.toJson(map, new TypeToken<HashMap<String, DeviceProfile>>() {
            }.getType());
            FileOutputStream fos = context.openFileOutput(PROFILE_FILENAME, Context.MODE_PRIVATE);
            fos.write(jsonString.getBytes());
            fos.close();

            // sync cache
            GlobalData.getInstance().getSavedProfileMap().clear();
            GlobalData.getInstance().getSavedProfileMap().putAll(map);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "profile file is missing.");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, DeviceProfile> loadProfiles(Context context) {
        File file = context.getFileStreamPath(PROFILE_FILENAME);

        HashMap<String, DeviceProfile> outputList = new HashMap<String, DeviceProfile>();

        if (file.exists()) {
            try {
                FileInputStream fin = context.openFileInput(PROFILE_FILENAME);
                byte[] buffer = new byte[fin.available()];
                fin.read(buffer);
                fin.close();
                String jsonString = new String(buffer);

                Gson gson = new Gson();
                HashMap<String, DeviceProfile> map = gson.fromJson(jsonString, new TypeToken<HashMap<String, DeviceProfile>>() {
                }.getType());
                outputList.putAll(map);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "profile file does not exist.");
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return outputList;
    }
}
