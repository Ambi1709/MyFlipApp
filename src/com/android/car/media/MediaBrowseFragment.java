package com.android.car.media;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.LibraryGridListAdapter;
import com.android.car.media.MediaLibraryController;
import com.android.car.media.MediaLibraryFragment;
import com.android.car.media.MediaPlaybackModel;
import com.android.car.usb.PSAUsbStateService;
import com.android.car.usb.UsbDevice;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.dropdowns.DropdownButton;
import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownHelper;
import com.harman.psa.widget.dropdowns.DropdownItem;
import com.harman.psa.widget.dropdowns.listener.OnDismissListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownButtonClickEventListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownItemClickListener;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MediaBrowseFragment extends MediaBaseFragment implements
        LibraryGridListAdapter.LibraryGridListItemClickListener,
        LibraryCategoryVerticalListAdapter.OnItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback,
        OnDropdownButtonClickEventListener, OnDropdownItemClickListener, PSAUsbStateService.UsbDeviceStateListener,
        OnDismissListener {
    private static final String TAG = "MediaBrowseFragment";

    private Context mContext;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private MediaPlaybackModel mMediaPlaybackModel;
    private MediaLibraryController mMediaLibraryController;

    private String mRootId = "";

    private List<LibraryCategoryGridItemData> mCategoriesList = new ArrayList<>();

    private PSAAppBarButton mSourceSwitchButton;
    private PSAUsbStateService mUsbNotificationService;

    private DropdownDialog mDropdownDialog;
    private List<DropdownItem> mDropdownItems = new ArrayList<>();
    private String mSourceId;

    private List<ItemData> mRecentlyPlayedList = new ArrayList<>();
    private RecyclerView mRecentlyPlayedRecyclerView;
    private LibraryCategoryVerticalListAdapter mRecentlyPlayedListAdapter;
    private TextView mRecentlyPlayedTextView;

    private boolean mIsEditMode;
    private Handler mEdgeHandler = new Handler();
    private int mEdgePosition = MediaConstants.UNDEFINED_EDGE_POSITION;
    private MediaNavigationManager mNavigationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaLibraryController = ((MediaActivity) getHostActivity()).getLibraryController();

        mNavigationManager = ((MediaActivity) getHostActivity()).getNavigationManagerImpl();

        mMediaLibraryController.addListener(this);
        mMediaLibraryController.updateRootElements();
    }

    @Override
    public void onStart() {
        super.onStart();
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter("com.harman.edge.EDGE"));
        getContext().registerReceiver(mDisableEditModeReceiver, new IntentFilter(MediaConstants.BROADCAST_MAGIC_TOUCH_EDIT_MODE));
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBroadcastReceiver != null) {
            getContext().unregisterReceiver(mBroadcastReceiver);
        }
        if(mDisableEditModeReceiver != null){
            getContext().unregisterReceiver(mDisableEditModeReceiver);
        }
        mEdgeHandler.removeCallbacksAndMessages(null);
        mMediaLibraryController.unsubscribe();
        mMediaLibraryController.removeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getView();
        if (v == null) {
            v = inflater.inflate(R.layout.psa_media_browse_fragment, container, false);
        }

        mRecyclerView = v.findViewById(R.id.library_grid);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setNestedScrollingEnabled(false);
        mAdapter = new LibraryGridListAdapter(mCategoriesList, this);
        mRecyclerView.setAdapter(mAdapter);

        mRecentlyPlayedTextView = v.findViewById(R.id.recently_played_text);
        mRecentlyPlayedRecyclerView = v.findViewById(R.id.recently_playlist);
        mRecentlyPlayedRecyclerView.setHasFixedSize(false);
        mRecentlyPlayedRecyclerView.setNestedScrollingEnabled(false);
        mRecentlyPlayedListAdapter = new LibraryCategoryVerticalListAdapter(this);

        ArrayList<String> keys = new ArrayList<String>();
        keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
        HashMap<String, List<ItemData>> result = new HashMap<>();
        result.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, mRecentlyPlayedList);

        mRecentlyPlayedListAdapter.setKeysArray(keys);
        mRecentlyPlayedListAdapter.setSectionsLists(result);
        mRecentlyPlayedRecyclerView.setAdapter(mRecentlyPlayedListAdapter);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        DropdownButton sourceSwitchButton = (DropdownButton) LayoutInflater.from(getContext()).inflate(
                R.layout.psa_view_source_switch_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_3),
                false);
        sourceSwitchButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.psa_media_source_usb, getActivity().getTheme()));
        mSourceSwitchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_3, sourceSwitchButton);
        getAppBarView().replaceAppBarButton(mSourceSwitchButton);
        sourceSwitchButton.setOnDropdownButtonClickEventListener(this);
        mSourceId = getSourceId();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveSourceId(mSourceId);
    }

    @Override
    void onUsbServiceReady(PSAUsbStateService usbNotificationService) {
        mUsbNotificationService = usbNotificationService;
        mUsbNotificationService.setUsbDeviceStateListener(this);
    }

    @Override
    public void onUsbDeviceStateChanged() {
        super.onUsbDeviceStateChanged();
        showVolumesDialogIfCan();
    }

    @Override
    public void onUsbDeviceRemoved(UsbDevice usbDevice) {
        if (mDropdownDialog != null) {
            if (!mUsbNotificationService.getUsbDevices().isEmpty()) {
                for (DropdownItem dropdownItem : mDropdownItems) {
                    if (usbDevice.getDeviceId().equals(dropdownItem.getId())) {
                        mDropdownDialog.removeDropdownItem(dropdownItem);
                        mDropdownItems.remove(dropdownItem);
                        break;
                    }
                }
            } else {
                mDropdownDialog.dismiss();
            }
        }
        if (usbDevice.getDeviceId().equals(mSourceId)) {
            final MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
            if (controls != null) {
                controls.stop();
            }
            selectFoldersAsMediaSource();
        }
    }

    private void showVolumesDialog(List<UsbDevice> usbDevices) {
        DropdownDialog.setDefaultColor(ResourcesCompat.getColor(getResources(), R.color.psa_dropdown_shadow_color,
                getActivity().getTheme()));
        DropdownDialog.setDefaultTextColor(Color.BLACK);
        if (mDropdownDialog == null) {
            mDropdownDialog = new DropdownDialog(mContext, DropdownDialog.HORIZONTAL, DropdownHelper.ItemType.ICON);
        } else {
            for (DropdownItem dropdownItem : mDropdownItems) {
                mDropdownDialog.removeDropdownItem(dropdownItem);
            }
        }
        mDropdownDialog.setColor(ResourcesCompat.getColor(getResources(), R.color.psa_general_background_color3,
                getActivity().getTheme()));
        mDropdownDialog.setTextColorRes(R.color.psa_dropdown_thumb_color);
        mDropdownItems.clear();
        for (int i = 0; i < usbDevices.size(); i++) {
            UsbDevice usbDevice = usbDevices.get(i);
            int icon = R.drawable.psa_media_source_usb;
            if (i == 0) {
                icon = R.drawable.psa_media_source_usb1;
            } else if (i == 1) {
                icon = R.drawable.psa_media_source_usb2;
            }
            DropdownItem dropdownItem = new DropdownItem(usbDevice.getDeviceId(), usbDevice.getName(), icon, DropdownHelper.ItemType.ICON);
            mDropdownDialog.addDropdownItem(dropdownItem);
            mDropdownItems.add(dropdownItem);
            if (usbDevice.getDeviceId().equals(mSourceId)) {
                dropdownItem.setSelected(true);
            }
        }
        mDropdownDialog.setOnActionItemClickListener(this);
        mDropdownDialog.setOnDismissListener(this);
        mDropdownDialog.show(mSourceSwitchButton.getAppBarButton(), DropdownHelper.Side.LEFT);
    }

    private void showVolumesDialogIfCan() {
        List<UsbDevice> usbDevices = mUsbNotificationService.getUsbDevices();
        if (usbDevices.isEmpty()) {
            if (mDropdownDialog != null) {
                mDropdownDialog.dismiss();
            }
        } else {
            showVolumesDialog(usbDevices);
        }
    }

    @Override
    public void onDismiss() {
        mDropdownDialog = null;
    }

    @Override
    public void onClick(DropdownButton view) {
        showVolumesDialogIfCan();
    }

    @Override
    public void onItemClick(DropdownItem item) {
        mDropdownDialog = null;
        item.setSelected(true);
        UsbDevice usbDevice = mUsbNotificationService.getUsbDeviceByDeviceId(item.getId());
        if (usbDevice != null) {
            mSourceId = usbDevice.getDeviceId();
            final MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
            if (controls != null) {
                controls.stop();
            }
            Bundle extras = new Bundle();
            extras.putStringArray("PATH", usbDevice.getVolumePaths());
            mMediaPlaybackModel.getMediaBrowser().subscribe("__USB__", extras, new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children, Bundle options) {
                    if (controls != null && !children.isEmpty()) {
                        MediaBrowser.MediaItem mediaItem = children.get(0);
                        controls.playFromMediaId(mediaItem.getMediaId(), mediaItem.getDescription().getExtras());
                    }
                }
            });
        }
    }

    /* Category */
    @Override
    public void onItemClicked(LibraryCategoryGridItemData data) {
        if (!mIsEditMode) {
            Log.d(TAG, "Category clicked " + data.getItemId());
            mMediaLibraryController.saveRootCategory(data.getItemId());

            String fragmentType = MediaLibraryFragment.FRAGMENT_TYPE_LIST;
            boolean playShuffleActionAvailable = false;
            String playShuffleActionText = null;
            String headerText = data.getPrimaryText();
            String subtitleText = null;
            String mediaId = data.getItemId();
            String rootCategory = data.getItemId();
            boolean isUsbDeviceRoot = false;
            boolean isUsbDeviceBrowsing = false;
            String usbDeviceId = null;
            if (data.getItemId().equals(MediaLibraryController.ALBUMS_ID)) {
                /* Albums showed in grid list*/
                fragmentType = MediaLibraryFragment.FRAGMENT_TYPE_GRID;
            } else if (MediaLibraryController.FOLDERS_ID.equals(data.getItemId())) {
                fragmentType = MediaLibraryFragment.FRAGMENT_TYPE_USB_SOURCES;
                isUsbDeviceBrowsing = true;
            } else {
                if (data.getItemId().equals(MediaLibraryController.ARTISTS_ID)) {
                    /* Shuffle all action is available for artists list*/
                    Bundle extras = data.getExtras();
                    if (extras.getBoolean(MediaLibraryController.PLAY_SHUFFLE_ACTION_KEY, false) == true) {
                        playShuffleActionAvailable = true;
                        playShuffleActionText = extras.getString(MediaLibraryController.PLAY_SHUFFLE_ACTION_TEXT_KEY,
                                getContext().getResources().getString(R.string.library_category_play_shuffle));
                    }
                }
            }

            getNavigationManager().showFragment(
                    MediaLibraryFragment.newCategoryInstance(fragmentType,
                            playShuffleActionAvailable,
                            playShuffleActionText,
                            headerText,
                            subtitleText,
                            mediaId,
                            rootCategory,
                            isUsbDeviceRoot,
                            isUsbDeviceBrowsing,
                            usbDeviceId));
        }
    }

    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        Log.d(TAG, "Root categories list updated.");
        mCategoriesList.clear();
        mCategoriesList.addAll(result);
        mAdapter.notifyDataSetChanged();
    }

    public void onItemsUpdated(List<ItemData> recentlyPlayedList, boolean showSections) {
        // recently played
        Log.d(TAG, "Recently played playlist updated");
        if (recentlyPlayedList.size() == 0) {
            Log.d(TAG, "List is empty - hide view");
            mRecentlyPlayedRecyclerView.setVisibility(View.GONE);
            mRecentlyPlayedTextView.setVisibility(View.GONE);
            return;
        }
        Log.d(TAG, "List size " + recentlyPlayedList.size());
        mRecentlyPlayedRecyclerView.setVisibility(View.VISIBLE);
        mRecentlyPlayedTextView.setVisibility(View.VISIBLE);

        ArrayList<String> keys = new ArrayList<String>();
        keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
        HashMap<String, List<ItemData>> result = new HashMap<>();
        result.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, recentlyPlayedList);

        mRecentlyPlayedListAdapter.setKeysArray(keys);
        mRecentlyPlayedListAdapter.setSectionsLists(result);
        mRecentlyPlayedListAdapter.notifyDataSetChanged();
    }


    /* Recently played list */
    @Override
    public void onItemClicked(ItemData data) {
        if (mIsEditMode) {
            mEdgeHandler.removeCallbacksAndMessages(null);
            sendEditModeAction(MediaConstants.EDGE_ACTION_PLAY_ITEM, mEdgePosition, "com.harman.psa.magic_touch_action_play_item",
                    "com.harman.psa.magic_touch_action_play_item_image", data);
            disableEditMode();
        } else {
            Bundle extras = data.getExtras();
            if (extras != null) {
                extras.putBoolean(MediaLibraryController.MAKE_LAST_PLAYED_PLAYLIST_QUEUE_KEY, true);
                mMediaLibraryController.playMediaItem(data);
            }
        }
    }

    @Override
    public void onActionClicked(ItemData data, int action) {
        mMediaLibraryController.fireAction(data, action, MediaLibraryController.RECENTLY_PLAYED_ID);
    }

    @Override
    public void onGeneralActionClicked(ItemData data) {
        //no use
    }

    private void selectFoldersAsMediaSource() {
        /***** Temporary *****/
        mSourceId = "2";
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            browser.subscribe("__FOLDERS__",
                    new MediaBrowser.SubscriptionCallback() {
                    });
        }
        /***** Temporary *****/
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

    private BroadcastReceiver mDisableEditModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            disableEditMode();
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

        mRecentlyPlayedListAdapter.setEditModeEnabled(true);

    }

    private void disableEditMode() {
        mIsEditMode = false;
        mRecentlyPlayedListAdapter.setEditModeEnabled(false);
        mEdgePosition = MediaConstants.UNDEFINED_EDGE_POSITION;

        mNavigationManager.setTabBarEnabled(true);
        ((MediaActivity) getHostActivity()).setEnabledAppBarButtons(true);
    }

    private void sendEditModeAction(String action, int position, String titleMetaName, String iconMetaName, ItemData item) {
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
            playIntentExtras = new Bundle(item.getExtras());
            playIntentExtras.putString(MediaConstants.MEDIA_ID_EXTRA_KEY, item.getId());
            data.putString(MediaConstants.CONTACT, item.getPrimaryText());
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

        getActivity().sendBroadcast(new Intent(MediaConstants.BROADCAST_MAGIC_TOUCH_EDIT_MODE));
    }

}