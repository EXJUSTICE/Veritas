package com.xu.servicequalityrater;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import java.util.List;

/**
 * Created by Omistaja on 04/06/2017.
 */

public class ProfileDialogWindow extends DialogFragment {
    EditText profileedit;
    Button dismissal;
    String profiletext;
    EditText numberedit;
    String numbertext;
    SharedPreferences profileSave;
    SharedPreferences.Editor welcomeeditor;
    public EditProfileListener listener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //TODO i dont think this is correct, the correct one should have a viewpager
        View view = inflater.inflate(R.layout.fragment_profile, container);
        profileSave=getActivity().getSharedPreferences("profileSave", Context.MODE_PRIVATE);
        profileedit= (EditText)view.findViewById(R.id.profileEntry);
        numberedit= (EditText)view.findViewById(R.id.numberEntry);
        dismissal = (Button)view.findViewById(R.id.buttonAccept);
        dismissal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                profiletext = profileedit.getText().toString();
                numbertext=  numberedit.getText().toString();
                welcomeeditor = profileSave.edit();
                welcomeeditor.putString("profilename",profiletext);
                welcomeeditor.putString("profilenumber",numbertext);
                welcomeeditor.commit();
                listener.profileEdited();
                dismiss();
            }
        });


        return view;
    }

    public interface EditProfileListener
    {
        public void profileEdited ();
    }

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);

        try
        {
            this.listener = (EditProfileListener) activity;
        }
        catch ( ClassCastException oops )
        {
            oops.printStackTrace();
        }
    }
}
