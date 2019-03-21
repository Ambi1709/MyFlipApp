package com.android.car.media.widget;


import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.media.Utils;
import com.android.car.media.MediaActivity;

import com.harman.psa.widget.PSABaseFragment;

import com.android.car.media.R;

import java.util.List;


public class WidgetRadioFragment extends PSABaseFragment {

    private static final String TAG = "WidgetMediaPlayerFragment";


    private ImageView mSwitchToMediaButton;
    private ImageView mPlayPauseButton;
    private ImageView mMuteButton;
    private TextView mTitle;


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_radio_widget, container, false);


        mSwitchToMediaButton = (ImageView) v.findViewById(R.id.switch_to_media_button);
        mPlayPauseButton = (ImageView) v.findViewById(R.id.play_pause_button);
        mMuteButton = (ImageView) v.findViewById(R.id.mute_button);

        mTitle = (TextView) v.findViewById(R.id.title);
        return v;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlayPauseButton.setOnClickListener(mControlsClickListener);
        mMuteButton.setOnClickListener(mControlsClickListener);
        mSwitchToMediaButton.setOnClickListener(mControlsClickListener);
    }

    private final View.OnClickListener mControlsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.play_pause_button:
                    //
                    break;
                case R.id.mute_button:
                    //
                    break;
                case R.id.switch_to_media_button:
                    ((MediaActivity) getHostActivity()).switchApp();
                    break;
                default:
                    throw new IllegalStateException("Unknown button press: " + v);
            }
        }
    };

}