package com.android.car.media.widget;


import android.annotation.TargetApi;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.apps.common.BitmapDownloader;
import com.android.car.apps.common.BitmapWorkerOptions;
import com.android.car.apps.common.util.Assert;

import com.android.car.media.Utils;
import com.android.car.media.MediaActivity;
import com.android.car.media.MediaPlaybackModel;
import com.android.car.media.MediaPlaybackModel.Listener;
import com.android.car.media.common.MediaAppSelectorWidget;
import com.android.car.media.common.AppSelectionFragment;
import com.android.car.media.widget.ui.CoverFlowPager;

import com.harman.psa.widget.PSABaseFragment;

import com.android.car.media.R;

import java.util.List;


public class WidgetMediaPlayerFragment extends PSABaseFragment implements MediaPlaybackModel.Listener, CoverFlowPager.CoverFlowSelectionListener {

    private static final String TAG = "WidgetMediaPlayerFragment";

    private MediaPlaybackModel mMediaPlaybackModel;

    private Handler mHandler = new Handler();
    private static final long DELAY_SHOW_NO_CONTENT_VIEW_MS = 3000;

    private CoverFlowPager mArtViewPager;

    private ImageView mSwitchToRadioButton;
    private ImageView mPlayPauseButton;
    private ImageView mNextButton;
    private TextView mTitle;
    private TextView mSubTitle;

