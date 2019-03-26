package com.android.car.media;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.verticallist.holder.SimpleActionViewHolder;
import com.harman.psa.widget.verticallist.model.ItemData;
import com.harman.psa.widget.verticallist.items.BaseItemView;

import java.util.List;

public class MediaPlaylistViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaPlaylistViewAdapter";

    static final String ITEM_AVAILABLE_KEY = "ITEM_AVAILABLE_KEY";

    static final String DEFAULT_MEDIA_ID_PREFIX = "MediaId_";

    private int mIdCounter = 0;

    List<MediaSession.QueueItem> mQueueList;
    long mCurrentQueueId;

    public interface OnItemClickListener {
        void onQueueItemClick(MediaSession.QueueItem item);
    }

    private OnItemClickListener mItemClickListener;
    private Context mContext;

    private boolean mIsEditMode;

    public void setContext(Context context) {
        mContext = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder holder;

        holder = new SimpleActionViewHolder(mContext);

        return holder;
    }

    public void setItemsData(List<MediaSession.QueueItem> data, long currentQueueId) {
        mQueueList = data;
        mCurrentQueueId = currentQueueId;
    }

    public void setActiveQueueId(long currentQueueId) {
        mCurrentQueueId = currentQueueId;
        notifyDataSetChanged();
    }

    public List<MediaSession.QueueItem> getItemsData() {
        return mQueueList;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int viewItemPosition) {

        final MediaSession.QueueItem queueItem = (MediaSession.QueueItem) mQueueList.get(viewItemPosition);

        //BUILD itemdata

        MediaDescription itemDescription = queueItem.getDescription();
        ItemData.Builder builder = new ItemData.Builder();

        String mediaId = itemDescription.getMediaId();
        if (TextUtils.isEmpty(mediaId)){
            mediaId = DEFAULT_MEDIA_ID_PREFIX + mIdCounter;
            mIdCounter++;
        }

        CharSequence title = itemDescription.getTitle();
        String titleString = MediaStore.UNKNOWN_STRING;
        if(title != null){
            titleString = title.toString();
        }

        CharSequence subTitle = itemDescription.getSubtitle();
        String subTitleString = MediaStore.UNKNOWN_STRING;
        if(subTitle != null){
            subTitleString = subTitle.toString();
        }


        builder.setId(mediaId)
                .setPrimaryText(titleString)
                .setSecondaryText(subTitleString)
                .setAction1ResId(R.drawable.psa_media_playlist_default_icon)
                .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW);
        if (mCurrentQueueId == queueItem.getQueueId()) {
            builder.setSelected(true);
        }
        Bundle extras = itemDescription.getExtras();
        if (extras != null) {
            builder.setEnabled(extras.getBoolean(ITEM_AVAILABLE_KEY, true));
        }

        ItemData value = builder.build();
        value.setAction1DrawableUri(itemDescription.getIconUri());


        final View checkableView = ((SimpleActionViewHolder) viewHolder).bindData(value);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null) {
                    mItemClickListener.onQueueItemClick(queueItem);
                }
            }
        });

        if (mIsEditMode) {
            viewHolder.itemView.setBackgroundResource(R.drawable.psa_general_generic_state_container_focus);
            ((BaseItemView)(viewHolder.itemView)).getPrimaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColor(R.color.psa_default_grid_list_item_state_text_color_focused));
            ((BaseItemView)(viewHolder.itemView)).getSecondaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColor(R.color.psa_default_grid_list_item_state_text_color_focused));
        } else {
            viewHolder.itemView.setBackgroundResource(R.drawable.psa_general_generic_state_container);
            ((BaseItemView)(viewHolder.itemView)).getPrimaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColorStateList(R.color.psa_default_grid_list_item_state_text_color));
            ((BaseItemView)(viewHolder.itemView)).getSecondaryTextView().setTextColor(viewHolder.itemView.getContext().getResources().getColorStateList(R.color.psa_default_grid_list_item_state_text_color));
        }
    }


    @Override
    public int getItemCount() {
        return mQueueList == null ? 0 : mQueueList.size();
    }

    public void setItemClickListener(OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }


    @Override
    public int getItemViewType(int position) {
        int viewType = -1;
        return viewType;
    }

    public void setEditModeEnabled(boolean isEditModeEnabled) {
        mIsEditMode = isEditModeEnabled;
        notifyDataSetChanged();
    }

}