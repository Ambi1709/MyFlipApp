package com.android.car.media;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.car.media.MediaLibraryController;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.List;


public class MediaLibraryFragment extends PSABaseFragment implements
        LibraryCategoryGridListAdapter.LibraryCategoryGridListItemClickListener,
        MediaLibraryController.ItemsUpdatedCallback {

    private static final String TAG = "MediaLibraryFragment";

    private static final String LIST_HEADER_KEY = "header";
    private static final String FRAGMENT_TYPE_KEY = "fragment_type";
    private static final String MEDIA_ID_KEY = "media_id";

    public static final String FRAGMENT_TYPE_GRID = "grid";
    public static final String FRAGMENT_TYPE_LIST = "list";

    private String mFragmentType = FRAGMENT_TYPE_LIST;
    private String mHeaderTitle = "";
    private String mHeaderSubtitle = null;
    private String mMediaId;

    private MediaLibraryController mLibraryController;

    private List<ItemData> mSubCategoriesList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    private MediaLibraryFragment(MediaLibraryController controller) {
        mLibraryController = controller;
        mLibraryController.setListener(this);
    }


    public static MediaLibraryFragment newCategoryInstance(MediaLibraryController controller,
                                                           String mediaId,
                                                           String header, String fragment_type) {
        MediaLibraryFragment fragment = new MediaLibraryFragment(controller);
        Bundle params = new Bundle();
        params.putString(LIST_HEADER_KEY, header);
        params.putString(FRAGMENT_TYPE_KEY, fragment_type);
        params.putString(MEDIA_ID_KEY, mediaId);
        fragment.setArguments(params);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v;
        mHeaderTitle = getArguments().getString(LIST_HEADER_KEY);
        mFragmentType = getArguments().getString(FRAGMENT_TYPE_KEY);
        mMediaId = getArguments().getString(MEDIA_ID_KEY);
        if (mFragmentType.equals(FRAGMENT_TYPE_GRID)) {
            v = inflater.inflate(R.layout.psa_media_library_category_grid_fragment, container, false);
            setUpGridListView(v);
        } else {
            v = inflater.inflate(R.layout.psa_media_library_category_list_fragment, container, false);
            setUpVerticalListView();
        }
        return v;
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

    }

    public void setUpGridListView(View v) {
        mRecyclerView = v.findViewById(R.id.category_grid);
        mRecyclerView.setHasFixedSize(true);
        mLibraryController.getChildrenElements(mMediaId);

        mAdapter = new LibraryCategoryGridListAdapter(mSubCategoriesList, this);

        mRecyclerView.setAdapter(mAdapter);
    }

    public void setUpVerticalListView() {

    }


    @Override
    public void onItemClicked(ItemData data) {
/*            getNavigationManager().showFragment(MediaLibraryFragment.newCategoryInstance(mLibraryController, 
                                    data.getId(), 
                                    data.getPrimaryText(), MediaLibraryFragment.FRAGMENT_TYPE_LIST));*/
    }

    public void onRootItemsUpdated(List<LibraryCategoryGridItemData> result) {
        // no use
    }

    public void onItemsUpdated(List<ItemData> result) {
        mSubCategoriesList.clear();
        mSubCategoriesList.addAll(result);
        mAdapter.notifyDataSetChanged();
    }

}