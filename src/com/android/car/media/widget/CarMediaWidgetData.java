package com.android.car.media.widget;

import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

public class CarMediaWidgetData {

    private List<Uri> mUrlList = new ArrayList<>();
    private int mCurrentPosition;

    public List<Uri> getUrlList() {
        return mUrlList;
    }

    public void setUrlList(List<Uri> list) {
        mUrlList = list;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public void setCurrentPosition(int mCurrentPosition) {
        this.mCurrentPosition = mCurrentPosition;
    }
}
