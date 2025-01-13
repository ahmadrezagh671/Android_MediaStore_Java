package com.ahmadrezagh671.mediastore_create_open_files;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TagForLogs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    public static String getAppName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            CharSequence appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(context.getPackageName(), 0)
            );
            return appName.toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "App Name Not Found";
        }
    }

    public void logger(String log){
        Log.i(TAG, log);
        //Toast.makeText(getApplicationContext(), log, Toast.LENGTH_SHORT).show();
        ((TextView) findViewById(R.id.textView)).setText(log);
    }

    public String getEditTextString(){
        String s = ((EditText) findViewById(R.id.editText)).getText().toString();
        if (s.isEmpty())
            s = "example_file_" + System.currentTimeMillis() + ".txt";
        return s;
    }

    public void createFileInDownloads(Context context) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, getEditTextString());
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + getAppName(this));

        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                // Write text to the file
                String fileContent = "Hello, this is the content of the file!\n Created at " + System.currentTimeMillis() + " SystemTime";
                outputStream.write(fileContent.getBytes());
                outputStream.close();
            }
            Uri realPath = getRealPathFromUri(context, uri);
            logger("File created at: " + realPath);
        } else {
            logger("Failed to create file.");
        }
    }

    public String readFileFromDownloads(Context context, String fileName) {

        // Query MediaStore for the file
        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME};
        String selection = MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String[] selectionArgs = {fileName};

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                // Get the file Uri
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                long id = cursor.getLong(idColumn);
                Uri fileUri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, String.valueOf(id));

                // Open the file and read its content
                try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
                     BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

                    StringBuilder content = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }

                    return content.toString().trim(); // Return the file content
                }
            } else {
                return "File not found.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading file: " + e.getMessage();
        }
    }

    public Uri getRealPathFromUri(Context context, Uri uri) {
        String realPath = null;

        // Query the MediaStore
        Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{MediaStore.MediaColumns.DATA}, // Column to retrieve
                null,
                null,
                null
        );

        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            if (cursor.moveToFirst()) {
                realPath = cursor.getString(columnIndex);
            }
            cursor.close();
        }

        return Uri.parse(realPath);
    }

    public void CreateTestFile(View view) {
        try {
            createFileInDownloads(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void OpenFile(View view) {
        String text = readFileFromDownloads(getApplicationContext(),getEditTextString());
        logger(text);
    }
}