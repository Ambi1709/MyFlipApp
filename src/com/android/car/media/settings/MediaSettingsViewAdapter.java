package com.android.car.media;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.verticallist.holder.SimpleActionViewHolder;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.util.List;

public class MediaSettingsViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = MediaSettingsViewAdapter.class.getSimpleName();

    private List<MediaSettingsFragment.SettingItem> mItemList;
    private OnItemClickListener mItemClickListener;
    private Context mContext;

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new SimpleActionViewHolder(mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int viewItemPosition) {

        final MediaSettingsFragment.SettingItem settingItem = mItemList.get(viewItemPosition);
        ItemData.Builder builder = new ItemData.Builder();
        switch (settingItem) {
            case AUDIO_SETTINGS:
                builder.setId(settingItem.name())
                        .setPrimaryText(mContext.getString(R.string.settings_item_audio_settings));
                break;
            case RADIO_OPTIONS:
                builder.setId(settingItem.name())
                        .setPrimaryText(mContext.getString(R.string.settings_item_radio_options));
                break;
            case SHARED_MEDIA:
                builder.setId(settingItem.name())
                        .setPrimaryText(mContext.getString(R.string.settings_item_shared_media));
                break;
            case STREAMED_MEDIA:
                builder.setId(settingItem.name())
                        .setPrimaryText(mContext.getString(R.string.settings_item_streamed_media));
                break;
        }

        ItemData value = builder.build();
        ((SimpleActionViewHolder) viewHolder).bindData(value);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(settingItem);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mItemList != null ? mItemList.size() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setSettingItems(List<MediaSettingsFragment.SettingItem> itemList) {
        mItemList = itemList;
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(MediaSettingsFragment.SettingItem settingItem);
    }
}
