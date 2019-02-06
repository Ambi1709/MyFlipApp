package com.android.car.media;

import com.android.car.eventbus.EventBus;

public class MessageEvent extends EventBus.Event {
    private int mLeftMargin;
    private int mDestWidth;
    private int mDuration;

    public MessageEvent(int leftMargin, int destinationWidth, int duration) {
        mLeftMargin = leftMargin;
        mDestWidth = destinationWidth;
        mDuration = duration;
    }

    public int getLeftMargin() {
        return mLeftMargin;
    }

    public int getDestinationWidth() {
        return mDestWidth;
    }

    public int getDuration() {
        return mDuration;
    }
}
