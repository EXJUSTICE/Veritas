package com.xu.servicequalityrater;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Works with tutorialDialogWindow to provide the introductory navigation
 */

public class TutorialAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;

    public TutorialAdapter(FragmentManager fm, List<Fragment> fragments){
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        switch(position){
            case 0: return FirstFragment.newInstance("FirstFragment, Instance 1");
            case 1: return SecondFragment.newInstance("SecondFragment, Instance 1");
            case 2: return ThirdFragment.newInstance("ThirdFragment, Instance 1");
            default: return FirstFragment.newInstance("FirstFragment, Default");
        }

    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }
}
