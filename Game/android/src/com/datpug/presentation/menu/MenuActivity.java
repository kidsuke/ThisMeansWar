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
    private Button playBtn;
    private Button quitBtn;
    private TextView appNameStart;
    private TextView appNameMid;
    private TextView appNameEnd;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        appNameStart = findViewById(R.id.appNameStart);
        appNameMid = findViewById(R.id.appNameMid);
        appNameEnd = findViewById(R.id.appNameEnd);
        Typeface appNameTypeface = Typeface.createFromAsset(getAssets(), "fonts/Sketch_3D.otf");
        appNameStart.setTypeface(appNameTypeface);
        appNameMid.setTypeface(appNameTypeface);
        appNameEnd.setTypeface(appNameTypeface);

        performAppNameAnimation();

        Typeface btnTypeface = Typeface.createFromAsset(getAssets(), "fonts/LuckyClover.ttf");
        playBtn = findViewById(R.id.playBtn);
        quitBtn = findViewById(R.id.quitBtn);
        playBtn.setTypeface(btnTypeface);
        quitBtn.setTypeface(btnTypeface);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            performButtonAnimation(playBtn);
            startActivity(new Intent(MenuActivity.this, GameLauncher.class));
            }
        });

        quitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performButtonAnimation(quitBtn);
                finish();
            }
        });
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

    // Bounce effect when menu button is clicked
    public void performButtonAnimation(Button button) {
        final Animation bouncing = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // Use bounce interpolator with amplitude 0.2 and frequency 20
        BounceInterpolator interpolator = new BounceInterpolator(0.2, 20);
        bouncing.setInterpolator(interpolator);

        button.startAnimation(bouncing);
    }

    // App name animation
    public void performAppNameAnimation() {
        final Animation slide_left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        final Animation slide_right = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slide_left.setStartOffset(500);
        slide_right.setStartOffset(1000);
        appNameStart.startAnimation(slide_left);
        appNameEnd.startAnimation(slide_left);
        appNameMid.startAnimation(slide_right);
    }
}
