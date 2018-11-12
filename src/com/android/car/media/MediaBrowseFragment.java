package com.android.car.media;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.LibraryGridListAdapter;
import com.android.car.media.MediaLibraryController;
import com.android.car.media.MediaLibraryFragment;
import com.android.car.media.MediaPlaybackModel;
import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.dropdowns.DropdownButton;
import com.harman.psa.widget.dropdowns.DropdownDialog;
import com.harman.psa.widget.dropdowns.DropdownHelper;
import com.harman.psa.widget.dropdowns.DropdownItem;
import com.harman.psa.widget.dropdowns.listener.OnDismissListener;
import com.harman.psa.widget.dropdowns.listener.OnDropdownButtonClickEventListener;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.List;

public class MediaBrowseFragment extends PSABaseFragment implements
        LibraryGridListAdapter.LibraryGridListItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback {
    private static final String TAG = "MediaBrowseFragment";

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private MediaPlaybackModel mMediaPlaybackModel;
    private MediaLibraryController mMediaLibraryController;

    private PSAAppBarButton mUsbSwitchButton;

    private String mRootId = "";

    private List<LibraryCategoryGridItemData> mCategoriesList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaLibraryController = new MediaLibraryController(mMediaPlaybackModel, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMediaLibraryController.removeListener(this);
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
        DropdownButton usbSwitchButton = (DropdownButton) LayoutInflater.from(getContext()).inflate(
                R.layout.psa_view_usb_switch_button,
                getAppBarView().getContainerForPosition(PSAAppBarButton.Position.LEFT_SIDE_3),
                false);
        mUsbSwitchButton = new PSAAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_3, usbSwitchButton);
        getAppBarView().replaceAppBarButton(mUsbSwitchButton);
        usbSwitchButton.setOnDropdownButtonClickEventListener(mUsbButtonClickListener);
    }

    private final OnDropdownButtonClickEventListener mUsbButtonClickListener = new OnDropdownButtonClickEventListener() {
        @Override
        public void onClick(DropdownButton view) {
            DropdownDialog.setDefaultColor(ResourcesCompat.getColor(getResources(), R.color.psa_dropdown_shadow_color,
                    getActivity().getTheme()));
            DropdownDialog.setDefaultTextColor(Color.BLACK);

            DropdownDialog mDropdownDialog = new DropdownDialog(getActivity().getApplicationContext(), DropdownDialog.HORIZONTAL, DropdownHelper.ItemType.ICON);
            mDropdownDialog.setColor(ResourcesCompat.getColor(getResources(), R.color.psa_general_background_color3,
                    getActivity().getTheme()));
            mDropdownDialog.setTextColorRes(R.color.psa_dropdown_thumb_color);

            mDropdownDialog.addDropdownItem(new DropdownItem(1, "", R.drawable.psa_media_source_usb1, DropdownHelper.ItemType.ICON));
            mDropdownDialog.addDropdownItem(new DropdownItem(2, "", R.drawable.psa_media_source_usb2, DropdownHelper.ItemType.ICON));

            mDropdownDialog.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss() {
                }
            });
            mDropdownDialog.show(view, DropdownHelper.Side.LEFT);
        }
    };

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


}