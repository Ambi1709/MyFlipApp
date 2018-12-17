package com.android.car.usb;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Debug;
import android.provider.MediaStore;
import android.util.Log;
import java.util.List;

public class UsbReadyChecker extends AsyncTask<Void, Void, Void> {

    private ContentResolver mResolver;

    private List<UsbDevice> mUsbDevices;

    private UsbCheckListener mUsbCheckListener;

    public interface UsbCheckListener {
        void onUsbChecked();
    }

    public UsbReadyChecker(Context context, List<UsbDevice> usbDevices, UsbCheckListener usbCheckListener) {
        mResolver = context.getContentResolver();
        mUsbDevices = usbDevices;
        mUsbCheckListener = usbCheckListener;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        for (UsbDevice usbDevice : mUsbDevices) {
            String path = usbDevice.getVolumePaths()[0];
            String where = MediaStore.Audio.Media.IS_MUSIC + " != 0 "
                    + "and " + MediaStore.Audio.AudioColumns.DATA + " LIKE '" + path + "%'";
            try (Cursor cursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, where,
                    null, MediaStore.Audio.AudioColumns.TITLE + " LIMIT 1")) {
                if (cursor != null && cursor.moveToFirst()) {
                    usbDevice.setScanned(true);
                }
            } catch (SQLiteException e) {
                Log.e("UsbReadyChecker", "Failed to execute query: " + e);
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void nothing) {
        if (mUsbCheckListener != null) {
            mUsbCheckListener.onUsbChecked();
        }
    }
}
