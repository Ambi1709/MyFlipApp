package com.android.car.media;

import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.car.apps.common.util.Assert;
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
    public static final String MAKE_LAST_PLAYED_PLAYLIST_QUEUE_KEY = "MAKE_LAST_PLAYED_PLAYLIST_QUEUE_KEY";
    public static final String SHOW_INDEX_KEY = "SHOW_INDEX";

    static final String ALBUMS_ID = "__ALBUMS__";
    static final String ARTISTS_ID = "__ARTISTS__";
    static final String RECENTLY_PLAYED_ID = "__RECENTLY_PLAYED__";
    static final String FOLDERS_ID = "__FOLDERS__";
    static final String ROOT_ID = "__ROOT__";

    static final String SECONDARY_TEXT_KEY = "SECONDARY_TEXT";

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

    private String mRootCategory;
    private String mCurrentCategory;

    private MediaBrowser.MediaItem mLastPlayedListItem = null;

    private List<ItemData> mItemsList = new ArrayList<>();

    private boolean mShowSections = false;

    private List<String> mMediaToUpdate = new LinkedList<>();

    interface ItemsUpdatedCallback {
        void onItemsUpdated(List<ItemData> result, boolean showSections);

        void onRootItemsUpdated(List<LibraryCategoryGridItemData> result);
    }

    public MediaLibraryController(MediaPlaybackModel model) {
        mMediaPlaybackModel = model;
        mMediaPlaybackModel.addListener(mModelListener);
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
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

    public void getFolderContent(String folder) {
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            browser.subscribe(folder, new Bundle(), new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children, Bundle options) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onChildrenLoaded " + parentId + " with options");
                    }
                    MediaLibraryController.this.onMediaItems(parentId, children);
                }

                @Override
                public void onError(String parentId) {
                    Log.e(TAG, "Error loading children of " + parentId);
                }
            });
            mMediaToUpdate.remove(folder);
        } else {
            mMediaToUpdate.add(folder);
        }
    }

    public void getChildrenElements(String parentId) {
        Log.d(TAG, "getChildrenElements " + parentId);
        mCurrentCategory = parentId;
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            browser.subscribe(parentId, new MediaBrowser.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "onChildrenLoaded " + parentId);
                    }
                    MediaLibraryController.this.onMediaItems(parentId, children);
                }

                @Override
                public void onError(String parentId) {
                    Log.e(TAG, "Error loading children of " + parentId);
                }
            });
            mMediaToUpdate.remove(parentId);
        } else {
            mMediaToUpdate.add(parentId);
        }
    }

    private void onMediaItems(String parentId, List<MediaBrowser.MediaItem> mediaItems) {
        Log.d(TAG, "onChildrenLoaded " + parentId + " size " + mediaItems.size());
        if (!mediaItems.isEmpty()) {
            List<ItemData> result = new ArrayList<>();
            mShowSections = false;
            List<Boolean> showSections = new LinkedList<>();
            showSections.add(mShowSections);
            for (MediaBrowser.MediaItem item : mediaItems) {
                ItemData.Builder builder = new ItemData.Builder()
                        .setId(item.getMediaId())
                        .setPrimaryText(item.getDescription().getTitle().toString())
                        .setAction1ResId(R.drawable.media_library_album_default_art)
                        .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                        .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW);
                Bundle itemExtras = item.getDescription().getExtras();
                if (itemExtras != null && !itemExtras.getString(SECONDARY_TEXT_KEY, "").isEmpty()) {
                    builder.setSecondaryText(itemExtras.getString(SECONDARY_TEXT_KEY));
                } else if (item.getDescription().getSubtitle() != null) {
                    builder.setSecondaryText(item.getDescription().getSubtitle().toString());
                }
                if (isActiveItem(item.getMediaId())) {
                    builder.setSelected(true);
                }

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
                    mShowSections = itemExtras.getBoolean(SHOW_INDEX_KEY, false);
                } else {
                    mShowSections = false;
                }
                showSections.set(0, mShowSections);
                model.setExtras(extras);
                result.add(model);
            }
            mItemsList.clear();
            mItemsList.addAll(result);
            notifyListeners((listener) -> listener.onItemsUpdated(result, showSections.get(0)));
        } else {
            notifyListeners((listener) -> listener.onItemsUpdated(new ArrayList<>(), false));
        }
    }

    public void updateRootElements() {
        Log.d(TAG, "updateRootElements ");
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            String rootId = browser.getRoot();
            if (rootId != null && rootId.length() != 0) {
                browser.subscribe(rootId, new MediaBrowser.SubscriptionCallback() {
                    @Override
                    public void onChildrenLoaded(String parentId, List<MediaBrowser.MediaItem> children) {
                        Log.d(TAG, "onChildrenLoaded " + parentId);
                        List<LibraryCategoryGridItemData> result = new ArrayList<>();
                        mLastPlayedListItem = null;
                        if (children.size() > 0) {
                            for (MediaBrowser.MediaItem item : children) {
                                if (item.getMediaId().equals(RECENTLY_PLAYED_ID)) {
                                    mLastPlayedListItem = item;
                                    continue;
                                }
                                LibraryCategoryGridItemData model =
                                        new LibraryCategoryGridItemData(item.getMediaId(),
                                                item.getDescription().getTitle().toString(),
                                                item.getDescription().getExtras());
                                model.setImageUri(item.getDescription().getIconUri());
                                result.add(model);
                            }

                        }
                        notifyListeners((listener) -> listener.onRootItemsUpdated(result));
                        if (mLastPlayedListItem != null) {
                            updateLastPlayed(mLastPlayedListItem);
                        }
                    }

                    @Override
                    public void onError(String parentId) {
                        Log.e(TAG, "Error loading children of " + parentId);
                    }
                });
            }
            mMediaToUpdate.remove(ROOT_ID);
        } else {
            Log.w(TAG, "Browser service is not connected. Failed to load root categories.");
            mMediaToUpdate.add(ROOT_ID);
        }
    }

    public void unsubscribe(String parentId) {
        Log.d(TAG, "Unsubscribe " + parentId);
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            browser.unsubscribe(parentId);
        }
    }

    public void unsubscribe() {
        Log.d(TAG, "Unsubscribe root");
        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
        if (browser != null && browser.isConnected()) {
            browser.unsubscribe(browser.getRoot());
            if (mLastPlayedListItem != null) {
                Log.d(TAG, "Unsubscribe last played list changes.");
                browser.unsubscribe(mLastPlayedListItem.getMediaId());
            }
        }
    }

    private void updateLastPlayed(MediaBrowser.MediaItem lastPlayedItem) {
        getChildrenElements(lastPlayedItem.getMediaId());
    }

    public void saveRootCategory(String category) {
        mRootCategory = category;
    }

    @Nullable
    public String getVariousSubtitle(Context context) {
        String result = null;
        if (mRootCategory != null && ALBUMS_ID.equals(mRootCategory)) {
            result = context.getResources().getString(R.string.various_artists);
        }
        return result;
    }

    public void playMediaItem(ItemData data) {
        mMediaPlaybackModel.playItem(data.getId(), mCurrentCategory, data.getExtras());
    }

    public void fireAction(ItemData data, int action, String rootCategoryId) {
        Log.d(TAG, "Fire action " + data.getId());
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
        String activeCategoryId = mMediaPlaybackModel.getActiveCategoryId();
        if (!TextUtils.isEmpty(activeCategoryId) && activeCategoryId.equals(mediaId)) {
            result = true;
        } else {
            List<MediaSession.QueueItem> queue = mMediaPlaybackModel.getQueue();
            int queueId = (int) mMediaPlaybackModel.getActiveQueueItemId();
            if (queueId != MediaSession.QueueItem.UNKNOWN_ID && queueId < queue.size()) {
                String activeId = queue.get(queueId).getDescription().getMediaId();
                if (activeId.equals(mediaId)) {
                    result = true;
                }
            }
        }
        return result;
    }


    private final MediaPlaybackModel.Listener mModelListener =
            new MediaPlaybackModel.AbstractListener() {

                @Override
                public void onMediaConnected() {
                    Log.d(TAG, "onMediaConnected");
                    Assert.isMainThread();
                    if (mMediaToUpdate.size() == 0) {
                        onPlaybackStateChanged(mMediaPlaybackModel.getPlaybackState());
                    } else {
                        MediaBrowser browser = mMediaPlaybackModel.getMediaBrowser();
                        if (browser != null && browser.isConnected()) {
                            List<String> mediaCopy = new LinkedList<>(mMediaToUpdate);
                            for (String mediaId : mediaCopy) {
                                if (ROOT_ID.equals(mediaId)) {
                                    updateRootElements();
                                } else {
                                    getChildrenElements(mediaId);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                    Log.d(TAG, "Playback state changed: check active item in current list.");
                    Assert.isMainThread();
                    boolean isUpdateRequired = false;
                    for (ItemData item : mItemsList) {
                        boolean wasSelected = item.isSelected();
                        boolean isCurrentlyActive = isActiveItem(item.getId());
                        if (wasSelected != isCurrentlyActive) {
                            isUpdateRequired = true;
                            item.setSelected(isCurrentlyActive);
                        }
                    }
                    if (isUpdateRequired) {
                        Log.d(TAG, "Playback state changed: update required.");
                        final boolean showSections = mShowSections;
                        notifyListeners((listener) -> listener.onItemsUpdated(mItemsList, showSections));
                    }
                }
            };
}