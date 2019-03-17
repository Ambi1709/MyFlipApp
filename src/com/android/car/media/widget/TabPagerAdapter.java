package com.android.car.media.widget;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.car.media.R;

public class TabPagerAdapter extends FragmentPagerAdapter {

    private static final int PAGE_COUNT = 3;
    private Context mContext;

    public TabPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        SubFragment.Type type = SubFragment.Type.SONG;
        switch (position) {
            case 0:
                type = SubFragment.Type.ALBUM;
                break;
            case 1:
                type = SubFragment.Type.ARTIST;
                break;
        }
        return SubFragment.newInstance(type);
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        String title = "";
        switch (position) {
            case 0:
                title = mContext.getString(R.string.album);
                break;
            case 1:
                title = mContext.getString(R.string.artist);
                break;
            case 2:
                title = mContext.getString(R.string.song);
                break;
        }
        return title;
    }
}
