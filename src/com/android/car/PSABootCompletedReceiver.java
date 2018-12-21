package com.android.car;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.car.usb.PSAUsbStateService;

public class PSABootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            context.startService(new Intent(context, PSAUsbStateService.class));
        }
    }
}
