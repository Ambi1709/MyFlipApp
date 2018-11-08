package com.android.car.media;


import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.car.apps.common.util.Assert;
import com.android.car.media.LibraryCategoryGridItemData;
import com.android.car.media.MediaPlaybackModel;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class MediaLibraryController {
    private static final String TAG = "MediaLibraryController";

    public static final String MEDIA_ITEM_TYPE_KEY = "key";
    public static final String SHOW_INDEX_KEY = "SHOW_INDEX";

    static final String ALBUMS_ID = "__ALBUMS__";

    private MediaPlaybackModel mMediaPlaybackModel;

    private final List<ItemsUpdatedCallback> mItemsChangedListeners = new LinkedList<>();

    private static String mRootCategory;

    interface ItemsUpdatedCallback {
        void onItemsUpdated(List<ItemData> result, boolean showSections);

        void onRootItemsUpdated(List<LibraryCategoryGridItemData> result);
    }

    public MediaLibraryController(MediaPlaybackModel model, ItemsUpdatedCallback listener) {
        mMediaPlaybackModel = model;
        addListener(listener);
    }

    @MainThread
    public void addListener(ItemsUpdatedCallback listener) {
        Assert.isMainThread();
        mItemsChangedListeners.add(listener);
    }

    @MainThread
    public void removeListener(ItemsUpdatedCallback listener) {
        Assert.isMainThread();
        mItemsChangedListeners.remove(listener);
    }

    @MainThread
    private void notifyListeners(Consumer<ItemsUpdatedCallback> callback) {
        Assert.isMainThread();
        // Clone mListeners in case any of the callbacks made triggers a listener to be added or
        // removed to/from mListeners.
        List<ItemsUpdatedCallback> listenersCopy = new LinkedList<>(mItemsChangedListeners);
        // Invokes callback.accept(listener) for each listener.
        listenersCopy.forEach(callback);
    }

    public void getChildrenElements(String parentId) {
        mMediaPlaybackModel.getMediaBrowser().subscribe(parentId, new MediaBrowser.SubscriptionCallback() {
            @Override
            public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "onChildrenLoaded " + parentId);
                }
                if (children.size() > 0) {
                    List<Boolean> showSections = new LinkedList<>();
                    showSections.add(false);
                    List<ItemData> result = new ArrayList<>();
                    for (MediaBrowser.MediaItem item : children) {
                        ItemData.Builder builder = new ItemData.Builder()
                                .setId(item.getMediaId())
                                .setPrimaryText(item.getDescription().getTitle().toString())
                                .setAction1ResId(R.drawable.media_library_album_default_art)
                                .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                                .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW);
                        if (item.getDescription().getSubtitle() != null) {
                            builder.setSecondaryText(item.getDescription().getSubtitle().toString());
                        }
                        Bundle itemExtras = item.getDescription().getExtras();
                        if (itemExtras != null) {
                            showSections.set(0, itemExtras.getBoolean(SHOW_INDEX_KEY, false));
                        }
                        ItemData model = builder.build();
                        model.setAction1DrawableUri(item.getDescription().getIconUri());
                        Bundle extras = new Bundle();
                        extras.putInt(MEDIA_ITEM_TYPE_KEY, (item.isPlayable() ? MediaBrowser.MediaItem.FLAG_PLAYABLE : MediaBrowser.MediaItem.FLAG_BROWSABLE));
                        model.setExtras(extras);
                        result.add(model);
                    }
                    mMediaPlaybackModel.getMediaBrowser().unsubscribe(parentId);
                    notifyListeners((listener) -> listener.onItemsUpdated(result, showSections.get(0)));
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
                    notifyListeners((listener) -> listener.onRootItemsUpdated(result));
                }

                @Override
                public void onError(String parentId) {
                    Log.e(TAG, "Error loading children of " + parentId);
                }
            });
        }

    }

    public void saveRootCategory(String category) {
        mRootCategory = category;
    }

    @Nullable
    public String getVariousSubtitle(Context context) {
        String result = null;
        if (mRootCategory != null && mRootCategory.equals(ALBUMS_ID)) {
            result = context.getResources().getString(R.string.various_artists);
        }
        return result;
    }

    public void playMediaItem(ItemData data) {
        MediaController.TransportControls controls = mMediaPlaybackModel.getTransportControls();
        if (controls != null) {
            controls.pause();
            controls.playFromMediaId(data.getId(), null);
        }
    }

}