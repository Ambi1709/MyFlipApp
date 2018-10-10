package com.android.car.media;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.car.app.CarDrawerActivity;
import com.android.car.apps.common.BitmapDownloader;
import com.android.car.apps.common.BitmapWorkerOptions;
import com.android.car.apps.common.ColorChecker;
import com.android.car.apps.common.util.Assert;
import com.android.car.media.util.widgets.PlayPauseStopImageView;

import com.harman.psa.widget.longpressview.PSALongPressView;
import com.harman.psa.widget.longpressview.OnLongClickListener;
import com.harman.psa.widget.button.PSACyclicButton;
import com.harman.psa.widget.button.OnCycleChangeListener;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.dropdowns.DropdownButton;
import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownHelper;
import com.harman.psa.widget.dropdowns.DropdownItem;
import com.harman.psa.widget.dropdowns.listener.OnDismissListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownButtonClickEventListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownItemClickListener;
import com.harman.psa.widget.feedback.PSAFeedbackDialog;
import com.harman.psa.widget.toast.PSAToast;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

import com.android.car.media.R;

import android.media.browse.MediaBrowser;

/**
 * Fragment that displays the media playback UI.
 */
public class MediaPlaybackFragment extends PSABaseFragment implements MediaPlaybackModel.Listener {
    private static final String TAG = "MediaPlayback";

    /**
     * The preferred ordering for bitmap to fetch. The metadata at lower indexes are preferred to
     * those at higher indexes.
     */
    private static final String[] PREFERRED_BITMAP_TYPE_ORDER = {
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
    };

    /**
     * The preferred ordering for metadata URIs to fetch. The metadata at lower indexes are
     * preferred to those at higher indexes.
     */
    private static final String[] PREFERRED_URI_ORDER = {
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
    };

    // The different types of Views that are contained within this Fragment.
    private static final int NO_CONTENT_VIEW = 0;
    private static final int PLAYBACK_CONTROLS_VIEW = 1;
    private static final int LOADING_VIEW = 2;

    @IntDef({NO_CONTENT_VIEW, PLAYBACK_CONTROLS_VIEW, LOADING_VIEW})
    private @interface ViewType {
    }

    /**
     * The amount of time between seek bar updates.
     */
    private static final long SEEK_BAR_UPDATE_TIME_INTERVAL_MS = 500;

    /**
     * The delay time before automatically closing the overflow controls view.
     */
    private static final long DELAY_CLOSE_OVERFLOW_MS = 3500;

    /**
     * Delay before showing any content. When the media app cold starts, it usually takes a
     * moment to load the last played song from database. So wait for three seconds before showing
     * the no content view rather than showing it and immediately switching to the playback view
     * when the metadata loads.
     */
    private static final long DELAY_SHOW_NO_CONTENT_VIEW_MS = 3000;
    private static final long FEEDBACK_MESSAGE_DISPLAY_TIME_MS = 6000;
    private static final int MEDIA_SCRIM_FADE_DURATION_MS = 400;
    private static final int OVERFLOW_MENU_FADE_DURATION_MS = 250;
    private static final int NUM_OF_CUSTOM_ACTION_BUTTONS = 4;

    // The default width and height for an image. These are used if the mAlbumArtView has not laid
    // out by the time a Bitmap needs to be created to fit in it.
    private static final int DEFAULT_ALBUM_ART_WIDTH = 320;
    private static final int DEFAULT_ALBUM_ART_HEIGHT = 320;

    private MediaPlaybackModel mMediaPlaybackModel;
    private final Handler mHandler = new Handler();

    private View mScrimView;
    private float mDefaultScrimAlpha;
    private float mDarkenedScrimAlpha;

    private CrossfadeImageView mAlbumArtView;

    private TextView mTitleView;
    private TextView mArtistView;

    private ImageView mPrevButton;
    private PlayPauseStopImageView mPlayPauseStopButton;
    private ImageView mNextButton;
    private PSACyclicButton mRepeatButton;
    private PSACyclicButton mShuffleButton;
    private View mCustomActionsPanel;

    private int mShuffleState = 0;
    private int mRepeatState = 0;

    private View mMusicPanel;
    private View mControlsView;
    private View mStatusPanel;

    private SeekBar mSeekBar;
    private TextView mCurrentTimeView;
    private TextView mDurationView;
    private View mProgressPanel;

