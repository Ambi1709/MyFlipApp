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
import java.util.TreeSet;


public class MediaLibraryFragment extends PSABaseFragment implements
        LibraryCategoryGridListAdapter.LibraryCategoryGridListItemClickListener,
        LibraryCategoryVerticalListAdapter.OnItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback {

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

    private MediaLibraryFragment(MediaLibraryController controller) {
        mLibraryController = controller;
    }

    public static MediaLibraryFragment newCategoryInstance(MediaLibraryController controller,
                                                           Bundle extras) {
        MediaLibraryFragment fragment = new MediaLibraryFragment(controller);
        fragment.setArguments(extras);
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
        if (mListAdapter != null) {
            mListAdapter.dismissDialog();
        }
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
                Bundle fragmentExtra = new Bundle();
                if (extras.getBoolean(MediaLibraryController.PLAY_SHUFFLE_ACTION_KEY, false) == true) {
                    fragmentExtra.putBoolean(PLAY_SHUFFLE_ACTION_AVAILABILITY_KEY, true);
                    fragmentExtra.putString(PLAY_SHUFFLE_ACTION_TEXT_KEY,
                            extras.getString(MediaLibraryController.PLAY_SHUFFLE_ACTION_TEXT_KEY,
                                    getContext().getResources().getString(R.string.library_category_play_shuffle)));
                }
                fragmentExtra.putString(LIST_HEADER_KEY, data.getPrimaryText());
                fragmentExtra.putString(LIST_SUBTITLE_KEY, data.getSecondaryText());
                fragmentExtra.putString(FRAGMENT_TYPE_KEY, MediaLibraryFragment.FRAGMENT_TYPE_LIST);
                fragmentExtra.putString(MEDIA_ID_KEY, data.getId());
                fragmentExtra.putString(ROOT_CATEGORY_ID_KEY, mRootCategoryId);
                getNavigationManager().showFragment(MediaLibraryFragment.newCategoryInstance(mLibraryController, fragmentExtra));
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