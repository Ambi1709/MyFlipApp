package com.android.car.media;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.MediaDescription;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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

import java.io.ByteArrayOutputStream;
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

    private int mShuffleState = MediaPlaybackModel.SHUFFLE_UNDEFINED_STATE;
    private int mRepeatState = MediaPlaybackModel.REPEAT_UNDEFINED_STATE;

    private boolean mIsEditMode;
    private Handler mEdgeHandler = new Handler();
    private int mEdgePosition = MediaConstants.UNDEFINED_EDGE_POSITION;
    private MediaNavigationManager mNavigationManager;

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaPlaybackModel.addListener(mModelListener);
        mNavigationManager = ((MediaActivity) getHostActivity()).getNavigationManagerImpl();

        mShuffleState = mMediaPlaybackModel.getShuffleState();
        mRepeatState = mMediaPlaybackModel.getRepeatState();
        updateShuffleButtonState();
        updateRepeatButtonState();

        List<MediaSession.QueueItem> data = updatePlaylistData(null);

        mAdapter.setContext(getHostActivity());
        mAdapter.setItemClickListener(this);

        mAdapter.setItemsData(data, mMediaPlaybackModel.getActiveQueueItemId());

        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);
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

        updateShuffleButtonState();
        updateRepeatButtonState();

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getAppBarView().hideRightBar();

        mRecyclerView = view.findViewById(R.id.list);

        mAdapter = new MediaPlaylistViewAdapter();
    }

    @Override
    public void onStart() {
        super.onStart();
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter("com.harman.edge.EDGE"));
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBroadcastReceiver != null) {
            getContext().unregisterReceiver(mBroadcastReceiver);
        }
        mEdgeHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlaybackModel.removeListener(mModelListener);
        mMediaPlaybackModel = null;
    }

    private void updateShuffleButtonState() {
        if (mShuffleState == MediaPlaybackModel.SHUFFLE_UNDEFINED_STATE) {
            mShuffleButton.setEnabled(false);
        } else {
            mShuffleButton.setEnabled(true);
            mShuffleButton.setPosition(mShuffleState);
        }
    }

    private void updateRepeatButtonState() {
        if (mRepeatState == MediaPlaybackModel.REPEAT_UNDEFINED_STATE) {
            mRepeatButton.setEnabled(false);
        } else {
            mRepeatButton.setEnabled(true);
            mRepeatButton.setPosition(mRepeatState);
        }
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
        if (mIsEditMode) {
            mEdgeHandler.removeCallbacksAndMessages(null);
            sendEditModeAction(MediaConstants.EDGE_ACTION_PLAY_ITEM, mEdgePosition, "com.harman.psa.magic_touch_action_play_item",
                    "com.harman.psa.magic_touch_action_play_item_image", item);
            disableEditMode();
        } else {
            MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
            if (controls != null) {
                controls.skipToQueueItem(item.getQueueId());
            }
        }
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra(MediaConstants.EDGE_SHORTCUT_POSITION, MediaConstants.UNDEFINED_EDGE_POSITION);
            String action = intent.getStringExtra(MediaConstants.EDGE_SHORTCUT_ACTION);
            String appKey = intent.getStringExtra(MediaConstants.APP_KEY);
            if (TextUtils.isEmpty(action) || MediaConstants.MAGIC_TOUCH_APP_KEY.equals(appKey)) {
                showEditMode(position);
            }
        }
    };


    private void showEditMode(final int position) {
        mEdgeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                disableEditMode();
            }
        }, MediaConstants.EDIT_MODE_TIMEOUT);

        mEdgePosition = position;

        mIsEditMode = true;

        mNavigationManager.setTabBarEnabled(false);
        ((MediaActivity) getHostActivity()).setEnabledAppBarButtons(false);
        mShuffleButton.setEnabled(false);
        mRepeatButton.setEnabled(false);

        mAdapter.setEditModeEnabled(true);

    }

    private void disableEditMode() {
        mIsEditMode = false;
        mAdapter.setEditModeEnabled(false);
        mEdgePosition = MediaConstants.UNDEFINED_EDGE_POSITION;

        mNavigationManager.setTabBarEnabled(true);
        ((MediaActivity) getHostActivity()).setEnabledAppBarButtons(true);
        mShuffleButton.setEnabled(true);
        mRepeatButton.setEnabled(true);

        getActivity().sendBroadcast(new Intent(MediaConstants.BROADCAST_MAGIC_TOUCH_EDIT_MODE));
    }

    private void sendEditModeAction(String action, int position, String titleMetaName, String iconMetaName, MediaSession.QueueItem item) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(MediaConstants.EDGE_SERVICE_PACKAGE, MediaConstants.EDGE_SERVICE_CLASS));
        Bundle data = new Bundle();
        Intent playIntent = new Intent();
        Bundle playIntentExtras = new Bundle();
        if (position == MediaConstants.UNDEFINED_EDGE_POSITION) {
            data.putString(MediaConstants.APP_KEY, MediaConstants.MAGIC_TOUCH_APP_KEY);
            playIntentExtras.putString(MediaConstants.APP_KEY, MediaConstants.MAGIC_TOUCH_APP_KEY);
        } else {
            data.putString(MediaConstants.APP_KEY, MediaConstants.EDGE_APP_KEY);
            data.putInt(MediaConstants.EDGE_SHORTCUT_POSITION, position);
            playIntentExtras.putString(MediaConstants.APP_KEY, MediaConstants.EDGE_APP_KEY);
        }

        playIntent.setComponent(new ComponentName("com.android.car.media", "com.android.car.media.MediaActivity"));

        if (MediaConstants.EDGE_ACTION_PLAY_ITEM.equals(action)) {
            MediaDescription currentItemDescription = item.getDescription();
            if (currentItemDescription != null && currentItemDescription.getExtras() != null) {
                playIntentExtras = new Bundle(currentItemDescription.getExtras());
                playIntentExtras.putString(MediaConstants.MEDIA_ID_EXTRA_KEY, currentItemDescription.getMediaId());
                data.putString(MediaConstants.CONTACT, currentItemDescription.getTitle().toString());
                Bitmap bitmap = Utils.getBitmapIcon(getContext(), currentItemDescription.getIconUri());
                if (bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    data.putByteArray(MediaConstants.BLOB_DATA, byteArray);
                }
            }
        } else {
            data.putString(MediaConstants.CONTACT, "");
        }

        playIntentExtras.putInt(MediaConstants.EDGE_SHORTCUT_POSITION, position);
        playIntentExtras.putString(MediaConstants.EDGE_SHORTCUT_ACTION, action);

        playIntent.putExtras(playIntentExtras);


        data.putString(MediaConstants.APP_PACKAGE, "com.android.car.media");
        data.putString(MediaConstants.RES_ACTION_NAME, titleMetaName);
        data.putString(MediaConstants.RES_ACTION_ON, playIntent.toUri(0));
        data.putString(MediaConstants.RES_ACTION_OFF, "");
        data.putString(MediaConstants.RES_ACTION_ICON, "");
        data.putString(MediaConstants.RES_ICON_ACTION_ON, iconMetaName);
        data.putString(MediaConstants.RES_ICON_ACTION_OFF, "");

        data.putString(MediaConstants.ACTION_CODE, action);
        int actionDataType = 0;
        data.putInt(MediaConstants.DATA_TYPE, actionDataType);
        intent.putExtras(data);
        getContext().startService(intent);
    }


}
