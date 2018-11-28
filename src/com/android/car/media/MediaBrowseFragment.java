package com.android.car.media;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.LibraryGridListAdapter;
import com.android.car.media.MediaLibraryController;
import com.android.car.media.MediaLibraryFragment;
import com.android.car.media.MediaPlaybackModel;
import android.widget.Toast;
import com.android.car.usb.PSAUsbStateService;
import com.android.car.usb.UsbDevice;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.dropdowns.DropdownButton;
import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownHelper;
import com.harman.psa.widget.dropdowns.DropdownItem;
import com.harman.psa.widget.dropdowns.listener.OnDismissListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownButtonClickEventListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownItemClickListener;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.graphics.PorterDuff;

import static android.content.Context.BIND_AUTO_CREATE;

public class MediaBrowseFragment extends MediaBaseFragment implements
        LibraryGridListAdapter.LibraryGridListItemClickListener, MediaLibraryController.ItemsUpdatedCallback,
        OnDropdownButtonClickEventListener, OnDropdownItemClickListener, PSAUsbStateService.UsbDeviceStateListener,
        OnDismissListener {
    private static final String TAG = "MediaBrowseFragment";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaLibraryController = new MediaLibraryController(mMediaPlaybackModel, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_browse_fragment, container, false);

        mRecyclerView = v.findViewById(R.id.library_grid);
        mRecyclerView.setHasFixedSize(true);

        mAdapter = new LibraryGridListAdapter(mCategoriesList, this);

        mRecyclerView.setAdapter(mAdapter);

        mMediaLibraryController.updateRootElements();


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
    public void onStop() {
        super.onStop();
        mMediaLibraryController.removeListener(this);
    }

    @Override
    void onUsbServiceReady(PSAUsbStateService usbNotificationService) {
        mUsbNotificationService = usbNotificationService;
        mUsbNotificationService.setUsbDeviceStateListener(this);
    }

    @Override
    public void onUsbDeviceStateChanged() {
        if (mUsbNotificationService.getUsbDeviceByDeviceId(mSourceId) == null) {
            final MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
            if (controls != null) {
                controls.stop();
            }
            selectFoldersAsMediaSource();
        }
        showVolumesDialogIfCan();
    }

    private void showVolumesDialog(List<UsbDevice> usbDevices) {
        DropdownDialog.setDefaultColor(ResourcesCompat.getColor(getResources(), R.color.psa_dropdown_shadow_color,
                getActivity().getTheme()));
        DropdownDialog.setDefaultTextColor(Color.BLACK);
        if (mDropdownDialog == null) {
            mDropdownDialog = new DropdownDialog(getActivity().getApplicationContext(), DropdownDialog.HORIZONTAL, DropdownHelper.ItemType.ICON);
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
            DropdownItem dropdownItem = new DropdownItem(usbDevice.getId(), usbDevice.getName(), icon, DropdownHelper.ItemType.ICON);
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
        UsbDevice usbDevice = mUsbNotificationService.getUsbDeviceById(item.getItemId());
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

    @Override
    public void onItemClicked(LibraryCategoryGridItemData data) {
        mMediaLibraryController.saveRootCategory(data.getItemId());
        Bundle fragmentExtra = new Bundle();
        fragmentExtra.putString(MediaLibraryFragment.LIST_HEADER_KEY, data.getPrimaryText());
        fragmentExtra.putString(MediaLibraryFragment.LIST_SUBTITLE_KEY, null);
        fragmentExtra.putString(MediaLibraryFragment.MEDIA_ID_KEY, data.getItemId());
        fragmentExtra.putString(MediaLibraryFragment.ROOT_CATEGORY_ID_KEY, data.getItemId());
        if (data.getItemId().equals(MediaLibraryController.ALBUMS_ID)) {
            fragmentExtra.putString(MediaLibraryFragment.FRAGMENT_TYPE_KEY, MediaLibraryFragment.FRAGMENT_TYPE_GRID);
            getNavigationManager().showFragment(
                    MediaLibraryFragment.newCategoryInstance(mMediaLibraryController, fragmentExtra));
        } else {
            if (data.getItemId().equals(MediaLibraryController.ARTISTS_ID)) {
                Bundle extras = data.getExtras();
                if (extras.getBoolean(MediaLibraryController.PLAY_SHUFFLE_ACTION_KEY, false) == true) {
                    fragmentExtra.putBoolean(MediaLibraryFragment.PLAY_SHUFFLE_ACTION_AVAILABILITY_KEY, true);
                    fragmentExtra.putString(MediaLibraryFragment.PLAY_SHUFFLE_ACTION_TEXT_KEY,
                            extras.getString(MediaLibraryController.PLAY_SHUFFLE_ACTION_TEXT_KEY,
                                    getContext().getResources().getString(R.string.library_category_play_shuffle)));
                }
            }
            fragmentExtra.putString(MediaLibraryFragment.FRAGMENT_TYPE_KEY, MediaLibraryFragment.FRAGMENT_TYPE_LIST);
            getNavigationManager().showFragment(
                    MediaLibraryFragment.newCategoryInstance(mMediaLibraryController, fragmentExtra));
        }
    }

    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        mCategoriesList.clear();
        mCategoriesList.addAll(result);
        mAdapter.notifyDataSetChanged();
    }

    public void onItemsUpdated(List<ItemData> result, boolean showSections) {
        // no use
        return;
    }

    private void selectFoldersAsMediaSource() {
        /***** Temporary *****/
        mSourceId = "5";
        mMediaPlaybackModel.getMediaBrowser().subscribe("__FOLDERS__",
                new MediaBrowser.SubscriptionCallback() {});
        /***** Temporary *****/
    }

}