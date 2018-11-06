package com.android.car.usb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Debug;
import android.os.IBinder;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.StorageEventListener;
import android.content.Intent;
import com.android.car.media.R;
import android.os.Binder;
import android.os.IBinder;
import com.android.car.media.MediaActivity;


public class PSAUsbStateService extends Service {
    public static final String DISK_INFO = "diskInfo";

    private final static int NOTIFICATION_ID = 0;

    private final static String NOTIFICATION_CHANNEL_ID = "psaNotificationChannelId";

    private IBinder mBinder = new UsbStateServiceBinder();

    private OnUsbStateListener mOnUsbStateListener;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final StorageManager storageManager = getSystemService(StorageManager.class);
        if (storageManager != null) {
            storageManager.registerListener(new StorageEventListener() {
                @Override
                public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
                    if (newState != oldState) {
                        PSAUsbStateService.this.handleUsbState(vol);
                    }
                }

                @Override
                public void onVolumeRecordChanged(VolumeRecord rec) {
                    final VolumeInfo vol = storageManager.findVolumeByUuid(rec.getFsUuid());
                    if (vol != null) {
                        PSAUsbStateService.this.handleUsbState(vol);
                    }
                }
            });
        }
    }

    private void handleUsbState(VolumeInfo vol) {
        if (mOnUsbStateListener != null) {
            if (vol.isMountedReadable()) {
                mOnUsbStateListener.onUsbDiskMounted(vol.getDisk());
            } else {
                mOnUsbStateListener.onUsbDiskUnmounted(vol.getDisk());
            }
        } else {
            notify(vol);
        }
    }

    private void notify(VolumeInfo volumeInfo) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            if (volumeInfo.isMountedReadable()) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "chanelName",
                        importance);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                nm.createNotificationChannel(channel);
                Notification notify = new Notification.Builder(getApplicationContext(), channel.getId())
                        .setChannelId(NOTIFICATION_CHANNEL_ID).setStyle(new Notification.MediaStyle())
                        .setContentTitle(getString(R.string.psa_general_notification_title))
                        .setContentText(getString(R.string.psa_general_notification_content, volumeInfo.getDisk().getDescription()))
                        .setSmallIcon(R.drawable.psa_general_media_center_picto_style)
                        .setContentIntent(getPendingIntent(volumeInfo.getDisk())).build();
                nm.notify(NOTIFICATION_ID, notify);
            } else {
                nm.cancel(NOTIFICATION_ID);
            }
        }
    }

    private PendingIntent getPendingIntent(DiskInfo discInfo) {
        Intent intent = new Intent(this, MediaActivity.class);
        intent.putExtra(DISK_INFO, discInfo);
        return PendingIntent.getActivity(this, 0, intent, 0);
    }

    public void setOnUsbStateListener(OnUsbStateListener listener) {
        mOnUsbStateListener = listener;
    }

    public class UsbStateServiceBinder extends Binder {
        public PSAUsbStateService getService() {
            return PSAUsbStateService.this;
        }
    }

    public interface OnUsbStateListener {
        void onUsbDiskMounted(DiskInfo diskInfo);

        void onUsbDiskUnmounted(DiskInfo diskInfo);
    }

}
