package com.datpug.presentation.menu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import com.datpug.R;
import com.datpug.notification.NotificationController;
import com.datpug.presentation.GameLauncher;

import java.io.IOException;

public class MenuActivity extends AppCompatActivity {
    public static final int PERMISSION_ALL = 100;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainer, new MenuFragment()).commit();

        // Play background music
        AssetFileDescriptor assetFileDescriptor;
        try {
            assetFileDescriptor = getAssets().openFd("menu_bg_music.mp3");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(), assetFileDescriptor.getStartOffset(), assetFileDescriptor.getLength());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        requestPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mediaPlayer.start();
        NotificationController.cancelReminder(this.getApplicationContext());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mediaPlayer.pause();
        NotificationController.setReminder(this.getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer = null;
    }

    public void goToHowToPlay() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new HowToPlayFragment()).addToBackStack(null).commit();
    }

    public void goToSetting() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new SettingFragment()).addToBackStack(null).commit();
    }

    public void requestPermissions() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_ALL);
        }
    }

    public boolean hasPermissions(String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
