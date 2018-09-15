package com.xu.servicequalityrater;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

/**
 * Created by Omistaja on 13/05/2017.
 */

public class ThirdFragment extends Fragment {
    SharedPreferences sp;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment3, container, false);

        Button shown= (Button)v.findViewById(R.id.buttonOK);
        shown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor =sp.edit();
                editor.putBoolean("welcomeShown", true);
                editor.commit();
                //Launch the PowerSaveSettings
                if(Build.VERSION.SDK_INT>=23){
                    Intent batterySaverIntent=new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(batterySaverIntent);
                }else{
                    Intent batterySaverIntent=new Intent(android.provider.Settings.ACTION_SETTINGS);
                    startActivity(batterySaverIntent);
                }

            }
        });






        return v;
    }
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

    }


    public static ThirdFragment newInstance(String text) {

        ThirdFragment f = new ThirdFragment();
        Bundle b = new Bundle();
        b.putString("msg", text);

        f.setArguments(b);

        return f;
    }
}
