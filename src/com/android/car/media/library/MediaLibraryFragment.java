package com.android.car.media;


import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.usb.PSAUsbStateService;
import com.android.car.usb.UsbDevice;
import com.android.car.usb.UsbVolume;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import static com.android.car.media.MediaLibraryController.FOLDERS_ID;
import static com.android.car.media.MediaLibraryController.MEDIA_ITEM_TYPE_KEY;

public class MediaLibraryFragment extends MediaBaseFragment implements
        LibraryCategoryGridListAdapter.LibraryCategoryGridListItemClickListener,
        LibraryCategoryVerticalListAdapter.OnItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback, PSAUsbStateService.UsbDeviceStateListener {

    private static final String TAG = "MediaLibraryFragment";

    public static final String LIST_HEADER_KEY = "header";
    public static final String LIST_SUBTITLE_KEY = "subtitle";
    public static final String FRAGMENT_TYPE_KEY = "fragment_type";
    public static final String MEDIA_ID_KEY = "media_id";
    public static final String ROOT_CATEGORY_ID_KEY = "root_category_id";
    public static final String PLAY_SHUFFLE_ACTION_AVAILABILITY_KEY = "play_shuffle_action_available";
    public static final String PLAY_SHUFFLE_ACTION_TEXT_KEY = "play_shuffle_action_text";

    public static final String FRAGMENT_TYPE_GRID = "grid";
    public static final String FRAGMENT_TYPE_LIST = "list";

    public static final String FRAGMENT_TYPE_USB_SOURCES = "usbSources";

    private static final String USB_DEVICE_ID = "usbDeviceId";

    private static final String IS_USB_DEVICE_ROOT = "isUsbDeviceRoot";

    public static final String IS_USB_DEVICE_BROWSING = "isUsbDeviceBrowsing";

    private String mFragmentType = FRAGMENT_TYPE_LIST;
    private String mHeaderTitle = "";
    private String mHeaderSubtitle = null;
    private String mMediaId;
    private String mRootCategoryId;

    private MediaLibraryController mLibraryController;

    private List<ItemData> mSubCategoriesList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private LibraryCategoryVerticalListAdapter mListAdapter;

    private TextView mSubtitleView;
    private ItemData mShuffleActionItemData;

    private PSAUsbStateService mUsbStateService;

    private String mUsbSourceId;

    private boolean mIsUsbDeviceRoot;

    private boolean mIsUsbDeviceBrowsing;

    public static MediaLibraryFragment newCategoryInstance(String fragmentType,
                                                           boolean playShuffleActionAvailable,
                                                           @Nullable String playShuffleActionText,
                                                           String headerText,
                                                           String subtitleText,
                                                           String mediaId,
                                                           String rootCategory,
                                                           boolean isUsbDeviceRoot,
                                                           boolean isUsbDeviceBrowsing,
                                                           String usbDeviceId) {
        MediaLibraryFragment fragment = new MediaLibraryFragment();

        Bundle fragmentExtra = new Bundle();
        if (playShuffleActionAvailable) {
            fragmentExtra.putBoolean(PLAY_SHUFFLE_ACTION_AVAILABILITY_KEY, true);
            fragmentExtra.putString(PLAY_SHUFFLE_ACTION_TEXT_KEY, playShuffleActionText);
        }
        fragmentExtra.putString(USB_DEVICE_ID, usbDeviceId);
        fragmentExtra.putString(LIST_HEADER_KEY, headerText);
        fragmentExtra.putString(LIST_SUBTITLE_KEY, subtitleText);
        fragmentExtra.putString(FRAGMENT_TYPE_KEY, fragmentType);
        fragmentExtra.putString(MEDIA_ID_KEY, mediaId);
        fragmentExtra.putString(ROOT_CATEGORY_ID_KEY, rootCategory);
        fragmentExtra.putBoolean(IS_USB_DEVICE_ROOT, isUsbDeviceRoot);
        fragmentExtra.putBoolean(IS_USB_DEVICE_BROWSING, isUsbDeviceBrowsing);

        fragment.setArguments(fragmentExtra);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getView();
        mHeaderTitle = getArguments().getString(LIST_HEADER_KEY);
        mHeaderSubtitle = getArguments().getString(LIST_SUBTITLE_KEY);
        mFragmentType = getArguments().getString(FRAGMENT_TYPE_KEY);
        mMediaId = getArguments().getString(MEDIA_ID_KEY);
        mRootCategoryId = getArguments().getString(ROOT_CATEGORY_ID_KEY);
        mIsUsbDeviceRoot = getArguments().getBoolean(IS_USB_DEVICE_ROOT, false);
        mIsUsbDeviceBrowsing = getArguments().getBoolean(IS_USB_DEVICE_BROWSING, false);
        if (mFragmentType.equals(FRAGMENT_TYPE_GRID)) {
            if (v == null) {
                v = inflater.inflate(R.layout.psa_media_library_category_grid_fragment, container, false);
            }
        } else {
            if (v == null) {
                v = inflater.inflate(R.layout.psa_media_library_category_list_fragment, container, false);
            }
        }
        mSubtitleView = (TextView) v.findViewById(R.id.category_subtitle);
        mUsbSourceId = getSourceId();
        return v;
    }

    @Override
    public void onStop() {
        super.onStop();
        mLibraryController.unsubscribe(mMediaId);
        mLibraryController.removeListener(this);
        if (!FRAGMENT_TYPE_USB_SOURCES.equals(mFragmentType)) {
            mLibraryController.removeListener(this);
        }
        if (mListAdapter != null) {
            mListAdapter.dismissDialog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!FRAGMENT_TYPE_USB_SOURCES.equals(mFragmentType)) {
            mLibraryController.addListener(this);
        }
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getNavigationManager().backToPrevScreen();
            }
        });
        TextView titleView = (TextView) view.findViewById(R.id.category_title);
        titleView.setText(mHeaderTitle);
        if (mHeaderSubtitle != null) {
            mSubtitleView.setVisibility(View.VISIBLE);
            mSubtitleView.setText(mHeaderSubtitle);
        }

    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        mLibraryController = ((MediaActivity) getHostActivity()).getLibraryController();
        if (mFragmentType.equals(FRAGMENT_TYPE_GRID)) {
            setUpGridListView(getView());
        } else {
            setUpVerticalListView(getView());
        }
    }

    public void setUpGridListView(View v) {
        mRecyclerView = v.findViewById(R.id.category_grid);
        mRecyclerView.setHasFixedSize(true);
        mAdapter = new LibraryCategoryGridListAdapter(mSubCategoriesList, this);

        mRecyclerView.setAdapter(mAdapter);
        mLibraryController.getChildrenElements(mMediaId);
    }

    public void setUpVerticalListView(View v) {
        if (getArguments().getBoolean(PLAY_SHUFFLE_ACTION_AVAILABILITY_KEY) == true && mShuffleActionItemData == null) {
            // set up shuffle play action (for categories)
            String shuffleActionText = getArguments().getString(PLAY_SHUFFLE_ACTION_TEXT_KEY);
            Bundle extras = new Bundle();
            extras.putInt(MediaLibraryController.MEDIA_ITEM_TYPE_KEY, MediaBrowser.MediaItem.FLAG_BROWSABLE);
            mShuffleActionItemData = new ItemData.Builder()
                    .setId(mMediaId)
                    .setPrimaryText(shuffleActionText)
                    .setAction1ResId(R.drawable.psa_media_button_icon_shuffle_on)
                    .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW)
                    .build();
            mShuffleActionItemData.setExtras(extras);
            mSubCategoriesList.add(mShuffleActionItemData);
        }

        mRecyclerView = v.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);

        if (mIsUsbDeviceBrowsing && !mIsUsbDeviceRoot) {
            mLibraryController.getFolderContent(mMediaId);
        } else if (!mIsUsbDeviceRoot) {
            mLibraryController.getChildrenElements(mMediaId);
        }

        mListAdapter = new LibraryCategoryVerticalListAdapter(this);
        ArrayList<String> keys = new ArrayList<String>();
        keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
        mListAdapter.setKeysArray(keys);
        HashMap<String, List<ItemData>> result = new HashMap<>();
        result.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, mSubCategoriesList);
        mListAdapter.setSectionsLists(result);

        mRecyclerView.setAdapter(mListAdapter);
    }


    @Override
    public void onItemClicked(ItemData data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            if (extras.getInt(MediaLibraryController.MEDIA_ITEM_TYPE_KEY) == MediaBrowser.MediaItem.FLAG_BROWSABLE) {
                getNavigationManager().showFragment(MediaLibraryFragment.newCategoryInstance(
                        MediaLibraryFragment.FRAGMENT_TYPE_LIST,
                        extras.getBoolean(MediaLibraryController.PLAY_SHUFFLE_ACTION_KEY, false),
                        extras.getString(MediaLibraryController.PLAY_SHUFFLE_ACTION_TEXT_KEY,
                                getContext().getResources().getString(R.string.library_category_play_shuffle)),
                        data.getPrimaryText(),
                        data.getSecondaryText(),
                        data.getId(),
                        mRootCategoryId,
                        extras.getBoolean(IS_USB_DEVICE_ROOT, false),
                        mIsUsbDeviceBrowsing,
                        mUsbSourceId));
            } else {
                mLibraryController.playMediaItem(data);
            }
        }

    }

    @Override
    public void onActionClicked(ItemData data, int action) {
        mLibraryController.fireAction(data, action, mRootCategoryId);
    }

    @Override
    public void onGeneralActionClicked(ItemData data) {
        mLibraryController.fireAction(data, MediaLibraryController.PLAY_SHUFFLE_ACTION_INDEX, mRootCategoryId);
    }

    @Override
    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        // no use
    }

    public void onItemsUpdated(List<ItemData> result, boolean showSections) {
        if (mAdapter != null) { // grid list
            mSubCategoriesList.clear();
            mSubCategoriesList.addAll(result);
            mAdapter.notifyDataSetChanged();
        }
        if (mListAdapter != null) { // vertical list
            if (mRootCategoryId.equals(MediaLibraryController.ALBUMS_ID) ||
                    mRootCategoryId.equals(MediaLibraryController.ARTISTS_ID)) {
                if (checkDiffSubTitles(result)) {
                    String subtitle = mLibraryController.getVariousSubtitle(getContext());
                    if (subtitle != null && mSubtitleView != null) {
                        mSubtitleView.setText(subtitle);
                    }
                } else {
                    mListAdapter.hideSubtitles(true);
                }
            }
            ArrayList<String> keys = new ArrayList<String>();
            HashMap<String, List<ItemData>> map = new HashMap<>();
            if (showSections) {
                mSubCategoriesList.clear();
                if (mShuffleActionItemData != null) { // shuffle play action for categories
                    mSubCategoriesList.add(mShuffleActionItemData);
                }
                keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
                map.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, mSubCategoriesList);
                prepareSectionsList(result, keys, map);
            } else {
                mSubCategoriesList.clear();
                if (mShuffleActionItemData != null) {
                    mSubCategoriesList.add(mShuffleActionItemData);
                }
                mSubCategoriesList.addAll(result);
                keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
                map.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, mSubCategoriesList);
            }
            mListAdapter.setKeysArray(keys);
            mListAdapter.setSectionsLists(map);
            mListAdapter.notifyDataSetChanged();
        }
    }

    /* Method to prepare index sections */
    private void prepareSectionsList(List<ItemData> result,
                                     ArrayList<String> keys, HashMap<String,
            List<ItemData>> map) {
        TreeSet<String> set = new TreeSet<>();
        for (ItemData item : result) {
            char index = item.getPrimaryText().charAt(0);
            char[] indexArr = {index};
            String indexStr = new String(indexArr);
            set.add(indexStr);
        }
        keys.addAll(set);
        for (String key : keys) {
            if (!map.containsKey(key)) {
                List<ItemData> indexItems = new ArrayList<>();
                for (ItemData item : result) {
                    if (item.getPrimaryText().charAt(0) == key.charAt(0)) {
                        indexItems.add(item);
                    }
                }
                map.put(key, indexItems);
            }
        }

    }

    private boolean checkDiffSubTitles(List<ItemData> items) {
        if (items == null || items.size() <= 1) return false;
        boolean result = false;
        String subtitle = items.get(0).getSecondaryText();
        for (ItemData item : items) {
            if (item.getSecondaryText() != null && !item.getSecondaryText().equals(subtitle)) {
                result = true;
                break;
            }
        }
        return result;
    }

    @Override
    void onUsbServiceReady(PSAUsbStateService usbNotificationService) {
        mUsbStateService = usbNotificationService;
        mUsbStateService.setUsbDeviceStateListener(this);
        if (FRAGMENT_TYPE_USB_SOURCES.equals(mFragmentType)) {
            showUsbDevices(mUsbStateService.getUsbDevices());
        } else if (mIsUsbDeviceRoot) {
            showUsbVolumes(mUsbStateService.getUsbDeviceByDeviceId(mMediaId));
        }
    }

    @Override
    public void onUsbDeviceStateChanged() {
        super.onUsbDeviceStateChanged();
        Log.d(TAG, "onUsbDeviceStateChanged");
        if (FRAGMENT_TYPE_USB_SOURCES.equals(mFragmentType)) {
            showUsbDevices(mUsbStateService.getUsbDevices());
        }
    }

    @Override
    public void onUsbDeviceRemoved(UsbDevice usbDevice) {
        if (!TextUtils.isEmpty(mUsbSourceId) && mUsbSourceId.equals(usbDevice.getDeviceId())) {
            FragmentManager fm = getFragmentManager();
            int count = fm.getBackStackEntryCount();
            if(FOLDERS_ID.equals(mRootCategoryId)) {
                int tillRootPage = 1;
                int tillUsbSourcesPage = 2;
                int till = FOLDERS_ID.equals(mRootCategoryId) ? tillUsbSourcesPage : tillRootPage;
                for (int i = count - 1; i >= till; i--) {
                    fm.popBackStackImmediate();
                }
            }
        } else if (FRAGMENT_TYPE_USB_SOURCES.equals(mFragmentType)) {
            showUsbDevices(mUsbStateService.getUsbDevices());
        }
    }

    private void showUsbDevices(List<UsbDevice> usbDevices) {
        List<ItemData> items = new ArrayList<>();
        for (int i = 0; i < usbDevices.size(); i++) {
            UsbDevice usbDevice = usbDevices.get(i);
            ItemData.Builder builder = new ItemData.Builder()
                    .setId(usbDevice.getDeviceId())
                    .setPrimaryText(usbDevice.getName())
                    .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                    .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW)
                    .setAction2ResId(-2);

            if (i == 0) {
                builder.setAction1ResId(R.drawable.psa_media_source_usb1);
            } else if (i == 1) {
                builder.setAction1ResId(R.drawable.psa_media_source_usb2);
            } else {
                builder.setAction1ResId(R.drawable.psa_media_source_usb);
            }

            ItemData model = builder.build();
            Bundle extras = new Bundle();
            extras.putInt(MEDIA_ITEM_TYPE_KEY, MediaBrowser.MediaItem.FLAG_BROWSABLE);
            extras.putBoolean(IS_USB_DEVICE_ROOT, true);
            model.setExtras(extras);
            items.add(model);
        }
        onItemsUpdated(items, false);
    }

    private void showUsbVolumes(UsbDevice usbDevice) {
        if (usbDevice != null) {
            if (usbDevice.getVolumes().size() > 1) {
                List<ItemData> items = new ArrayList<>();
                for (UsbVolume usbVolume : usbDevice.getVolumes()) {
                    ItemData.Builder builder = new ItemData.Builder()
                            .setId(usbVolume.getPath() + File.separator)
                            .setPrimaryText(usbVolume.getId())
                            .setAction1ResId(R.drawable.media_library_album_default_art)
                            .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                            .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW)
                            .setAction2ResId(-2);
                    builder.setSecondaryText(usbVolume.getDescr());
                    ItemData model = builder.build();
                    Bundle extras = new Bundle();
                    extras.putInt(MEDIA_ITEM_TYPE_KEY, MediaBrowser.MediaItem.FLAG_BROWSABLE);
                    model.setExtras(extras);
                    items.add(model);
                }
                onItemsUpdated(items, false);
            } else {
                mFragmentType = FRAGMENT_TYPE_LIST;
                mLibraryController.addListener(this);
                mMediaId = usbDevice.getVolumePaths()[0] + File.separator;
                mLibraryController.getFolderContent(mMediaId);
            }
        }
    }
}