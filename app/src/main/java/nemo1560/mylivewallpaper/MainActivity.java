package nemo1560.mylivewallpaper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
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
import android.view.View;
import android.widget.Button;
import android.Manifest;
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
        String imagePath = String.valueOf(Uri.parse(Environment.DIRECTORY_DCIM));  //"%2f" represents "/"
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI,imagePath);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String filePath;
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(contentUri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } else {
            filePath = contentUri.getPath();
        }

        return filePath;
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
            String filePath = getRealPathFromURI(selectedFileUri);
            startLiveWallpaper(filePath);
        }
    }

    private void startLiveWallpaper(String filePath) {
        SharedPreferences preferences = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("file_path", filePath);
        editor.apply();

        Intent intent = new Intent();
        intent.setAction(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(this, GIFWallpaperService.class));
        startActivity(intent);
        finish();
    }
}