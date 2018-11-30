package com.android.car.media;


import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

import com.android.car.apps.common.util.Assert;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.button.OnCycleChangeListener;
import com.harman.psa.widget.button.PSACyclicButton;
import com.harman.psa.widget.button.PSAIconButton;
import com.harman.psa.widget.toast.PSAToast;
import com.harman.psa.widget.verticallist.PsaRecyclerView;

import java.util.List;

public class MediaPlaylistFragment extends PSABaseFragment
        implements MediaPlaylistViewAdapter.OnItemClickListener {
    private static final String TAG = "PSAMediaPlaylistFragment";
    private static final int WRITE_PERMISSION_REQUEST_CODE = 10;

    private PsaRecyclerView mRecyclerView;
    private MediaPlaylistViewAdapter mAdapter;
    private MediaPlaybackModel mMediaPlaybackModel;

    private PSACyclicButton mRepeatButton;
    private PSACyclicButton mShuffleButton;
    private PSAIconButton mPlaylistSaveButton;

    private int mShuffleState = -1;
    private int mRepeatState = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaPlaybackModel.addListener(mModelListener);

        mShuffleState = mMediaPlaybackModel.getShuffleState();
        mRepeatState = mMediaPlaybackModel.getRepeatState();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_playlist_fragment, container, false);
        getAppBarView().removeAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_3);

        mRepeatButton = v.findViewById(R.id.playlist_repeat);
        mShuffleButton = v.findViewById(R.id.playlist_shuffle);
        mPlaylistSaveButton = v.findViewById(R.id.playlist_save);
        mRepeatButton.setImages(new int[]{R.drawable.psa_media_button_icon_repeat_off,
                R.drawable.psa_media_button_icon_repeat_on,
                R.drawable.psa_media_button_icon_repeat_one});

        mShuffleButton.setImages(new int[]{R.drawable.psa_media_button_icon_shuffle_on,
                R.drawable.psa_media_button_icon_shuffle_on});


        mShuffleButton.setListener(mShuffleButtonClickListener);
        mRepeatButton.setListener(mRepeatButtonClickListener);
        mPlaylistSaveButton.setClickListener(mPlaylistSaveButtonClickListener);

        if (mShuffleState == -1) {
            mShuffleButton.setEnabled(false);
        } else {
            mShuffleButton.setEnabled(true);
            mShuffleButton.setPosition(mShuffleState);
        }

        if (mRepeatState == -1) {
            mRepeatButton.setEnabled(false);
        } else {
            mRepeatButton.setEnabled(true);
            mRepeatButton.setPosition(mRepeatState);
        }

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getAppBarView().hideRightBar();

        mRecyclerView = view.findViewById(R.id.list);

        mAdapter = new MediaPlaylistViewAdapter();

        List<MediaSession.QueueItem> data = updatePlaylistData(null);

        mAdapter.setContext(getHostActivity());
        mAdapter.setItemClickListener(this);

        mAdapter.setItemsData(data, mMediaPlaybackModel.getActiveQueueItemId());

        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlaybackModel.removeListener(mModelListener);
        mMediaPlaybackModel = null;
    }

    private List<MediaSession.QueueItem> updatePlaylistData(List<MediaSession.QueueItem> currentQueue) {
        if (currentQueue == null) {
            currentQueue = mMediaPlaybackModel.getQueue();
        }
        return currentQueue;

    }


    private final MediaPlaybackModel.Listener mModelListener =
            new MediaPlaybackModel.AbstractListener() {

                @Override
                public void onMediaConnected() {
                    Assert.isMainThread();
                    onQueueChanged(mMediaPlaybackModel.getQueue());
                    onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
                }

                @Override
                public void onQueueChanged(List<MediaSession.QueueItem> queue) {
                    Assert.isMainThread();
                    mAdapter.setItemsData(queue, mMediaPlaybackModel.getActiveQueueItemId());
                    mAdapter.notifyDataSetChanged();
                }

                @Override
                @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
                public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                    mAdapter.setActiveQueueId(mMediaPlaybackModel.getActiveQueueItemId());
                }
            };


    private final OnCycleChangeListener mShuffleButtonClickListener = new OnCycleChangeListener() {
        @Override
        public void onChanged(int position) {
            mShuffleState = position;
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.psa_general_major_color1, typedValue, true);
            final int colorAccent = typedValue.data;

            getActivity().getTheme().resolveAttribute(R.attr.psa_general_content_color1, typedValue, true);
            final int colorContent = typedValue.data;


            switch (position) {
                case 0:
                    mShuffleButton.setColorFilter(colorContent, PorterDuff.Mode.SRC_ATOP);
                    break;
                case 1:
                    mShuffleButton.setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
                    break;
            }
            mMediaPlaybackModel.setShuffleState(mShuffleState);
        }
    };

    private final OnCycleChangeListener mRepeatButtonClickListener = new OnCycleChangeListener() {
        @Override
        public void onChanged(int position) {
            mRepeatState = position;
            TypedValue typedValue = new TypedValue();
            getActivity().getTheme().resolveAttribute(R.attr.psa_general_major_color1, typedValue, true);
            final int colorAccent = typedValue.data;

            getActivity().getTheme().resolveAttribute(R.attr.psa_general_content_color1, typedValue, true);
            final int colorContent = typedValue.data;
            switch (position) {
                case 0:
                    mRepeatButton.setColorFilter(colorContent, PorterDuff.Mode.SRC_ATOP);
                    break;
                case 1:
                    mRepeatButton.setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
                    break;
                case 2:
                    mRepeatButton.setColorFilter(colorAccent, PorterDuff.Mode.SRC_ATOP);
                    break;
            }
            mMediaPlaybackModel.setRepeatState(mRepeatState);
        }
    };

    private final View.OnClickListener mPlaylistSaveButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (Utils.hasRequiredPermissions(getContext())) {
                saveCurrentTracklist();
            } else {
                requestPermissions(Utils.PERMISSIONS, WRITE_PERMISSION_REQUEST_CODE);
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == WRITE_PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Required permissions for saving playlist didn't grant.");
                    return;
                }
            }
            saveCurrentTracklist();
        }
    }

    private void saveCurrentTracklist() {
        if (mMediaPlaybackModel.saveCurrentTracklist()) {
            PSAToast.makeText(getContext(),
                    R.string.playlist_saved,
                    PSAToast.LENGTH_SHORT)
                    .show();
        } else {
            Log.d(TAG, "Playlist didn't save.");
        }
    }

    public void onQueueItemClick(MediaSession.QueueItem item) {
        MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
        if (controls != null) {
            controls.skipToQueueItem(item.getQueueId());
        }
    }

}