    private BitmapDownloader mDownloader;
    private int mAlbumArtWidth = Utils.DEFAULT_ALBUM_ART_WIDTH;
    private int mAlbumArtHeight = Utils.DEFAULT_ALBUM_ART_HEIGHT;


    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_widget, container, false);

        MediaAppSelectorWidget appSelector = v.findViewById(R.id.app_switch_container);
        appSelector.setFragmentActivity(getHostActivity());

        mArtViewPager = (CoverFlowPager) v.findViewById(R.id.albumArtCoverFlow);

        mSwitchToRadioButton = (ImageView) v.findViewById(R.id.switch_to_radio_button);
        mPlayPauseButton = (ImageView) v.findViewById(R.id.play_pause_button);
        mNextButton = (ImageView) v.findViewById(R.id.next_button);

        mTitle = (TextView) v.findViewById(R.id.title);
        mSubTitle = (TextView) v.findViewById(R.id.subTitle);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaPlaybackModel.addListener(this);

        mArtViewPager.setCoverFlowSelectionListener(this);

        onMetadataChanged(mMediaPlaybackModel.getMetadata());
        onQueueChanged(mMediaPlaybackModel.getQueue());
        onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
    }
    @Override
    public void onDestroy() {
        if (mMediaPlaybackModel != null) {
            mMediaPlaybackModel.removeListener(this);
        }
        mMediaPlaybackModel = null;
        super.onDestroy();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPlayPauseButton.setOnClickListener(mControlsClickListener);
        mNextButton.setOnClickListener(mControlsClickListener);
        mSwitchToRadioButton.setOnClickListener(mControlsClickListener);
    }

    private final View.OnClickListener mControlsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaController.TransportControls transportControls =
                    mMediaPlaybackModel.getTransportControls();

            switch (v.getId()) {
                case R.id.play_pause_button:
                    if (!mMediaPlaybackModel.isConnected()) {
                        Log.e(TAG, "Unable to send action for " + v
                                + ". The MediaPlaybackModel is not connected.");
                        return;
                    }
                    handlePlaybackStateForPlay(mMediaPlaybackModel.getPlaybackState(),
                            transportControls);
                    break;
                case R.id.next_button:
                    if (!mMediaPlaybackModel.isConnected()) {
                        Log.e(TAG, "Unable to send action for " + v
                                + ". The MediaPlaybackModel is not connected.");
                        return;
                    }
                    transportControls.skipToNext();
                    break;
                case R.id.switch_to_radio_button:
                    ((MediaActivity) getHostActivity()).switchApp();
                    break;
                default:
                    throw new IllegalStateException("Unknown button press: " + v);
            }
        }
        /**
         * Plays, pauses or stops the music playback depending on the state given in
         * {@link PlaybackState}.
         */
        private void handlePlaybackStateForPlay(PlaybackState playbackState,
                                                MediaController.TransportControls transportControls) {
            if (playbackState == null) {
                return;
            }
            switch (playbackState.getState()) {
                // Only if the music is currently playing does this method need to handle pausing
                // and stopping of media.
                case PlaybackState.STATE_PLAYING:
                case PlaybackState.STATE_BUFFERING:
                    long actions = playbackState.getActions();
                    if ((actions & PlaybackState.ACTION_PAUSE) != 0) {
                        transportControls.pause();
                    } else if ((actions & PlaybackState.ACTION_STOP) != 0) {
                        transportControls.stop();
                    }
                    break;

                default:
                    transportControls.play();
            }
        }
    };

    @Override
    public void onMediaAppChanged(@Nullable ComponentName currentName,
                                  @Nullable ComponentName newName) {
        //
    }

    @Override
    public void onMediaAppStatusMessageChanged(@Nullable String message) {
    }

    @Override
    public void onMediaConnected() {
        Assert.isMainThread();
        onMetadataChanged(mMediaPlaybackModel.getMetadata());
        onQueueChanged(mMediaPlaybackModel.getQueue());
        onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
    }

    @Override
    public void onMediaConnectionSuspended() {
        //
    }

    @Override
    public void onMediaConnectionFailed(CharSequence failedClientName) {
        Assert.isMainThread();
        mHandler.removeCallbacks(mShowNoContentViewRunnable);
        mTitle.setText(failedClientName);
        mSubTitle.setText("Connection failed");
        mArtViewPager.clear();
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
        Assert.isMainThread();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onPlaybackStateChanged; state: "
                    + (state == null ? "<< NULL >>" : state.toString()));
        }

        if (state == null) {
            return;
        }
        mHandler.removeCallbacks(mShowNoContentViewRunnable);
        if (state.getState() == PlaybackState.STATE_ERROR) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "ERROR: " + state.getErrorMessage());
            }
            String message = TextUtils.isEmpty(state.getErrorMessage())
                    ? getString(R.string.unknown_error)
                    : state.getErrorMessage().toString();
            mTitle.setText("");
            mSubTitle.setText(message);
            mArtViewPager.clear();
            return;
        }

        mArtViewPager.setCurrentItem(mMediaPlaybackModel.getCurrentMediaPosition());

        int playbackState = state.getState();
        if (playbackState == PlaybackState.STATE_PLAYING){
            mPlayPauseButton.setImageResource(R.drawable.psa_media_button_icon_pause);
        } else{
            mPlayPauseButton.setImageResource(R.drawable.psa_media_button_icon_play);
        }

        mNextButton.setEnabled((state.getActions() & PlaybackState.ACTION_SKIP_TO_NEXT) != 0);
        
    }

    @Override
    public void onMetadataChanged(@Nullable MediaMetadata metadata) {
        Assert.isMainThread();
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onMetadataChanged; description: "
                    + (metadata == null ? "<< NULL >>" : metadata.getDescription().toString()));
        }
        if (metadata == null) {
            mHandler.postDelayed(mShowNoContentViewRunnable, DELAY_SHOW_NO_CONTENT_VIEW_MS);
            return;
        } else {
            mHandler.removeCallbacks(mShowNoContentViewRunnable);
        }

        Bitmap icon = Utils.getMetadataBitmap(metadata);
        Uri iconUri = Utils.getMetadataIconUri(metadata, getContext());

        mTitle.setText(metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        mSubTitle.setText(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                + " - "
                + metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
    }

    private final Runnable mShowNoContentViewRunnable = () -> {
        mTitle.setText("");
        mSubTitle.setText("");
        mArtViewPager.clear();
        mNextButton.setEnabled(false);
        mPlayPauseButton.setEnabled(false);
    };

    @Override
    public void onQueueChanged(List<MediaSession.QueueItem> queue) {
        Assert.isMainThread();
        mHandler.removeCallbacks(mShowNoContentViewRunnable);
        CarMediaWidgetData data = mMediaPlaybackModel.getCarMediaWidgetDataFromQueue(queue);
        mArtViewPager.refreshData(data);
        mPlayPauseButton.setEnabled(true);
    }

    @Override
    public void onSessionDestroyed(CharSequence destroyedMediaClientName) {
        Assert.isMainThread();
        mHandler.removeCallbacks(mShowNoContentViewRunnable);
        mTitle.setText("");
        mSubTitle.setText("");
        mArtViewPager.clear();
        mNextButton.setEnabled(false);
        mPlayPauseButton.setEnabled(false);
    }

    @Override
    public void onEdgeActionReceived(String action, Bundle extras) {
        //
    }

    public void selectNext(){
        MediaController.TransportControls transportControls =
                mMediaPlaybackModel.getTransportControls();

        if (!mMediaPlaybackModel.isConnected()) {
            Log.e(TAG, "Unable to send action. The MediaPlaybackModel is not connected.");
            return;
        }
        transportControls.skipToNext();
    }

    public void selectPrev(){
        MediaController.TransportControls transportControls =
                mMediaPlaybackModel.getTransportControls();

        if (!mMediaPlaybackModel.isConnected()) {
            Log.e(TAG, "Unable to send action. The MediaPlaybackModel is not connected.");
            return;
        }
        transportControls.skipToPrevious();
    }

}