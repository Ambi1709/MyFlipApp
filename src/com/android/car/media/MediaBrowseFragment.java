package com.android.car.media;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.LibraryGridListAdapter;
import com.android.car.media.MediaLibraryController;
import com.android.car.media.MediaLibraryFragment;
import com.android.car.media.MediaPlaybackModel;
import com.harman.psa.widget.PSABaseFragment;
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

    private String mRootId = "";

    private List<LibraryCategoryGridItemData> mCategoriesList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_browse_fragment, container, false);

        mRecyclerView = v.findViewById(R.id.library_grid);
        mRecyclerView.setHasFixedSize(true);

        mMediaPlaybackModel = ((MediaActivity) getHostActivity()).getPlaybackModel();
        mMediaLibraryController = new MediaLibraryController(mMediaPlaybackModel, this);

        mMediaLibraryController.updateRootElements();

        mAdapter = new LibraryGridListAdapter(mCategoriesList, this);

        mRecyclerView.setAdapter(mAdapter);


        return v;
    }

    @Override
    public void onItemClicked(LibraryCategoryGridItemData data) {
        if (data.getItemId().equals("__ALBUMS__")) {
            getNavigationManager().showFragment(MediaLibraryFragment.newCategoryInstance(mMediaLibraryController,
                    data.getItemId(),
                    data.getPrimaryText(), MediaLibraryFragment.FRAGMENT_TYPE_GRID));
        }
    }

    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        mCategoriesList.clear();
        mCategoriesList.addAll(result);
        mAdapter.notifyDataSetChanged();
    }

    public void onItemsUpdated(List<ItemData> result) {
        // no use
        return;
    }


}