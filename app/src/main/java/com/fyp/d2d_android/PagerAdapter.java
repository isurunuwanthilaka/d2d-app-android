package com.fyp.d2d_android;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                FileScreen tab1 = new FileScreen();
                return tab1;
            case 1:
                CloudFileScreen tab2 = new CloudFileScreen();
                return tab2;
            case 2:
                NetworkScreen tab3 = new NetworkScreen();
                return tab3;
            case 3:
                EditUserActivity tab4 = new EditUserActivity();
                return tab4;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}