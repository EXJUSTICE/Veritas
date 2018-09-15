package com.xu.servicequalityrater;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;


/**
 * Allows user to modify the minimum detection limiter
 */
public class SettingsFragment extends DialogFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    RadioButton button10min;
    RadioButton button30min;
    RadioButton button1hour;
    RadioButton button4hour;
    RadioButton button12hour;
    RadioButton button24hour;
    Button OK;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor edit;
    int type;


    private OnFragmentInteractionListener mListener;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     */
    // TODO: Rename and change types and number of parameters
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getActivity().getSharedPreferences("Settings",Context.MODE_PRIVATE);
        edit = sharedPreferences.edit();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        button10min = (RadioButton) v.findViewById(R.id.button10minutes);
        button30min = (RadioButton) v.findViewById(R.id.button30minutes);
        button1hour = (RadioButton) v.findViewById(R.id.button60minutes);
        button4hour = (RadioButton) v.findViewById(R.id.button4hours);
        button12hour =(RadioButton) v.findViewById(R.id.button12hours);
        button24hour =(RadioButton) v.findViewById(R.id.button24hours);
        OK = (Button)v.findViewById(R.id.buttonOK);

        //Load existing selection from sharedpreferences

        loadExistingPreference();





        OK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (button10min.isChecked()) {
                    type = 0;

                } else if (button30min.isChecked()) {
                    type = 1;

                } else if (button1hour.isChecked()) {
                    type = 2;
                } else if(button4hour.isChecked()) {
                    type = 3;
                }else if(button12hour.isChecked()) {
                    type = 4;
                }else if(button24hour.isChecked()) {
                    type = 5;
                }

                edit.putInt("type",type);
                edit.putString("save","SAVED SUCCESS");
                edit.commit();


                //TODO if we save in defaultSharedPreferences do we still need listener
                //mListener.onFragmentInteraction(type);
                dismiss();

        }
        });

        return v;
    }

    public void loadExistingPreference(){
        type = sharedPreferences.getInt("type",0);
        if (type ==0){
            button10min.setChecked(true);
        }else if(type==1){
            button30min.setChecked(true);
        }else if (type==2){
            button1hour.setChecked(true);
        }else if (type==3){
            button4hour.setChecked(true);
        }else if (type==4){
            button12hour.setChecked(true);
        }else if (type==5){
            button24hour.setChecked(true);
        }

    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(int length);
    }
}
