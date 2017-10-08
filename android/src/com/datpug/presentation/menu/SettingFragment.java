package com.datpug.presentation.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.datpug.R;

import java.util.ArrayList;
import java.util.List;

public class SettingFragment extends Fragment {

    private boolean firstInvocation = true;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);

        // SETUP VIEWS
        final TextView instruction = view.findViewById(R.id.instruction);
        final LinearLayout macAddInput = view.findViewById(R.id.macAddInput);
        final CheckBox checkBox = view.findViewById(R.id.checkbox);
        final Button saveBtn = view.findViewById(R.id.save_btn);
        EditText macAdd1 = view.findViewById(R.id.macAdd1);
        EditText macAdd2 = view.findViewById(R.id.macAdd2);
        EditText macAdd3 = view.findViewById(R.id.macAdd3);
        EditText macAdd4 = view.findViewById(R.id.macAdd4);
        EditText macAdd5 = view.findViewById(R.id.macAdd5);
        EditText macAdd6 = view.findViewById(R.id.macAdd6);
        final List<EditText> editTexts = new ArrayList<>();
        editTexts.add(macAdd1);
        editTexts.add(macAdd2);
        editTexts.add(macAdd3);
        editTexts.add(macAdd4);
        editTexts.add(macAdd5);
        editTexts.add(macAdd6);

        Typeface inputTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/AmaticSC-Bold.ttf");
        instruction.setTypeface(inputTypeface);
        checkBox.setTypeface(inputTypeface);
        Typeface buttonTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/KBLuckyClover.ttf");
        saveBtn.setTypeface(buttonTypeface);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isCheck) {
                if (!firstInvocation) saveBtn.setVisibility(View.VISIBLE);
                if(!isCheck) {
                    instruction.setVisibility(View.INVISIBLE);
                    macAddInput.setVisibility(View.INVISIBLE);
                } else {
                    instruction.setVisibility(View.VISIBLE);
                    macAddInput.setVisibility(View.VISIBLE);
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Build mac address
                StringBuilder macAddressBuilder = new StringBuilder();
                for (EditText macAdd: editTexts) {
                    if (macAddressBuilder.toString().isEmpty()) {
                        macAddressBuilder.append(macAdd.getText().toString());
                    } else {
                        macAddressBuilder.append(":").append(macAdd.getText().toString());
                    }
                }
                // Save settings to pref
                SharedPreferences sharePref = getActivity().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharePref.edit();
                editor.putString("macAddress", macAddressBuilder.toString());
                editor.putBoolean("remoteControl", checkBox.isChecked());
                editor.apply();

                saveBtn.setVisibility(View.GONE);
                Toast.makeText(getContext(), R.string.setting_saved, Toast.LENGTH_SHORT).show();
            }
        });

        for (final EditText macAdd: editTexts) {
            macAdd.setTypeface(inputTypeface);
            macAdd.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (!firstInvocation) saveBtn.setVisibility(View.VISIBLE);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if (editable.toString().length() == 2) {
                        View view = macAdd.focusSearch(View.FOCUS_RIGHT);
                        if (view != null) view.requestFocus();
                    }
                }
            });
        }

        // Get previous settings from pref
        SharedPreferences sharePref = getActivity().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE);
        String macAddress = sharePref.getString("macAddress", "C5:71:71:99:C4:D4");
        Boolean enableRemoteControl = sharePref.getBoolean("remoteControl", false);

        checkBox.setChecked(enableRemoteControl);
        String[] macAddComponents = macAddress.split(":");
        for (int i = 0; i < macAddComponents.length; i++) {
            editTexts.get(i).setText(macAddComponents[i]);
        }
        firstInvocation = false;

        return view;
    }
}