    private long mStartProgress;
    private long mStartTime;
    private MediaDescription mCurrentTrack;
    private boolean mShowingMessage;

    private View mInitialNoContentView;
    private View mMetadata;
    private View mMusicErrorIcon;
    private TextView mTapToSelectText;
    private ProgressBar mAppConnectingSpinner;

    private boolean mDelayedResetTitleInProgress;
    private int mAlbumArtWidth = DEFAULT_ALBUM_ART_WIDTH;
    private int mAlbumArtHeight = DEFAULT_ALBUM_ART_HEIGHT;
    private int mShowTitleDelayMs;

    private TelephonyManager mTelephonyManager;
    private boolean mInCall;

    private BitmapDownloader mDownloader;
    private boolean mReturnFromOnStop;
    @ViewType
    private int mCurrentView;
    private PlayQueueRevealer mPlayQueueRevealer;

    /* App bar buttons */
    private PSAAppBarButton mSourceSwitchButton;

    private DropdownDialog mDropdownDialog;

    private HashMap<Integer, Integer> mSourceIconMap = new HashMap();
    private int mSourceId = 0;

    /**
     * An interface that is responsible for displaying a list of the items in the user's currently
     * playing queue.
     */
    interface PlayQueueRevealer {
        void showPlayQueue();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context context = getContext();
        mMediaPlaybackModel = new MediaPlaybackModel(context, null /* browserExtras */);
        mMediaPlaybackModel.addListener(this);

        mTelephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        Resources res = context.getResources();
        mShowTitleDelayMs = res.getInteger(R.integer.new_album_art_fade_in_duration);
        mDefaultScrimAlpha = res.getFloat(R.dimen.media_scrim_alpha);
        mDarkenedScrimAlpha = res.getFloat(R.dimen.media_scrim_darkened_alpha);
    }

    /**
     * Sets the object that is responsible for displaying the current list of items in the user's
     * play queue.
     */
    void setPlayQueueRevealer(PlayQueueRevealer revealer) {
        mPlayQueueRevealer = revealer;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mMediaPlaybackModel.removeListener(this);
        mMediaPlaybackModel = null;
        // Calling this with null will clear queue of callbacks and message.
        mHandler.removeCallbacksAndMessages(null);
        mDelayedResetTitleInProgress = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.now_playing_screen, container, false);
        mScrimView = v.findViewById(R.id.scrim);
        mAlbumArtView = v.findViewById(R.id.album_art);
        mAlbumArtView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mAlbumArtWidth = mAlbumArtView.getWidth();
                        mAlbumArtHeight = mAlbumArtView.getHeight();
                        mAlbumArtView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        setBackgroundColor(getContext().getColor(R.color.music_default_artwork));

        mTitleView = v.findViewById(R.id.title);
        mArtistView = v.findViewById(R.id.artist);
        mSeekBar = v.findViewById(R.id.seek_bar);
        mCurrentTimeView = v.findViewById(R.id.current_time);
        mDurationView = v.findViewById(R.id.duration);

        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mProgressPanel = v.findViewById(R.id.psa_media_player_progress_container);

        mControlsView = v.findViewById(R.id.controls);
        mMusicPanel = v.findViewById(R.id.music_panel);
        mStatusPanel = v.findViewById(R.id.psa_media_player_mini_status_bar_container);
        mInitialNoContentView = v.findViewById(R.id.initial_view);
        mMetadata = v.findViewById(R.id.metadata);

        mCustomActionsPanel = v.findViewById(R.id.custom_actions_container);

        mMusicErrorIcon = v.findViewById(R.id.error_icon);
        mTapToSelectText = v.findViewById(R.id.tap_to_select_item);
        mAppConnectingSpinner = v.findViewById(R.id.loading_spinner);

