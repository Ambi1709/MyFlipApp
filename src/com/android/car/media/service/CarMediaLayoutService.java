package com.android.car.media.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.android.car.PSACarMediaApp;
import com.android.car.eventbus.EventBus;
import com.android.car.media.MessageEvent;

public class CarMediaLayoutService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final ICarMediaAnimationController.Stub mBinder = new ICarMediaAnimationController.Stub() {
        public void startAnimation(int leftMargin, int destinationWidth, int duration) {
            EventBus.getDefault().post(new MessageEvent(leftMargin, destinationWidth, duration));
        }
    };
}
