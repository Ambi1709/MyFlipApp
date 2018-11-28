package com.android.car.usb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.IBinder;
import android.os.storage.*;
import com.android.car.media.R;
import android.os.Binder;
import com.android.car.media.MediaActivity;

import java.util.*;
import java.util.stream.Collectors;


public class PSAUsbStateService extends Service {

    private static int sLastUsbDeviceId = 5;

    private final static String NOTIFICATION_CHANNEL_ID = "psaNotificationChannelId";

    private IBinder mBinder = new UsbStateServiceBinder();

    private UsbDeviceStateListener mUsbDeviceStateListener;

    private Map<String, UsbDevice> mUsbDevices = new HashMap<>();

    private UsbDevice mToBeScanned;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onScanFinished();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final StorageManager storageManager = getSystemService(StorageManager.class);
        if (storageManager != null) {
            for (VolumeInfo volumeInfo : storageManager.getVolumes()) {
                if (volumeInfo.isMountedReadable() && volumeInfo.disk != null && volumeInfo.disk.isUsb()) {
                    addVolumeInfo(volumeInfo);
                }
            }
            storageManager.registerListener(new StorageEventListener() {
                @Override
                public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
                    if (newState != oldState) {
                        PSAUsbStateService.this.handleUsbState(vol);
                    }
                }

                @Override
                public void onVolumeRecordChanged(VolumeRecord rec) {
                    VolumeInfo vol = storageManager.findVolumeById(rec.fsUuid);
                    if (vol != null ) {
                        PSAUsbStateService.this.handleUsbState(vol);
                    }
                }

                @Override
                public void onStorageStateChanged(String path, String oldState, String newState) {
                    super.onStorageStateChanged(path, oldState, newState);
                }
            });
        }

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void onScanFinished() {
        if (mToBeScanned != null) {
            mToBeScanned.setScanned(true);
            notify(mToBeScanned);
            mToBeScanned = null;
        }
    }

    private void handleUsbState(VolumeInfo vol) {
        if (vol.disk != null && vol.disk.isUsb()) {
            UsbDevice usbDevice;
            if (vol.isMountedWritable()) {
                usbDevice = addVolumeInfo(vol);
            } else {
                usbDevice = removeVolumeInfo(vol);
            }
            notify(usbDevice);
        }
    }

    private UsbDevice addVolumeInfo(VolumeInfo vol) {
        UsbDevice usbDevice = mUsbDevices.get(vol.disk.id);
        UsbVolume usbVolume = new UsbVolume(vol.id, vol.path, vol.isMountedReadable(), vol.getDescription());
        if (usbDevice == null) {
            usbDevice = new UsbDevice(++sLastUsbDeviceId, vol.disk.id, vol.disk.label, vol.disk.getDescription(),
                    usbVolume);
            mToBeScanned = usbDevice;
            mUsbDevices.put(usbDevice.getDeviceId(), usbDevice);
        } else {
            usbDevice.setName(vol.disk.label);
            usbDevice.setDescr(vol.disk.getDescription());
            usbDevice.addVolume(usbVolume);
        }
        return usbDevice;
    }

    private UsbDevice removeVolumeInfo(VolumeInfo vol) {
        UsbDevice usbDevice = mUsbDevices.get(vol.disk.id);
        if (usbDevice != null) {
            usbDevice.removeVolume(vol.id);
            if (usbDevice.getVolumes().isEmpty()) {
                mUsbDevices.remove(usbDevice.getDeviceId());
            }
        }
        return usbDevice;
    }

    private void notify(UsbDevice usbDevice) {
        if (mUsbDeviceStateListener == null) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null && usbDevice != null) {
                if (usbDevice.isReady()) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;
                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "chanelName",
                            importance);
                    channel.enableLights(true);
                    channel.setLightColor(Color.RED);
                    nm.createNotificationChannel(channel);
                    Notification notify = new Notification.Builder(getApplicationContext(), channel.getId())
                            .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                            .setContentTitle(getString(R.string.psa_general_notification_title))
                            .setContentText(getString(R.string.psa_general_notification_content, usbDevice.getName()))
                            .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                            .setContentIntent(getPendingIntent()).build();
                    nm.notify(usbDevice.getId(), notify);
                } else {
                    nm.cancel(usbDevice.getId());
                }
            }
        } else {
            mUsbDeviceStateListener.onUsbDeviceStateChanged();
        }
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MediaActivity.class);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    public void setUsbDeviceStateListener(UsbDeviceStateListener listener) {
        mUsbDeviceStateListener = listener;
    }

    public List<UsbDevice> getUsbDevices() {
        return mUsbDevices.values().stream()
                .filter(UsbDevice::isReady)
                .collect(Collectors.toList());
    }

    public UsbDevice getUsbDeviceById(int id) {
        for (UsbDevice usbDevice : getUsbDevices()) {
            if (usbDevice.getId() == id) {
                return usbDevice;
            }
        }
        return null;
    }

    public UsbDevice getUsbDeviceByDeviceId(String deviceId) {
        return mUsbDevices.values().stream()
                .filter(usbDevice -> usbDevice.getDeviceId().equals(deviceId))
                .findFirst()
                .orElse(null);
    }

    public class UsbStateServiceBinder extends Binder {
        public PSAUsbStateService getService() {
            return PSAUsbStateService.this;
        }
    }

    public interface UsbDeviceStateListener {
        void onUsbDeviceStateChanged();
    }

}
