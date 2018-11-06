package com.android.car.media;


import android.graphics.drawable.Drawable;
import android.media.browse.MediaBrowser;
import android.util.Log;

import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.MediaPlaybackModel;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.List;

public class MediaLibraryController {
    private static final String TAG = "MediaLibraryController";

    private MediaPlaybackModel mMediaPlaybackModel;
    private ItemsUpdatedCallback mItemsChangedListener;
    
    interface ItemsUpdatedCallback {
        void onItemsUpdated(List<ItemData> result);

        void onRootItemsUpdated(List<LibraryCategoryGridItemData> result);
    }

    public MediaLibraryController(MediaPlaybackModel model, ItemsUpdatedCallback listener) {
        mMediaPlaybackModel = model;
        mItemsChangedListener = listener;
    }

    public void setListener(ItemsUpdatedCallback listener) {
        mItemsChangedListener = listener;
    }

    public void getChildrenElements(String parentId) {
        mMediaPlaybackModel.getMediaBrowser().subscribe(parentId, new MediaBrowser.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                if (children.size() > 0) {
                    List<ItemData> result = new ArrayList<>();
                    for (MediaBrowser.MediaItem item : children) {
                        ItemData model = new ItemData.Builder()
                                .setId(item.getMediaId())
                                .setPrimaryText(item.getDescription().getTitle().toString())
                                .setSecondaryText(item.getDescription().getSubtitle().toString())
                                .setAction1ResId(R.drawable.media_library_album_default_art)
                                .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                                .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW)
                                .build();
                        model.setAction1DrawableUri(item.getDescription().getIconUri());
                        result.add(model);
                    }
                    mMediaPlaybackModel.getMediaBrowser().unsubscribe(parentId);
                    mItemsChangedListener.onItemsUpdated(result);
                }
            }

            @Override
            public void onError(String parentId) {
                Log.e(TAG, "Error loading children of " + parentId);
            }
        });
    }

    public void updateRootElements() {
        String rootId = mMediaPlaybackModel.getMediaBrowser().getRoot();
        if (rootId != null && rootId.length() != 0) {
            mMediaPlaybackModel.getMediaBrowser().subscribe(rootId, new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                    List<LibraryCategoryGridItemData> result = new ArrayList<>();
                    if (children.size() > 0) {
                        for (MediaBrowser.MediaItem item : children) {
                            LibraryCategoryGridItemData model =
                                    new LibraryCategoryGridItemData(item.getMediaId(),
                                            item.getDescription().getTitle().toString());
                            model.setImageUri(item.getDescription().getIconUri());
                            result.add(model);
                        }

                    }
                    mMediaPlaybackModel.getMediaBrowser().unsubscribe(rootId);
                    mItemsChangedListener.onRootItemsUpdated(result);
                }

                @Override
                public void onError(String parentId) {
                    Log.e(TAG, "Error loading children of " + parentId);
                }
            });
        }

    }

}