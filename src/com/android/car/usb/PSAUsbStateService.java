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
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.storage.*;
import android.util.Log;
import com.android.car.media.R;
import android.os.Binder;
import com.android.car.media.MediaActivity;
import com.android.internal.messages.nano.SystemMessageProto;

import java.util.*;
import java.util.stream.Collectors;


public class PSAUsbStateService extends Service {

    private static final String TAG = PSAUsbStateService.class.getCanonicalName();

    private static final String CHANNEL_NAME = "pasNotificationChanel";

    private static int sLastUsbDeviceId = 5;

    public static final String USB_SOURCE_ID = "usbSourceId";

    private final static String NOTIFICATION_CHANNEL_ID = "psaNotificationChannelId";

    private IBinder mBinder = new UsbStateServiceBinder();

    private UsbDeviceStateListener mUsbDeviceStateListener;

    private Map<String, UsbDevice> mUsbDevices = new HashMap<>();

    private UsbDevice mToBeScanned;

    private UsbDevice mUsbDevice;

    private NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH);

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
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.RED);
        nm.createNotificationChannel(mChannel);
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
                    if (vol != null) {
                        handleUsbState(vol);
                    }
                }

                @Override
                public void onDiskScanned(DiskInfo disk, int volumeCount) {
                    onScanFinished(disk, volumeCount);
                }

                @Override
                public void onDiskDestroyed(DiskInfo disk) {
                    handleDiskDestroyed(disk);
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

    private void handleDiskDestroyed(DiskInfo disk) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancelAsUser(disk.getId(), SystemMessageProto.SystemMessage.NOTE_STORAGE_DISK,
                    UserHandle.ALL);
        }
    }

    private void onScanFinished(DiskInfo disk, int volumeCount) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            if (volumeCount == 0 && disk.size > 0) {
                Notification notify = new Notification.Builder(getApplicationContext(), mChannel.getId())
                        .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                        .setContentTitle(getString(R.string.psa_general_notification_title_usb))
                        .setContentText(getString(R.string.psa_general_notification_content_usb))
                        .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                        .setAutoCancel(true)
                        .build();
                nm.notifyAsUser(disk.getId(), SystemMessageProto.SystemMessage.NOTE_STORAGE_DISK,
                        notify, UserHandle.ALL);
            } else {
                nm.cancelAsUser(disk.getId(), SystemMessageProto.SystemMessage.NOTE_STORAGE_DISK,
                        UserHandle.ALL);
            }
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
        mUsbDevice = usbDevice;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mUsbDeviceStateListener == null) {
            if (nm != null && usbDevice != null) {
                if (usbDevice.isReady()) {
                    nm.notify(usbDevice.getId(), buildNotification());
                } else {
                    nm.cancel(usbDevice.getId());
                }
            }
        } else {
            if (nm != null && usbDevice != null && !usbDevice.isReady()) {
                nm.cancel(usbDevice.getId());
            }
            mUsbDeviceStateListener.onUsbDeviceStateChanged();
        }
    }

    public void forceNotify() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && mUsbDevice != null && mUsbDevice.isReady()) {
            nm.notify(mUsbDevice.getId(), buildNotification());
        }
    }

    private Notification buildNotification() {
        return new Notification.Builder(getApplicationContext(), mChannel.getId())
                .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                .setContentTitle(getString(R.string.psa_general_notification_title))
                .setContentText(getString(R.string.psa_general_notification_content, mUsbDevice.getName()))
                .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                .setContentIntent(getPendingIntent(mUsbDevice.getDeviceId()))
                .build();
    }

    private PendingIntent getPendingIntent(String usbSourceId) {
        Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(USB_SOURCE_ID, usbSourceId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
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
