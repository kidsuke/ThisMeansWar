package com.datpug;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

public class MenuActivity extends AppCompatActivity {
    private Button playBtn;
    private Button quitBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        TextView appName = findViewById(R.id.appName);
        Typeface appNameTypeface = Typeface.createFromAsset(getAssets(), "fonts/Sketch_3D.otf");
        appName.setTypeface(appNameTypeface);

        Typeface btnTypeface = Typeface.createFromAsset(getAssets(), "fonts/LuckyClover.ttf");
        playBtn = findViewById(R.id.playBtn);
        quitBtn = findViewById(R.id.quitBtn);
        playBtn.setTypeface(btnTypeface);
        quitBtn.setTypeface(btnTypeface);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performAnimation(playBtn);
            }
        });

        quitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performAnimation(quitBtn);
                finish();
            }
        });


    }

    // Bounce effect when button is clicked
    public void performAnimation(Button button) {
        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // Use bounce interpolator with amplitude 0.2 and frequency 20
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
        myAnim.setInterpolator(interpolator);

        button.startAnimation(myAnim);
    }
}
