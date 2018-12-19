package com.android.car.usb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.os.storage.*;
import com.android.car.media.R;
import com.android.car.media.MediaActivity;
import com.android.internal.messages.nano.SystemMessageProto;

import java.util.*;
import java.util.stream.Collectors;

import android.content.IntentFilter;

import static android.app.Notification.Builder;


public class PSAUsbStateService extends Service {

    private static final String TAG = PSAUsbStateService.class.getCanonicalName();

    private static final String SP_USB_DEVICE_IDS = "usbIdsSharedPrefs";

    private static final String SP_LAST_USB_DEVICE_ID = "lastUsbDeviceId";

    private static final String CHANNEL_NAME = "pasNotificationChanel";

    private static int sLastUsbDeviceId;

    public static final String USB_SOURCE_ID = "usbSourceId";

    private final static String NOTIFICATION_CHANNEL_ID = "psaNotificationChannelId";

    private IBinder mBinder = new UsbStateServiceBinder();

    private UsbDeviceStateListener mUsbDeviceStateListener;

    private Map<String, UsbDevice> mUsbDevices = new HashMap<>();

    private UsbDevice mWaitingScan;

    private UsbDevice mWaitingNotify;

    private BroadcastReceiver mUsbScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onScanFinished();
        }
    };

    private NotificationChannel mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH);

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sLastUsbDeviceId = getSharedPreferences(SP_USB_DEVICE_IDS, MODE_PRIVATE).getInt(SP_LAST_USB_DEVICE_ID, 1000);
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
            new UsbReadyChecker(this, new ArrayList<>(mUsbDevices.values()), () -> {
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
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
                intentFilter.addDataScheme("file");
                registerReceiver(mUsbScanReceiver, intentFilter);
            }).execute();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbScanReceiver);
    }

    private void handleDiskDestroyed(DiskInfo disk) {
        cancelNotify(SystemMessageProto.SystemMessage.NOTE_STORAGE_DISK);
        onRemoveUsbDevice(mUsbDevices.remove(disk.id));
    }

    private void onScanFinished() {
        if (mWaitingScan != null) {
            mWaitingScan.setScanned(true);
            if (mWaitingScan.isReady()) {
                onAddUsbDevice(mWaitingScan);
            }
            mWaitingScan = null;
        }
    }

    private void onScanFinished(DiskInfo disk, int volumeCount) {
        UsbDevice usbDevice = mUsbDevices.get(disk.id);
        if (usbDevice == null) {
            usbDevice = new UsbDevice(++sLastUsbDeviceId, disk.id, disk.label, disk.getDescription());
            mUsbDevices.put(usbDevice.getDeviceId(), usbDevice);
        }
        if (volumeCount == 0 && disk.size > 0) {
            notify(usbDevice.getId(), buildBadUsbNotification());
        } else {
            cancelNotify(SystemMessageProto.SystemMessage.NOTE_STORAGE_DISK);
        }
    }

    private void handleUsbState(VolumeInfo vol) {
        if (vol.disk != null && vol.disk.isUsb()) {
            UsbDevice usbDevice;
            if (vol.isMountedWritable()) {
                usbDevice = addVolumeInfo(vol);
                if (usbDevice.isReady()) {
                    onAddUsbDevice(usbDevice);
                }
            } else if (vol.state == VolumeInfo.STATE_UNMOUNTABLE) {
                usbDevice = addVolumeInfo(vol);
                notify(usbDevice.getId(), buildBadUsbNotification());
            } else {
                usbDevice = removeVolumeInfo(vol);
                onRemoveUsbDevice(usbDevice);
            }
        }
    }

    private UsbDevice addVolumeInfo(VolumeInfo vol) {
        UsbDevice usbDevice = mUsbDevices.get(vol.disk.id);
        UsbVolume usbVolume = new UsbVolume(vol.id, vol.path, vol.isMountedReadable(), vol.getDescription());
        if (usbDevice == null) {
            usbDevice = new UsbDevice(++sLastUsbDeviceId, vol.disk.id, vol.disk.label, vol.disk.getDescription(),
                    usbVolume);
            mWaitingScan = usbDevice;
            mUsbDevices.put(usbDevice.getDeviceId(), usbDevice);
            getSharedPreferences(SP_USB_DEVICE_IDS, MODE_PRIVATE)
                    .edit().putInt(SP_LAST_USB_DEVICE_ID, sLastUsbDeviceId).apply();
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

    private void onAddUsbDevice(UsbDevice usbDevice) {
        mWaitingNotify = usbDevice;
        if (mUsbDeviceStateListener == null) {
            if (usbDevice != null) {
                if (usbDevice.isReady()) {
                    notify(usbDevice.getId(), buildNotification());
                } else {
                    cancelNotify(usbDevice.getId());
                }
            }
        } else {
            mUsbDeviceStateListener.onUsbDeviceStateChanged();
        }
    }

    private void onRemoveUsbDevice(UsbDevice usbDevice) {
        if (usbDevice != null) {
            if (mUsbDeviceStateListener != null) {
                mUsbDeviceStateListener.onUsbDeviceRemoved(usbDevice);
            }
            cancelNotify(usbDevice.getId());
        }
    }

    private void notify(int id, Notification notification) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(id, notification);
        }
    }

    private void cancelNotify(int id) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(id);
        }
    }

    public void forceNotify() {
        if (mWaitingNotify != null && mWaitingNotify.isReady()) {
            notify(mWaitingNotify.getId(), buildNotification());
        }
    }

    private Notification buildBadUsbNotification() {
        return new Builder(getApplicationContext(), mChannel.getId())
                .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                .setContentTitle(getString(R.string.psa_general_notification_title_usb))
                .setContentText(getString(R.string.psa_general_notification_content_usb))
                .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                .build();
    }

    private Notification buildNotification() {
        return new Builder(getApplicationContext(), mChannel.getId())
                .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                .setContentTitle(getString(R.string.psa_general_notification_title))
                .setContentText(getString(R.string.psa_general_notification_content, mWaitingNotify.getName()))
                .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                .setContentIntent(getPendingIntent(mWaitingNotify.getDeviceId()))
                .setAutoCancel(true)
                .build();
    }

    private PendingIntent getPendingIntent(String usbSourceId) {
        Intent intent = new Intent(this, MediaActivity.class);
        Bundle extras = new Bundle();
        extras.putString(USB_SOURCE_ID, usbSourceId);
        intent.putExtras(extras);
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

        void onUsbDeviceRemoved(UsbDevice usbDevice);
    }
}
