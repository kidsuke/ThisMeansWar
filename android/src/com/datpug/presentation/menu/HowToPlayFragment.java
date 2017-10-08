package com.datpug.presentation.menu;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.datpug.R;

/**
 * Created by hongphuc on 10/7/17.
 */

public class HowToPlayFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_how_to_play, container, false);
        TextView step1 = view.findViewById(R.id.htp_step1);
        TextView step2 = view.findViewById(R.id.htp_step2);
        TextView step3 = view.findViewById(R.id.htp_step3);
        Typeface htpTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/AmaticSC-Bold.ttf");
        step1.setTypeface(htpTypeface);
        step2.setTypeface(htpTypeface);
        step3.setTypeface(htpTypeface);

        return view;
    }
}
