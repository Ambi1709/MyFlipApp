package com.android.car;

import android.app.Application;
import android.content.Intent;
import com.android.car.usb.PSAUsbStateService;

public class PSACarMediaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, PSAUsbStateService.class));
    }
}
