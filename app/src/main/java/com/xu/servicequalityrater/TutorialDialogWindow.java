package com.xu.servicequalityrater;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.List;

/**
 *http://stackoverflow.com/questions/19544829/viewpager-with-fragments-inside-popupwindow-or-dialogfragment-error-no-view
 1305 WORKS, tablayout added http://stackoverflow.com/questions/20586619/android-viewpager-with-bottom-dots
TODO implement the check
 */

public class TutorialDialogWindow extends DialogFragment {
    ViewPager vp;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //TODO i dont think this is correct, the correct one should have a viewpager
       View view = inflater.inflate(R.layout.fragment_viewpager, container);

        vp= (ViewPager) view.findViewById(R.id.viewPager);
        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(vp, true);
        List fragments = getFragments();
        //TODO getChildFragmentManager is important, otherwise doesnt work
        TutorialAdapter ama = new TutorialAdapter(getChildFragmentManager(), fragments);
        vp.setAdapter(ama);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);


        return view;
    }

    private List getFragments(){

        List fList = new ArrayList();

        fList.add("First");
        fList.add("Second");
        fList.add("Third");

        return fList;
    }


}
