package com.datpug.presentation.menu;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
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
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        mediaPlayer.stop();
        NotificationController.setReminder(this.getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer = null;
    }

    public void goToHowToPlay() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new HowToPlayFragment()).addToBackStack(null).commit();
    }

    public void goToSetting() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new SettingFragment()).addToBackStack(null).commit();
    }
}
