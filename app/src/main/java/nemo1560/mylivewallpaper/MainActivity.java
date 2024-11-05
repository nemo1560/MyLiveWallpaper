package nemo1560.mylivewallpaper;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.Manifest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private Button btn;
    private List<String> fileList;
    private static final int REQUEST_CODE_PICK_FILE = 100;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        btn = findViewById(R.id.chooseFileButton);
        fileList = new ArrayList<>();
        btn.setOnClickListener(view -> onChooseFileButtonClick());
        RecyclerView.Adapter adapter = new FileListAdapter(fileList, new OnFileClickListener() {
            @Override
            public void onFileClick(String filePath) {
                startLiveWallpaper(filePath);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        checkPermission();
        if(Keys.TYPE == 2){
            startLiveWallpaper(null);
        }
    }

    public void onChooseFileButtonClick() {
        String imagePath = Environment.getExternalStorageDirectory().getAbsolutePath();  //"%2f" represents "/"
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,imagePath);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    public String getRealPathFromURI(final Context context, Uri contentUri) {
        // Check here to KITKAT or new version
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String selection = null;
        String[] selectionArgs = null;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, contentUri)) {
            // ExternalStorageProvider
            if (Keys.isExternalStorageDocument(contentUri)) {
                final String docId = DocumentsContract.getDocumentId(contentUri);
                final String[] split = docId.split(":");
                final String type = split[0];

                String fullPath = Keys.getPathFromExtSD(split);
                if (!fullPath.equals("")) {
                    return fullPath;
                } else {
                    return null;
                }
            }
            else if (Keys.isDownloadsDocument(contentUri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(contentUri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                    id = DocumentsContract.getDocumentId(contentUri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri _contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));

                                // final Uri contentUri = ContentUris.withAppendedId(
                                //        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                                return Keys.getDataColumn(context, _contentUri, null, null);
                            } catch (NumberFormatException e) {
                                // In Android 8 and Android P the id is not a number
                                return contentUri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }
                } else {
                    final String id = DocumentsContract.getDocumentId(contentUri);
                    final boolean isOreo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (contentUri != null) {
                        return Keys.getDataColumn(context, contentUri, null, null);
                    }
                }
            }
            // MediaProvider
            else if (Keys.isMediaDocument(contentUri)) {
                final String docId = DocumentsContract.getDocumentId(contentUri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri _contentUri = null;

                if ("image".equals(type)) {
                    _contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    _contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    _contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
                return Keys.getDataColumn(context, _contentUri, selection, selectionArgs);
            } else if (Keys.isGoogleDriveUri(contentUri)) {
                return Keys.getDriveFilePath(contentUri, context);
            }
        }
        return selection;
    }

    private void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // if android 11+ request MANAGER_EXTERNAL_STORAGE
            if (!Environment.isExternalStorageManager()) { // check if we already have permission
                Uri uri = Uri.parse(String.format(Locale.ENGLISH, "package:%s", getApplicationContext().getPackageName()));
                startActivity(
                        new Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                uri
                        )
                );
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { // check if we already have permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK) {
            Uri selectedFileUri = data.getData();
            String filePath = getRealPathFromURI(this.getBaseContext(),selectedFileUri);
            File _file = new File(filePath);
            double fileSizeInMB = _file.length() / 1024.0;
            if(filePath != null && _file.canRead() && fileSizeInMB < 60000){
                startLiveWallpaper(filePath);
            }else{
                Toast.makeText(getApplicationContext(), "File over size", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLiveWallpaper(String filePath) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        try {
            wallpaperManager.clear();
            Toast.makeText(getApplicationContext(), "Stop live wallpaper.", Toast.LENGTH_SHORT).show();
            SharedPreferences preferences = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("file_path", filePath);
            editor.apply();

            Intent intent = new Intent();
            intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, GIFWallpaperService.class));
            startActivity(intent);
            finish();
            Toast.makeText(getApplicationContext(), "Start new live wallpaper.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}