package com.android.car.media;


import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.media.LibraryCategoryVerticalListAdapter;
import com.android.car.media.MediaLibraryController;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MediaLibraryFragment extends PSABaseFragment implements
        LibraryCategoryGridListAdapter.LibraryCategoryGridListItemClickListener,
        LibraryCategoryVerticalListAdapter.OnItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback {

    private static final String TAG = "MediaLibraryFragment";

    private static final String LIST_HEADER_KEY = "header";
    private static final String LIST_SUBTITLE_KEY = "subtitle";
    private static final String FRAGMENT_TYPE_KEY = "fragment_type";
    private static final String MEDIA_ID_KEY = "media_id";
    private static final String ROOT_CATEGORY_ID_KEY = "root_category_id";

    public static final String FRAGMENT_TYPE_GRID = "grid";
    public static final String FRAGMENT_TYPE_LIST = "list";

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

    private MediaLibraryFragment(MediaLibraryController controller) {
        mLibraryController = controller;
    }

    public static MediaLibraryFragment newCategoryInstance(MediaLibraryController controller,
                                                           String mediaId,
                                                           String header,
                                                           @Nullable String subtitle,
                                                           String fragment_type,
                                                           @NonNull String rootCategoryId) {
        MediaLibraryFragment fragment = new MediaLibraryFragment(controller);
        Bundle params = new Bundle();
        params.putString(LIST_HEADER_KEY, header);
        params.putString(LIST_SUBTITLE_KEY, subtitle);
        params.putString(FRAGMENT_TYPE_KEY, fragment_type);
        params.putString(MEDIA_ID_KEY, mediaId);
        params.putString(ROOT_CATEGORY_ID_KEY, rootCategoryId);
        fragment.setArguments(params);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v;
        mHeaderTitle = getArguments().getString(LIST_HEADER_KEY);
        mHeaderSubtitle = getArguments().getString(LIST_SUBTITLE_KEY);
        mFragmentType = getArguments().getString(FRAGMENT_TYPE_KEY);
        mMediaId = getArguments().getString(MEDIA_ID_KEY);
        mRootCategoryId = getArguments().getString(ROOT_CATEGORY_ID_KEY);
        if (mFragmentType.equals(FRAGMENT_TYPE_GRID)) {
            v = inflater.inflate(R.layout.psa_media_library_category_grid_fragment, container, false);
            setUpGridListView(v);
        } else {
            v = inflater.inflate(R.layout.psa_media_library_category_list_fragment, container, false);
            setUpVerticalListView(v);
        }
        mSubtitleView = (TextView) v.findViewById(R.id.category_subtitle);
        return v;
    }

    @Override
    public void onStop() {
        super.onStop();
        mLibraryController.removeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        mLibraryController.addListener(this);
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

    public void setUpGridListView(View v) {
        mRecyclerView = v.findViewById(R.id.category_grid);
        mRecyclerView.setHasFixedSize(true);
        mLibraryController.getChildrenElements(mMediaId);

        mAdapter = new LibraryCategoryGridListAdapter(mSubCategoriesList, this);

        mRecyclerView.setAdapter(mAdapter);
    }

    public void setUpVerticalListView(View v) {
        mRecyclerView = v.findViewById(R.id.list);
        mRecyclerView.setHasFixedSize(true);
        mLibraryController.getChildrenElements(mMediaId);

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
                getNavigationManager().showFragment(MediaLibraryFragment.newCategoryInstance(mLibraryController,
                        data.getId(),
                        data.getPrimaryText(),
                        data.getSecondaryText(), MediaLibraryFragment.FRAGMENT_TYPE_LIST,
                        mRootCategoryId));
            } else {
                mLibraryController.playMediaItem(data);
            }
        }

    }

    @Override
    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        // no use
    }

    public void onItemsUpdated(List<ItemData> result, boolean showSections) {
        if (mAdapter != null) {
            mSubCategoriesList.clear();
            mSubCategoriesList.addAll(result);
            mAdapter.notifyDataSetChanged();
        }
        if (mListAdapter != null) {
            if (mRootCategoryId.equals(MediaLibraryController.ALBUMS_ID)) { // various artists
                if (checkDiffSubTitles(result)) {
                    String subtitle = mLibraryController.getVariousSubtitle(getContext());
                    if (subtitle != null && mSubtitleView != null) {
                        mSubtitleView.setText(subtitle);
                    }
                } else {
                    mListAdapter.hideSubtitles(true);
                }
            }
            mSubCategoriesList.clear();
            mSubCategoriesList.addAll(result);
            ArrayList<String> keys = new ArrayList<String>();
            keys.add(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION);
            mListAdapter.setKeysArray(keys);
            HashMap<String, List<ItemData>> map = new HashMap<>();
            map.put(LibraryCategoryVerticalListAdapter.DEFAULT_SECTION, mSubCategoriesList);
            mListAdapter.setSectionsLists(map);
            mListAdapter.notifyDataSetChanged();
        }
    }

    private boolean checkDiffSubTitles(List<ItemData> items) {
        if (items.size() == 1) return false;
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

}