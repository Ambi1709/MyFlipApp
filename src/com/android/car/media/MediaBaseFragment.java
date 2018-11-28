package com.android.car.media;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.car.usb.PSAUsbStateService;
import com.android.car.usb.UsbDevice;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownItem;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.BIND_AUTO_CREATE;

public abstract class MediaBaseFragment extends PSABaseFragment {

    private static final String MEDIA_SOURCE_PREFERENCE = "mediaSourcePreference";

    private static final String SOURCE_ID = "sourceId";

    private static final String NO_SOURCE_ID = "";

    private boolean mIsServiceBound;

    private PSAUsbStateService mUsbNotificationService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            PSAUsbStateService.UsbStateServiceBinder binder = (PSAUsbStateService.UsbStateServiceBinder) service;
            mUsbNotificationService = binder.getService();
            onUsbServiceReady(mUsbNotificationService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Intent intent = new Intent(getContext(), PSAUsbStateService.class);
        mIsServiceBound = getContext().bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mIsServiceBound) {
            mIsServiceBound = false;
            if (mUsbNotificationService != null) {
                mUsbNotificationService.setUsbDeviceStateListener(null);
            }
            getContext().unbindService(mConnection);
        }
    }

    abstract void onUsbServiceReady(PSAUsbStateService usbNotificationService);

    protected void saveSourceId(String sourceId) {
        getContext()
                .getSharedPreferences(MEDIA_SOURCE_PREFERENCE, Context.MODE_PRIVATE)
                .edit()
                .putString(SOURCE_ID, sourceId)
                .commit();
    }

    public String getSourceId() {
        return getContext()
                .getSharedPreferences(MEDIA_SOURCE_PREFERENCE, Context.MODE_PRIVATE)
                .getString(SOURCE_ID, NO_SOURCE_ID);
    }
}
