package com.android.car.media;


import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


/* Root categories */
public class LibraryCategoryGridItemData {

    private static String TAG = "LibraryCategoryGridItemData";

    private String mItemId;
    private Bundle mExtras;
    private String mPrimaryText = "";
    private String mSecondaryText = "";
    private Uri mIconUri;
    private boolean mIsEnabled = true;
    private boolean mIsActive = false;

    public LibraryCategoryGridItemData(@NonNull String id, @NonNull String label, @Nullable Bundle extras) {
        mItemId = id;
        mPrimaryText = label;
        mExtras = extras;
    }

    @NonNull
    public String getItemId() {
        return mItemId;
    }

    public Bundle getExtras() {
        return mExtras;
    }

    @NonNull
    public String getPrimaryText() {
        return mPrimaryText;
    }

    @Nullable
    public String getSecondaryText() {
        return mSecondaryText;
    }

    @Nullable
    public Uri getImageUri() {
        return mIconUri;
    }

    public void setImageUri(@Nullable Uri imageUri) {
        mIconUri = imageUri;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    public boolean isActive() {
        return mIsActive;
    }

    public void setEnabledState(boolean isEnabled) {
        mIsEnabled = isEnabled;
    }

    public void setActiveState(boolean isActive) {
        mIsActive = isActive;
    }


}