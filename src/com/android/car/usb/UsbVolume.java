package com.android.car.usb;

import android.os.Parcelable;

public class UsbVolume {

    private String mId;

    private String mPath;

    private boolean mIsMounted;

    private String mDescr;

    public UsbVolume(String id, String path, boolean isMounted, String descr) {
        mId = id;
        mPath = path;
        mIsMounted = isMounted;
        mDescr = descr;
    }

    public String getId() {
        return mId;
    }

    public String getPath() {
        return mPath;
    }

    public boolean isMounted() {
        return mIsMounted;
    }

    public void setMounted(boolean mounted) {
        mIsMounted = mounted;
    }

    public String getDescr() {
        return mDescr;
    }
}
