package com.android.car.media;

import android.content.Context;
import android.media.MediaDescription;
import android.media.session.MediaSession;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.verticallist.holder.SimpleActionViewHolder;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.List;


public class MediaPlaylistViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaPlaylistViewAdapter";

    List<MediaSession.QueueItem> mQueueList;
    long mCurrentQueueId;

    public interface OnItemClickListener {
        void onItemClick(ItemData itemValue);

        void onQueueItemClick(MediaSession.QueueItem item);
    }

    private OnItemClickListener mItemClickListener;
    private Context mContext;

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
        builder.setId(itemDescription.getMediaId().toString())
                .setPrimaryText(itemDescription.getTitle().toString())
                .setSecondaryText(itemDescription.getSubtitle().toString())
                .setAction1ResId(R.drawable.psa_media_playlist_default_icon)
                .setAction1SelectedResId(R.drawable.psa_media_playlist_active_icon)
                .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW);
        if (mCurrentQueueId == queueItem.getQueueId()) {
            builder.setSelected(true);
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

}