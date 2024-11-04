package nemo1560.mylivewallpaper;

import static android.content.Intent.getIntent;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class GIFWallpaperService extends WallpaperService {
    protected static int playheadTime = 0;
    private SurfaceHolder mSurfaceHolder;

    @Override
    public Engine onCreateEngine() {
        SharedPreferences preferences = getSharedPreferences("WallpaperPrefs", MODE_PRIVATE);
        String filePath = preferences.getString("file_path", null);
        try {
            if(Keys.TYPE == 2){
                AssetManager assetManager = getApplicationContext().getAssets();
                String videoFilePath = "1.mp4";
                VideoEngine videoEngine = new VideoEngine(assetManager, videoFilePath);
                mSurfaceHolder = videoEngine.getSurfaceHolder();
                return videoEngine;
            } else {
                VideoEngine videoEngine = new VideoEngine(null, filePath);
                mSurfaceHolder = videoEngine.getSurfaceHolder();
                return videoEngine;
            }
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDestroy() {
        stopSelf();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Canvas canvas = null;
            boolean canvasLocked = false;

            while (!canvasLocked) {
                try {
                    canvas = mSurfaceHolder.lockCanvas();
                    if (canvas != null) {
                        canvasLocked = true;
                    }
                } catch (Exception e) {
                    // Handle exception if needed
                }
            }
            if (canvas != null) {
                // Clear the canvas by drawing a white rectangle
                Paint paint = new Paint();
                paint.setColor(Color.BLACK);
                canvas.drawRect(100, 0, canvas.getWidth(), canvas.getHeight(), paint);
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        } else {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && "STOP_WALLPAPER_SERVICE".equals(intent.getAction())){
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    class VideoEngine extends Engine {
        private final String TAG = getClass().getSimpleName();
        private MediaPlayer mediaPlayer;
        private AssetManager assetManager;
        private String videoFilePath;

        public VideoEngine(AssetManager assetManager, String videoFilePath) throws IOException {
            super();
            Log.i(TAG, "( VideoEngine )");
            this.assetManager = assetManager;
            this.videoFilePath = videoFilePath;
            mediaPlayer = new MediaPlayer();
            try {
                if(Keys.TYPE == 2){
                    AssetFileDescriptor afd = this.assetManager.openFd(this.videoFilePath);
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    mediaPlayer.setLooping(true);
                }else{
                    File file = new File(videoFilePath); // Assuming the video file is named video.mp4 in the internal storage
                    FileInputStream fis = new FileInputStream(file);
                    AssetFileDescriptor afd = new AssetFileDescriptor(ParcelFileDescriptor.dup(fis.getFD()), 0, file.length());
                    mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                    mediaPlayer.setLooping(true);
                }
            }catch (Exception e){
                Log.e("IOException", e.toString());
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            Log.i(TAG, "onSurfaceCreated");
            mediaPlayer.setSurface(holder.getSurface());
            try {
                mediaPlayer.prepare();
                mediaPlayer.setVolume(0,0);
                mediaPlayer.start();
            } catch (IOException e) {
                Log.e("IOException", e.toString());
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.i(TAG, "onSurfaceDestroyed");
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }
}



