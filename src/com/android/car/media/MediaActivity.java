package com.android.car.media;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.android.car.media.widget.MediaWidget1x1;
import com.android.car.media.widget.SubFragment;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.PSABaseActivity;
import com.harman.psa.widget.PSABaseNavigationManager;
import com.harman.psa.widget.PSATabBarManager;
import com.harman.psa.widget.toast.PSAToast;

import java.util.List;


public class MediaActivity extends PSABaseActivity implements MediaPlaybackModel.Listener {

    private static final String TAG = "MediaPlayerActivity";

    private static final String ACTION_MEDIA_APP_STATE_CHANGE
            = "android.intent.action.MEDIA_APP_STATE_CHANGE";
    private static final String EXTRA_MEDIA_APP_FOREGROUND
            = "android.intent.action.MEDIA_APP_STATE";

    private static final String TAB_VISIBLE_STATE = "tabVisibleState";

    private static final String SHARED_PREFS_NAME = "com.android.car.media.prefs";
    private static final String LAST_ACTIVE_APP = "LAST_ACTIVE_APP_KEY";

    private static int mActiveApp;

    private MediaPlaybackModel mMediaPlaybackModel;

    private SharedPreferences mSharedPrefs;

    private MediaNavigationManager mNavigationManager;

    /* App bar buttons */
    private PSAAppBarButton mBurgerMenuButton;
    private PSAAppBarButton mMediaSearchButton;
    private PSAAppBarButton mAppSwitchButton;
    private View mRadioSwitchButton;
    private View mMediaSwitchButton;
    private Bundle mWidgetExtra;

