package com.android.car.media.widget;

import java.util.ArrayList;
import java.util.List;

public class CarMediaWidgetData {

    private List<String> mUrlList = new ArrayList<>();
    private int mCurrentPosition;

    public List<String> getUrlList() {
        return mUrlList;
    }

    public void setUrlList(List<String> list) {
        mUrlList = list;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }
}
