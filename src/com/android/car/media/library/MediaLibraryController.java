package com.android.car.media;


import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
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

    public static final String MEDIA_ITEM_TYPE_KEY = "ITEM_TYPE_KEY";
    public static final String ROOT_CATEGORY_ID_KEY = "ROOT_CATEGORY_KEY";
    public static final String MAKE_NEW_QUEUE_KEY = "MAKE_QUEUE_KEY";
    public static final String SHOW_INDEX_KEY = "SHOW_INDEX";

    static final String ALBUMS_ID = "__ALBUMS__";
    static final String ARTISTS_ID = "__ARTISTS__";

    static final String PLAY_ITEM_ACTION_KEY = "PLAY_ITEM";
    static final String PLAY_ITEM_ACTION_TEXT_KEY = "PLAY_ITEM_TEXT";
    static final String ADD_TOP_ACTION_KEY = "ADD_TOP_ACTION";
    static final String ADD_BOTTOM_ACTION_KEY = "ADD_BOTTOM_ACTION";
    static final String PLAY_SHUFFLE_ACTION_KEY = "PLAY_SHUFFLE";
    static final String PLAY_SHUFFLE_ACTION_TEXT_KEY = "PLAY_SHUFFLE_TEXT";

    static final int PLAY_ITEM_ACTION_INDEX = 0;
    static final int ADD_TOP_ACTION_INDEX = 1;
    static final int ADD_BOTTOM_ACTION_INDEX = 2;
    static final int PLAY_SHUFFLE_ACTION_INDEX = 3;

    private MediaPlaybackModel mMediaPlaybackModel;

    private final List<ItemsUpdatedCallback> mItemsChangedListeners = new LinkedList<>();

    private static String mRootCategory;

    private String[] mActiveMediaId;
    private List<ItemData> mItemsList = new ArrayList<>();

    interface ItemsUpdatedCallback {
        void onItemsUpdated(List<ItemData> result, boolean showSections);

        void onRootItemsUpdated(List<LibraryCategoryGridItemData> result);
    }

    public MediaLibraryController(MediaPlaybackModel model, ItemsUpdatedCallback listener) {
        mMediaPlaybackModel = model;
        mMediaPlaybackModel.addListener(mModelListener);
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
                        if (isActiveItem(item.getMediaId())) {
                            builder.setSelected(true);
                        }
                        Bundle itemExtras = item.getDescription().getExtras();
                        if (itemExtras != null &&
                                itemExtras.getBoolean(MediaLibraryController.PLAY_ITEM_ACTION_KEY, false) == true) {
                            builder.setAction2ResId(R.drawable.psa_media_library_item_action_icon);
                        } else {
                            builder.setAction2ResId(-2);
                        }
                        ItemData model = builder.build();
                        model.setAction1DrawableUri(item.getDescription().getIconUri());
                        Bundle extras = itemExtras;
                        if (extras == null) {
                            extras = new Bundle();
                        }
                        extras.putInt(MEDIA_ITEM_TYPE_KEY, (item.isPlayable() ? MediaBrowser.MediaItem.FLAG_PLAYABLE
                                : MediaBrowser.MediaItem.FLAG_BROWSABLE));
                        if (itemExtras != null) {
                            showSections.set(0, itemExtras.getBoolean(SHOW_INDEX_KEY, false));
                        } else {
                            showSections.set(0, false);
                        }
                        model.setExtras(extras);
                        result.add(model);
                    }
                    mItemsList.clear();
                    mItemsList.addAll(result);
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
                                            item.getDescription().getTitle().toString(),
                                            item.getDescription().getExtras());
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
            controls.playFromMediaId(data.getId(), data.getExtras());
        }
    }

    public void fireAction(ItemData data, int action, String rootCategoryId) {
        int itemType = -1;
        Bundle extras = data.getExtras();
        if (extras != null) {
            itemType = extras.getInt(MEDIA_ITEM_TYPE_KEY, -1);
        }
        extras.putBoolean(MAKE_NEW_QUEUE_KEY, true);
        switch (action) {
            case PLAY_ITEM_ACTION_INDEX:
                mMediaPlaybackModel.playItemAction(data.getId(), itemType, rootCategoryId, extras);
                break;
            case ADD_TOP_ACTION_INDEX:
                mMediaPlaybackModel.addItemToQueueTopAction(data.getId(), itemType, rootCategoryId, extras);
                break;
            case ADD_BOTTOM_ACTION_INDEX:
                mMediaPlaybackModel.addItemToQueueBottomAction(data.getId(), itemType, rootCategoryId, extras);
                break;
            case PLAY_SHUFFLE_ACTION_INDEX:
                mMediaPlaybackModel.shufflePlayItemAction(data.getId(), itemType, rootCategoryId, extras);
                break;
            default:
                Log.e(TAG, "Unknown action index: " + action);
        }
    }

    private boolean isActiveItem(String mediaId) {
        boolean result = false;
        if (mActiveMediaId != null) {
            for (String id : mActiveMediaId) {
                if (id.equals(mediaId)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public void setActiveIds(String[] ids) {
        mActiveMediaId = ids;
    }

    private final MediaPlaybackModel.Listener mModelListener =
        new MediaPlaybackModel.AbstractListener() {

            @Override
            public void onMediaConnected() {
                Assert.isMainThread();
                onQueueChanged(mMediaPlaybackModel.getQueue());
                onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
            }

            @Override
            public void onQueueChanged(List<MediaSession.QueueItem> queue) {
                Assert.isMainThread();
                // TODO
                //mActiveMediaId[]
                // update list
                //onItemsUpdated
            }

            @Override
            public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                // TODO
                //mActiveMediaId[]
                // update list
                //onItemsUpdated
            }
        };

}