    private final MediaManager.Listener mListener = new MediaManager.Listener() {
        @Override
        public void onMediaAppChanged(ComponentName componentName) {
            sendMediaConnectionStatusBroadcast(componentName, MediaConstants.MEDIA_CONNECTED);
        }

        @Override
        public void onStatusMessageChanged(String msg) {
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        mNavigationManager = new MediaNavigationManager(this, getSupportFragmentManager(),
                getMainContentContainerId());
        mNavigationManager.setActiveApp(mActiveApp);

        getTabBarManager().addOnTabChangeListener(mNavigationManager);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setAppBarButtonsForActiveApp();

        PSATabBarManager tabManager = getTabBarManager();
        mNavigationManager.formMediaTabBar(tabManager, this);
        mNavigationManager.showActiveApp();

        if (mMediaPlaybackModel == null) {
            mMediaPlaybackModel = new MediaPlaybackModel(MediaActivity.this, null /*
            browserExtras */);
        }

        mMediaPlaybackModel.start();

        mMediaPlaybackModel.addListener(this);

        if (mActiveApp == MediaConstants.MEDIA_APP) {
            Intent i = new Intent(ACTION_MEDIA_APP_STATE_CHANGE);
            i.putExtra(EXTRA_MEDIA_APP_FOREGROUND, true);
            sendBroadcast(i);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        /* Keep last active app */
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putInt(LAST_ACTIVE_APP, mActiveApp).commit();

        if (mActiveApp == MediaConstants.MEDIA_APP) {
            Intent i = new Intent(ACTION_MEDIA_APP_STATE_CHANGE);
            i.putExtra(EXTRA_MEDIA_APP_FOREGROUND, false);
            sendBroadcast(i);
        }

        mMediaPlaybackModel.removeListener(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPrefs = getApplicationContext().getSharedPreferences(SHARED_PREFS_NAME, Context
                .MODE_PRIVATE);
        mActiveApp = mSharedPrefs.getInt(LAST_ACTIVE_APP, MediaConstants.MEDIA_APP);

        /* Open Tab bar button */
        View burgerButton = LayoutInflater.from(this).inflate(
                R.layout.psa_view_burger_menu_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_1),
                false);
        mBurgerMenuButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_1, burgerButton);
        getAppBarView().replaceAppBarButton(mBurgerMenuButton);
        burgerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getTabBarManager().isTabBarVisible()) {
                    getTabBarManager().hideTabBar();
                } else {
                    getTabBarManager().showTabBar();
                }
            }
        });

        /* Switch to App button */
        mRadioSwitchButton = LayoutInflater.from(MediaActivity.this).inflate(
                R.layout.psa_view_radio_open_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_2),
                false);
        mRadioSwitchButton.setEnabled(false);
        mRadioSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchApp();
            }
        });
        mMediaSwitchButton = LayoutInflater.from(MediaActivity.this).inflate(
                R.layout.psa_view_media_open_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_2),
                false);
        mMediaSwitchButton.setEnabled(false);
        mMediaSwitchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchApp();
            }
        });

        /* media search button */
        View mediaSearchButton = LayoutInflater.from(MediaActivity.this).inflate(
                R.layout.psa_view_media_search_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_4),
                false);
        mMediaSearchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_4,
                mediaSearchButton);
        getAppBarView().replaceAppBarButton(mMediaSearchButton);
        mediaSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PSAToast.makeText(MediaActivity.this, "To be Implemented", Toast.LENGTH_SHORT)
                        .show();
            }
        });


        if (savedInstanceState != null) {
            boolean isVisible = savedInstanceState.getBoolean(TAB_VISIBLE_STATE);
            if (isVisible) {
                getTabBarManager().showTabBar();
                getAppBarView().replaceAppBarButton(mBurgerMenuButton);
            } else {
                getTabBarManager().hideTabBar();
            }
            mActiveApp = savedInstanceState.getInt(LAST_ACTIVE_APP);
        }
        if (getTabBarManager().isTabBarVisible()) {
            getAppBarView().removeAppBarButton(mBurgerMenuButton.getPosition());
        }
        /* Hide right bar since we don't need one for media app*/
        getAppBarView().hideRightBar();

    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt(LAST_ACTIVE_APP, mActiveApp);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResumeFragments() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onResumeFragments");
        }
        super.onResumeFragments();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "onNewIntent(); intent: " + (intent == null ? "<< NULL >>" : intent));
        }

        setIntent(intent);
    }


    @Override
    public PSABaseNavigationManager getBaseNavigationManager() {
        return mNavigationManager;
    }

    protected void switchApp() {
        if (mActiveApp == MediaConstants.MEDIA_APP) {
            mActiveApp = MediaConstants.RADIO_APP;
            //Pause playback
            MediaController.TransportControls transportControls =
                    mMediaPlaybackModel.getTransportControls();
            transportControls.pause();
            mMediaPlaybackModel.stop();
            MediaManager.getInstance(this).removeListener(mListener);
        } else {
            mActiveApp = MediaConstants.MEDIA_APP;
            MediaManager.getInstance(this).addListener(mListener);
            mMediaPlaybackModel.start();
            sendMediaConnectionStatusBroadcast(
                    MediaManager.getInstance(this).getCurrentComponent(),
                    MediaConstants.MEDIA_CONNECTED);
        }
        mNavigationManager.setActiveApp(mActiveApp);
        mNavigationManager.formMediaTabBar(getTabBarManager(), this);
        mNavigationManager.showActiveApp();
    }


    private void handleIntent(Intent intent) {
        Bundle extras = null;
        if (intent != null) {
            extras = intent.getExtras();
        }

        // If the intent has a media component name set, connect to it directly
        if (extras != null && extras.containsKey(MediaManager.KEY_MEDIA_PACKAGE) &&
                extras.containsKey(MediaManager.KEY_MEDIA_CLASS)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Media component in intent.");
            }

            ComponentName component = new ComponentName(
                    intent.getStringExtra(MediaManager.KEY_MEDIA_PACKAGE),
                    intent.getStringExtra(MediaManager.KEY_MEDIA_CLASS)
            );
            MediaManager.getInstance(this).setMediaClientComponent(component);
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Launching most recent / default component.");
            }

            // Set it to the default GPM component.
            MediaManager.getInstance(this).connectToMostRecentMediaComponent(
                    new CarClientServiceAdapter(getPackageManager()));
        }

        if (isSearchIntent(intent)) {
            MediaManager.getInstance(this).processSearchIntent(intent);
            setIntent(null);
        }
    }

    /**
     * Returns {@code true} if the given intent is one that contains a search query for the
     * attached media application.
     */
    private boolean isSearchIntent(Intent intent) {
        return (intent != null && intent.getAction() != null &&
                intent.getAction().equals(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH));
    }

    private void sendMediaConnectionStatusBroadcast(ComponentName componentName,
                                                    String connectionStatus) {
        // There will be no current component if no media app has been chosen before.
        if (componentName == null) {
            return;
        }

        Intent intent = new Intent(MediaConstants.ACTION_MEDIA_STATUS);
        intent.setPackage(componentName.getPackageName());
        intent.putExtra(MediaConstants.MEDIA_CONNECTION_STATUS, connectionStatus);
        sendBroadcast(intent);
    }

    private void setAppBarButtonsForActiveApp() {
        if (mActiveApp == MediaConstants.RADIO_APP) {
            mAppSwitchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_2,
                    mMediaSwitchButton);
        } else {
            mAppSwitchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_2,
                    mRadioSwitchButton);
        }
        getAppBarView().replaceAppBarButton(mAppSwitchButton);
    }

    public MediaPlaybackModel getPlaybackModel() {
        return mMediaPlaybackModel;
    }

    @Override
    public void onMediaConnected() {

        Intent intent = getIntent();
        Bundle bundle = null;
        if (intent != null) {
            bundle = intent.getBundleExtra(MediaWidget1x1.PLAY_FROM_WIDGET);
        }
        if (bundle != null) {
            MediaController.TransportControls transportControls =
                    mMediaPlaybackModel.getTransportControls();
            transportControls.playFromMediaId(bundle.getString(SubFragment.ITEM_ID),
                    bundle);

            setIntent(null);
        }

    }

    @Override
    public void onMediaAppChanged(@Nullable ComponentName currentName,
                                  @Nullable ComponentName newName) {

    }

    @Override
    public void onMediaAppStatusMessageChanged(@Nullable String message) {

    }


    @Override
    public void onMediaConnectionSuspended() {

    }

    @Override
    public void onMediaConnectionFailed(CharSequence failedClientName) {

    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    public void onPlaybackStateChanged(@Nullable PlaybackState state) {

    }

    @Override
    public void onMetadataChanged(@Nullable MediaMetadata metadata) {

    }

    @Override
    public void onQueueChanged(List<MediaSession.QueueItem> queue) {
    }

    @Override
    public void onSessionDestroyed(CharSequence destroyedMediaClientName) {

    }
}