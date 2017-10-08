package com.datpug.presentation.menu;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.datpug.R;
import com.datpug.presentation.GameLauncher;

public class MenuFragment extends Fragment {
    private Button playBtn;
    private Button quitBtn;
    private ImageView settingBtn;
    private Button howToPlayBtn;
    private TextView appNameStart;
    private TextView appNameMid;
    private TextView appNameEnd;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        appNameStart = view.findViewById(R.id.appNameStart);
        appNameMid = view.findViewById(R.id.appNameMid);
        appNameEnd = view.findViewById(R.id.appNameEnd);
        Typeface appNameTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/Sketch-3D.otf");
        appNameStart.setTypeface(appNameTypeface);
        appNameMid.setTypeface(appNameTypeface);
        appNameEnd.setTypeface(appNameTypeface);

        performAppNameAnimation();

        Typeface btnTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/KBLuckyClover.ttf");
        playBtn = view.findViewById(R.id.playBtn);
        quitBtn = view.findViewById(R.id.quitBtn);
        howToPlayBtn = view.findViewById(R.id.howToPlayBtn);
        playBtn.setTypeface(btnTypeface);
        quitBtn.setTypeface(btnTypeface);
        howToPlayBtn.setTypeface(btnTypeface);

        settingBtn = view.findViewById(R.id.setting_btn);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performButtonAnimation(playBtn);
                startActivity(new Intent(getActivity(), GameLauncher.class));
            }
        });

        quitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performButtonAnimation(quitBtn);
                getActivity().finish();
            }
        });

        howToPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performButtonAnimation(howToPlayBtn);
                ((MenuActivity) getActivity()).goToHowToPlay();
            }
        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performButtonAnimation(settingBtn);
                ((MenuActivity) getActivity()).goToSetting();
            }
        });

        return view;
    }

    // Bounce effect when menu button is clicked
    public void performButtonAnimation(View button) {
        final Animation bouncing = AnimationUtils.loadAnimation(getContext(), R.anim.bounce);

        // Use bounce interpolator with amplitude 0.2 and frequency 20
        BounceInterpolator interpolator = new BounceInterpolator(0.2, 20);
        bouncing.setInterpolator(interpolator);

        button.startAnimation(bouncing);
    }

    // App name animation
    public void performAppNameAnimation() {
        final Animation slide_left = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_left);
        final Animation slide_right = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
        slide_left.setStartOffset(500);
        slide_right.setStartOffset(1000);
        appNameStart.startAnimation(slide_left);
        appNameEnd.startAnimation(slide_left);
        appNameMid.startAnimation(slide_right);
    }
}
