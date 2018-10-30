package com.android.car.media;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.harman.psa.widget.gridlist.model.GridItemDataProvider;


public class LibraryCategory implements GridItemDataProvider {

    private static String TAG = "LibraryCategory model";

    private String mText;
    private Drawable mIcon;
    private boolean mIsEnabled = true;

    public LibraryCategory(@NonNull String label, @NonNull Drawable icon) {
        mText = label;
        mIcon = icon;
    }

    @Override
    @NonNull
    public String getPrimaryText() {
        return mText;
    }

    ;

    @Override
    @Nullable
    public String getSecondaryText() {
        return null; // Icon item - no use
    }

    ;

    @Override
    @NonNull
    public Drawable getImageCover() {
        return mIcon;
    }

    ;

    @Override
    public void setImageCover(@NonNull Drawable imageCover) {
        mIcon = imageCover;
    }

    ;

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    ;

    @Override
    public boolean isActive() {
        Log.w(TAG, "Categories don't have active states -- return false every time");
        return false; // categories don't have active state
    }

    ;

    @Override
    public void setEnabledState(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    ;

    @Override
    public void setActiveState(boolean isActive) {
        Log.w(TAG, "Categories don't have active states -- ignore");
        return;
    }

    ;
}