        setupMediaButtons(v);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* source switch button */
        //TODO implement source selection
        generateSourceIconMap();
        DropdownButton sourceSwitchButton = (DropdownButton) LayoutInflater.from(getContext()).inflate(
                R.layout.psa_view_source_switch_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_3),
                false);
        sourceSwitchButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), mSourceIconMap.get(mSourceId), getActivity().getTheme()));
        mSourceSwitchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_3, sourceSwitchButton);
        getAppBarView().replaceAppBarButton(mSourceSwitchButton);
        sourceSwitchButton.setOnDropdownButtonClickEventListener(new OnDropdownButtonClickEventListener() {
            @Override
            public void onClick(DropdownButton view) {
                DropdownDialog.setDefaultColor(ResourcesCompat.getColor(getResources(), R.color.psa_dropdown_shadow_color,
                        getActivity().getTheme()));
                DropdownDialog.setDefaultTextColor(Color.BLACK);

                mDropdownDialog = new DropdownDialog(getActivity().getApplicationContext(), DropdownDialog.VERTICAL);
                mDropdownDialog.setColor(ResourcesCompat.getColor(getResources(), R.color.psa_general_background_color3,
                        getActivity().getTheme()));
                mDropdownDialog.setTextColorRes(R.color.psa_dropdown_thumb_color);

                // TODO refresh sources list
                int[] i = {0};
                mSourceIconMap.entrySet().stream().forEach(e -> {
                    mDropdownDialog.addDropdownItem(new DropdownItem(++(i[0]), e.getKey() + " source", e.getValue()));
                });

                //Set listener for action item clicked
                mDropdownDialog.setOnActionItemClickListener(new OnDropdownItemClickListener() {
                    @Override
                    public void onItemClick(DropdownItem item) {
                        //here we can filter which action item was clicked with pos or actionId parameter
                        String title = item.getTitle();
                        PSAToast.makeText(getContext(), title + " selected", Toast.LENGTH_SHORT).show();
                        if (!item.isSticky()) {
                            mDropdownDialog.removeDropdownItem(item);
                        }
                        mSourceId = item.getItemId();
                        ((DropdownButton) mSourceSwitchButton.getAppBarButton()).setImageDrawable(
                                ResourcesCompat.getDrawable(getResources(), mSourceIconMap.get(mSourceId), getActivity().getTheme()));
                        
                        /***** Temporary *****/
                        mMediaPlaybackModel.getMediaBrowser().subscribe("__FOLDERS__", new MediaBrowser.SubscriptionCallback() {});
                        mMediaPlaybackModel.getMediaBrowser().subscribe("/system/media/audio/ringtones%", new MediaBrowser.SubscriptionCallback() {
                            @Override
                            public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                                Log.d(TAG, "onChildrenLoaded " + parentId);
                                MediaBrowser.MediaItem item = children.get(0);

                                MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
                                if (controls != null) {
                                    controls.pause();
                                    controls.playFromMediaId(item.getMediaId(), item.getDescription().getExtras());
                                }
                            }

                            @Override
                            public void onError(String parentId) {
                                Log.e(TAG, "Error loading children of: /system/media/audio/ringtones%");
                            }
                        });
                        /***** Temporary *****/
                    }
                });

                mDropdownDialog.setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss() {
                    }
                });
                mDropdownDialog.show(view, DropdownHelper.Side.LEFT);
            }
        });
    }

    private void setupMediaButtons(View parentView) {
        mPrevButton = parentView.findViewById(R.id.prev);
        mNextButton = parentView.findViewById(R.id.next);
        mPlayPauseStopButton = parentView.findViewById(R.id.play_pause);

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.psa_general_major_color1, typedValue, true);
        final int colorAccent = typedValue.data;

        getActivity().getTheme().resolveAttribute(R.attr.psa_general_content_color1, typedValue, true);
        final int colorContent = typedValue.data;

        mRepeatButton = parentView.findViewById(R.id.repeat);
        mShuffleButton = parentView.findViewById(R.id.shuffle);

        mPrevButton.setOnClickListener(mControlsClickListener);
        mNextButton.setOnClickListener(mControlsClickListener);

        mPlayPauseStopButton.setOnClickListener(mControlsClickListener);

        mRepeatButton.setImages(new int[]{R.drawable.psa_media_button_icon_repeat_off,
                R.drawable.psa_media_button_icon_repeat_on,
                R.drawable.psa_media_button_icon_repeat_one});

        mShuffleButton.setImages(new int[]{R.drawable.psa_media_button_icon_shuffle_on,
                R.drawable.psa_media_button_icon_shuffle_on});
    }

    @Override
    public void onPause() {
        super.onPause();
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        // When switch apps, onStop() will be called. Mark it and don't show fade in/out title and
        // background animations when come back.
        mReturnFromOnStop = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMediaPlaybackModel.start();
        onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
        // Note: at registration, TelephonyManager will invoke the callback with the current state.
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onMediaAppChanged(@Nullable ComponentName currentName,
                                  @Nullable ComponentName newName) {
        Assert.isMainThread();
        resetTitle();
        if (Objects.equals(currentName, newName)) {
            return;
        }
        int accentColor = mMediaPlaybackModel.getAccentColor();
        int overflowViewColor = mMediaPlaybackModel.getPrimaryColorDark();
        int overflowTintColor = ColorChecker.getTintColor(getContext(), overflowViewColor);
        ColorStateList colorStateList = ColorStateList.valueOf(accentColor);

        mAppConnectingSpinner.setIndeterminateTintList(ColorStateList.valueOf(accentColor));
        showLoadingView();
    }

    @Override
    public void onMediaAppStatusMessageChanged(@Nullable String message) {
        Assert.isMainThread();
        if (message == null) {
            resetTitle();
        } else {
            showMessage(message);
        }
    }

    @Override
    public void onMediaConnected() {
        Assert.isMainThread();
        onMetadataChanged(mMediaPlaybackModel.getMetadata());
        onQueueChanged(mMediaPlaybackModel.getQueue());
        onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
        mReturnFromOnStop = false;
    }

    @Override
    public void onMediaConnectionSuspended() {
        Assert.isMainThread();
        mReturnFromOnStop = false;
    }

    @Override
    public void onMediaConnectionFailed(CharSequence failedClientName) {
        Assert.isMainThread();
        showInitialNoContentView(getString(R.string.cannot_connect_to_app, failedClientName),
                true /* isError */);
        mReturnFromOnStop = false;
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

        if (state.getState() == PlaybackState.STATE_ERROR) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "ERROR: " + state.getErrorMessage());
            }
            String message = TextUtils.isEmpty(state.getErrorMessage())
                    ? getString(R.string.unknown_error)
                    : state.getErrorMessage().toString();
            showInitialNoContentView(message, true /* isError */);
            return;
        }

        mStartProgress = state.getPosition();
        mStartTime = System.currentTimeMillis();
        int currentTime = (int) mStartProgress;
        setProgress(currentTime);
        if (state.getState() == PlaybackState.STATE_PLAYING) {
            mHandler.post(mSeekBarRunnable);
        } else {
            mHandler.removeCallbacks(mSeekBarRunnable);
        }
        if (!mInCall) {
            int playbackState = state.getState();
            mPlayPauseStopButton.setPlayState(playbackState);
            // Due to the action of PlaybackState will be changed when the state of PlaybackState is
            // changed, we set mode every time onPlaybackStateChanged() is called.
            if (playbackState == PlaybackState.STATE_PLAYING ||
                    playbackState == PlaybackState.STATE_BUFFERING) {
                mPlayPauseStopButton.setMode(((state.getActions() & PlaybackState.ACTION_STOP) != 0)
                        ? PlayPauseStopImageView.MODE_STOP : PlayPauseStopImageView.MODE_PAUSE);
            } else {
                mPlayPauseStopButton.setMode(PlayPauseStopImageView.MODE_PAUSE);
            }
            mPlayPauseStopButton.refreshDrawableState();
        }
        updateActions(state.getActions(), state.getCustomActions());

        if (mMediaPlaybackModel.getMetadata() == null) {
            return;
        }
        showMediaPlaybackControlsView();
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

        showMediaPlaybackControlsView();
        mCurrentTrack = metadata.getDescription();
        Bitmap icon = getMetadataBitmap(metadata);
        if (!mShowingMessage) {
            mHandler.removeCallbacks(mSetTitleRunnable);
            // Show the title when the new album art starts to fade in, but don't need to show
            // the fade in animation when come back from switching apps.
            mHandler.postDelayed(mSetTitleRunnable,
                    icon == null || mReturnFromOnStop ? 0 : mShowTitleDelayMs);
        }
        Uri iconUri = getMetadataIconUri(metadata);
        if (icon != null) {
            Bitmap scaledIcon = cropAlbumArt(icon);
            if (scaledIcon != icon && !icon.isRecycled()) {
                icon.recycle();
            }
            // Fade out the old background and then fade in the new one when the new album art
            // starts, but don't need to show the fade out and fade in animations when come back
            // from switching apps.
            setBackgroundBitmap(scaledIcon, !mReturnFromOnStop /* showAnimation */);
        } else if (iconUri != null) {
            if (mDownloader == null) {
                mDownloader = new BitmapDownloader(getContext());
            }
            final int flags = BitmapWorkerOptions.CACHE_FLAG_DISK_DISABLED
                    | BitmapWorkerOptions.CACHE_FLAG_MEM_DISABLED;
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Album art size " + mAlbumArtWidth + "x" + mAlbumArtHeight);
            }

            BitmapWorkerOptions bitmapWorkerOptions = new BitmapWorkerOptions.Builder(getContext())
                    .resource(iconUri)
                    .height(mAlbumArtHeight)
                    .width(mAlbumArtWidth)
                    .cacheFlag(flags)
                    .build();

            mDownloader.getBitmap(bitmapWorkerOptions,
                    new BitmapDownloader.BitmapCallback() {
                        @Override
                        public void onBitmapRetrieved(Bitmap bitmap) {
                            setBackgroundBitmap(bitmap, true /* showAnimation */);
                        }
                    });
        } else {
            setBackgroundColor(mMediaPlaybackModel.getPrimaryColorDark());
        }

        int durationTime = (int) metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        mSeekBar.setMax(durationTime);
        mDurationView.setText(convertMstoMinSec(durationTime));
    }

    @Override
    public void onQueueChanged(List<MediaSession.QueueItem> queue) {
        Assert.isMainThread();
    }

    @Override
    public void onSessionDestroyed(CharSequence destroyedMediaClientName) {
        Assert.isMainThread();
        mHandler.removeCallbacks(mSeekBarRunnable);
        showInitialNoContentView(
                getString(R.string.cannot_connect_to_app, destroyedMediaClientName), true);
    }

    /**
     * Sets the given {@link Bitmap} as the background of this playback fragment. If
     *
     * @param showAnimation {@code true} if the bitmap should be faded in.
     */
    private void setBackgroundBitmap(Bitmap bitmap, boolean showAnimation) {
        mAlbumArtView.setImageBitmap(bitmap, showAnimation);
    }

    /**
     * Sets the given color as the background color of the view.
     */
    private void setBackgroundColor(int color) {
        mAlbumArtView.setBackgroundColor(color);
    }

    /**
     * Darkens the scrim's alpha level.
     */
    private void darkenScrim() {
        mScrimView.animate()
                .alpha(mDarkenedScrimAlpha)
                .setDuration(MEDIA_SCRIM_FADE_DURATION_MS);
    }

    /**
     * Sets whether or not the scrim is visible. The scrim is a semi-transparent View that darkens
     * an album art so that does not overpower any text that is over it.
     */
    private void setScrimVisible(boolean visible) {
        float alpha = visible ? mDefaultScrimAlpha : 0.f;
        mScrimView.animate()
                .alpha(alpha)
                .setDuration(MEDIA_SCRIM_FADE_DURATION_MS);
    }

    /**
     * Displays the given message to the user. The message is displayed in the field that
     * normally displays the title of the currently playing media item.
     */
    private void showMessage(String message) {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "showMessage(); message: " + message);
        }


        mHandler.removeCallbacks(mResetTitleRunnable);
        darkenScrim();
        mTitleView.setText(message);
        mArtistView.setVisibility(View.GONE);
        mShowingMessage = true;
    }


    /**
     * For a given drawer slot, set the proper action of the slot's button,
     * based on the slot being reserved and the corresponding action being enabled.
     * If the slot is not reserved and the corresponding action is disabled,
     * then the next available custom action is assigned to the button.
     *
     * @param button             The button corresponding to the slot
     * @param originalResId      The drawable resource ID for the original button,
     *                           only used if the original action is not replaced by a custom action.
     * @param slotAlwaysReserved True if the slot should be empty when the
     *                           corresponding action is disabled. If false, when the action is disabled
     *                           the slot has its default action replaced by the next custom action, if any.
     * @param isOriginalEnabled  True if the original action of this button is
     *                           enabled.
     * @param customActions      A list of custom actions still unassigned to slots.
     */
    private void handleSlot(ImageView button, int originalResId, boolean slotAlwaysReserved,
                            boolean isOriginalEnabled, List<PlaybackState.CustomAction> customActions) {
        if (isOriginalEnabled || slotAlwaysReserved) {
            setActionDrawable(button, originalResId, getResources());
            button.setVisibility(isOriginalEnabled ? View.VISIBLE : View.INVISIBLE);
            button.setTag(null);
            return;
        }

        // SHUFFLE and REPEAT custom actions
        if (customActions.isEmpty()) {
            button.setVisibility(View.INVISIBLE);
            return;
        }
        PlaybackState.CustomAction customAction = customActions.remove(0);
        Bundle extras = customAction.getExtras();

        button.setVisibility(View.VISIBLE);
        button.setTag(customAction);

        if (extras.getInt(MediaConstants.ACTION_SHUFFLE_STATE, -1) != -1) {
            //SHUFFLE
            mShuffleButton.setListener(mShuffleButtonClickListener);
            mShuffleButton.setPosition(extras.getInt(MediaConstants.ACTION_SHUFFLE_STATE, mShuffleState));
        } else if (extras.getInt(MediaConstants.ACTION_REPEAT_STATE, -1) != -1) {
            //REPEAT
            mRepeatButton.setListener(mRepeatButtonClickListener);
            mRepeatButton.setPosition(extras.getInt(MediaConstants.ACTION_REPEAT_STATE, mRepeatState));
        }
    }

    /**
     * Takes a list of custom actions and standard actions and displays them in the media
     * controls card (or hides ones that aren't available).
     *
     * @param actions       A bit mask of active actions (android.media.session.PlaybackState#ACTION_*).
     * @param customActions A list of custom actions specified by the
     *                      {@link android.media.session.MediaSession}.
     */
    private void updateActions(long actions, List<PlaybackState.CustomAction> customActions) {
        List<MediaSession.QueueItem> mediaQueue = mMediaPlaybackModel.getQueue();

        handleSlot(
                mPrevButton, R.drawable.psa_media_button_icon_prev,
                mMediaPlaybackModel.isSlotForActionReserved(
                        MediaConstants.EXTRA_RESERVED_SLOT_SKIP_TO_PREVIOUS),
                (actions & PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0,
                customActions);

        handleSlot(
                mNextButton, R.drawable.psa_media_button_icon_next,
                mMediaPlaybackModel.isSlotForActionReserved(
                        MediaConstants.EXTRA_RESERVED_SLOT_SKIP_TO_NEXT),
                (actions & PlaybackState.ACTION_SKIP_TO_NEXT) != 0,
                customActions);

        handleSlot(mShuffleButton, 0, false, false, customActions);
        handleSlot(mRepeatButton, 0, false, false, customActions);
    }

    private void showInitialNoContentView(String msg, boolean isError) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "showInitialNoContentView()");
        }
        if (mCurrentView == NO_CONTENT_VIEW) {
            return;
        }
        mCurrentView = NO_CONTENT_VIEW;
        mAppConnectingSpinner.setVisibility(View.GONE);
        setScrimVisible(false);
        if (isError) {
            setBackgroundColor(getContext().getColor(R.color.car_error_screen));
            mMusicErrorIcon.setVisibility(View.VISIBLE);
        } else {
            setBackgroundColor(getContext().getColor(R.color.car_dark_blue_grey_800));
            mMusicErrorIcon.setVisibility(View.INVISIBLE);
        }
        mTapToSelectText.setVisibility(View.VISIBLE);
        mTapToSelectText.setText(msg);
        mInitialNoContentView.setVisibility(View.VISIBLE);
        mMetadata.setVisibility(View.GONE);
        mMusicPanel.setVisibility(View.GONE);
        mCustomActionsPanel.setVisibility(View.GONE);
        mProgressPanel.setVisibility(View.GONE);
        mStatusPanel.setVisibility(View.GONE);
    }

    private void showMediaPlaybackControlsView() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "showMediaPlaybackControlsView()");
        }
        if (mCurrentView == PLAYBACK_CONTROLS_VIEW) {
            return;
        }
        mCurrentView = PLAYBACK_CONTROLS_VIEW;
        if (!mShowingMessage) {
            setScrimVisible(true);
        }
        mTapToSelectText.setVisibility(View.GONE);
        mInitialNoContentView.setVisibility(View.GONE);
        mMetadata.setVisibility(View.VISIBLE);
        mMusicPanel.setVisibility(View.VISIBLE);
        mCustomActionsPanel.setVisibility(View.VISIBLE);
        mProgressPanel.setVisibility(View.VISIBLE);
        mStatusPanel.setVisibility(View.VISIBLE);
    }

    private void showLoadingView() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "showLoadingView()");
        }
        if (mCurrentView == LOADING_VIEW) {
            return;
        }
        mCurrentView = LOADING_VIEW;
        setBackgroundColor(getContext().getColor(R.color.music_loading_view_background));
        mAppConnectingSpinner.setVisibility(View.VISIBLE);
        mMusicErrorIcon.setVisibility(View.GONE);
        mTapToSelectText.setVisibility(View.GONE);
        mInitialNoContentView.setVisibility(View.VISIBLE);
        mMetadata.setVisibility(View.GONE);
        mMusicPanel.setVisibility(View.GONE);
    }

    private void resetTitle() {
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "resetTitle()");
        }
        if (!mShowingMessage) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Message not currently shown; not resetting title");
            }
            return;
        }
        // Feedback message is currently being displayed, reset will automatically take place when
        // the display interval expires.
        if (mDelayedResetTitleInProgress) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Delayed reset title is in progress; not resetting title now");
            }
            return;
        }
        setScrimVisible(true);
        mArtistView.setVisibility(View.VISIBLE);
        if (mCurrentTrack != null) {
            mTitleView.setText(mCurrentTrack.getTitle());
            mArtistView.setText(mCurrentTrack.getSubtitle());
        }
        mShowingMessage = false;
    }

    private Bitmap cropAlbumArt(Bitmap icon) {
        if (icon == null) {
            return null;
        }
        int width = icon.getWidth();
        int height = icon.getHeight();
        int startX = width > mAlbumArtWidth ? (width - mAlbumArtWidth) / 2 : 0;
        int startY = height > mAlbumArtHeight ? (height - mAlbumArtHeight) / 2 : 0;
        int newWidth = width > mAlbumArtWidth ? mAlbumArtWidth : width;
        int newHeight = height > mAlbumArtHeight ? mAlbumArtHeight : height;

        return Bitmap.createBitmap(icon, startX, startY, newWidth, newHeight);
    }

    private Bitmap getMetadataBitmap(MediaMetadata metadata) {
        // Get the best art bitmap we can find
        for (String bitmapType : PREFERRED_BITMAP_TYPE_ORDER) {
            Bitmap bitmap = metadata.getBitmap(bitmapType);
            if (bitmap != null) {
                return bitmap;
            }
        }
        return null;
    }

    private Uri getMetadataIconUri(MediaMetadata metadata) {
        // Get the best Uri we can find
        for (String bitmapUri : PREFERRED_URI_ORDER) {
            String iconUri = metadata.getString(bitmapUri);
            if (!TextUtils.isEmpty(iconUri)) {
                return Uri.parse(iconUri);
            }
        }
        return null;
    }

    /**
     * Sets the drawable given by the {@code resId} on the specified {@link ImageButton}.
     *
     * @param resources The {@link Resources} to retrieve the Drawable from. This may be different
     *                  from the Resources of this Fragment.
     */
    private void setActionDrawable(ImageView button, @DrawableRes int resId,
                                   Resources resources) {
        if (resources == null) {
            Log.e(TAG, "Resources is null. Icons will not show up.");
            return;
        }

        Resources myResources = getResources();
        // The resources may be from another package. We need to update the configuration using
        // the context from the activity so we get the drawable from the correct DPI bucket.
        resources.updateConfiguration(myResources.getConfiguration(),
                myResources.getDisplayMetrics());
        try {
            Drawable icon = resources.getDrawable(resId, null);
            int inset = myResources.getDimensionPixelSize(R.dimen.music_action_icon_inset);
            InsetDrawable insetIcon = new InsetDrawable(icon, inset);
            button.setImageDrawable(insetIcon);
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "Resource not found: " + resId);
        }
    }

    private void checkAndDisplayFeedbackMessage(PlaybackState.CustomAction ca) {
        Bundle extras = ca.getExtras();
        if (extras == null) {
            return;
        }

        String feedbackMessage = extras.getString(MediaConstants.EXTRA_CUSTOM_ACTION_STATUS, "");
        if (!TextUtils.isEmpty(feedbackMessage)) {
            // Show feedback message that appears for a time interval unless a new
            // message is shown.
            showMessage(feedbackMessage);
            mDelayedResetTitleInProgress = true;
            mHandler.postDelayed(mResetTitleRunnable, FEEDBACK_MESSAGE_DISPLAY_TIME_MS);
        }
    }

    private final View.OnClickListener mControlsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!mMediaPlaybackModel.isConnected()) {
                Log.e(TAG, "Unable to send action for " + v
                        + ". The MediaPlaybackModel is not connected.");
                return;
            }

            MediaController.TransportControls transportControls =
                    mMediaPlaybackModel.getTransportControls();

            if (v.getTag() instanceof PlaybackState.CustomAction) {
                PlaybackState.CustomAction ca = (PlaybackState.CustomAction) v.getTag();
                checkAndDisplayFeedbackMessage(ca);
                Bundle caExtras = ca.getExtras();
                if (v.getId() == R.id.shuffle) {
                    caExtras.putInt(MediaConstants.ACTION_SHUFFLE_STATE, mShuffleState);
                } else if (v.getId() == R.id.repeat) {
                    caExtras.putInt(MediaConstants.ACTION_REPEAT_STATE, mRepeatState);
                }
                transportControls.sendCustomAction(ca, ca.getExtras());
                return;
            }

            switch (v.getId()) {
                case R.id.prev:
                    transportControls.skipToPrevious();
                    break;
                case R.id.play_pause:
                    handlePlaybackStateForPlay(mMediaPlaybackModel.getPlaybackState(),
                            transportControls);
                    break;
                case R.id.next:
                    transportControls.skipToNext();
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

    private final SeekBar.OnSeekBarChangeListener mSeekBarChangeListener
            = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mMediaPlaybackModel.getTransportControls().seekTo(seekBar.getProgress());
            mMediaPlaybackModel.getTransportControls().play();
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mMediaPlaybackModel.getTransportControls().pause();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                mMediaPlaybackModel.getTransportControls().seekTo(progress);
            }
            setProgress(progress);
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
            mControlsClickListener.onClick(mShuffleButton);
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
            mControlsClickListener.onClick(mRepeatButton);
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mPlayPauseStopButton
                            .setPlayState(PlayPauseStopImageView.PLAYBACKSTATE_DISABLED);
                    mPlayPauseStopButton.setMode(PlayPauseStopImageView.MODE_PAUSE);
                    mPlayPauseStopButton.refreshDrawableState();
                    mInCall = true;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mInCall) {
                        PlaybackState playbackState = mMediaPlaybackModel.getPlaybackState();
                        if (playbackState != null) {
                            mPlayPauseStopButton.setPlayState(playbackState.getState());

                            boolean isStopAction =
                                    (playbackState.getActions() & PlaybackState.ACTION_STOP) != 0;

                            mPlayPauseStopButton.setMode(isStopAction
                                    ? PlayPauseStopImageView.MODE_STOP
                                    : PlayPauseStopImageView.MODE_PAUSE);
                            mPlayPauseStopButton.refreshDrawableState();
                        }
                        mInCall = false;
                    }
                    break;
                default:
                    Log.w(TAG, "TelephonyManager reports an unknown call state: " + state);
            }
        }
    };

    private final Runnable mSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            int currentTime = (int) (System.currentTimeMillis() - mStartTime + mStartProgress);
            setProgress(currentTime);
            mHandler.postDelayed(this, SEEK_BAR_UPDATE_TIME_INTERVAL_MS);
        }
    };

    private final Runnable mShowNoContentViewRunnable =
            () -> showInitialNoContentView(getString(R.string.nothing_to_play), false);

    private final Runnable mResetTitleRunnable = () -> {
        mDelayedResetTitleInProgress = false;
        resetTitle();
    };

    private final Runnable mSetTitleRunnable = () -> {
        mTitleView.setText(mCurrentTrack.getTitle());
        mArtistView.setText(mCurrentTrack.getSubtitle());
    };

    private String convertMstoMinSec(int millisecs) {
        return String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(millisecs),
                TimeUnit.MILLISECONDS.toSeconds(millisecs) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millisecs))
        );
    };

    private void setProgress(int progress) {
        mSeekBar.setProgress(progress);
        mCurrentTimeView.setText(convertMstoMinSec(progress));
    }

    private void generateSourceIconMap() {
        mSourceIconMap.put(1, R.drawable.psa_media_source_bluetooth);
        mSourceIconMap.put(2, R.drawable.psa_media_source_aux);
        mSourceIconMap.put(3, R.drawable.psa_media_source_ipod);
        mSourceIconMap.put(4, R.drawable.psa_media_source_usb);
        mSourceIconMap.put(5, R.drawable.psa_media_source_disc);
        mSourceIconMap.put(6, R.drawable.psa_media_source_folder);
        mSourceId = 6;
    }
}
