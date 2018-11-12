package com.android.car.media;


import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.gridlist.item.TextGridListItem;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.io.IOException;
import java.util.List;

public class LibraryCategoryGridListAdapter extends RecyclerView.Adapter<LibraryCategoryGridListAdapter.ViewHolder> {

    private static final String TAG = "LibraryCategoryGridListAdapter";
    /**
     * Set of specific mData
     */
    private final List<ItemData> mDataList;
    /**
     * Listener of onClick event
     */
    private LibraryCategoryGridListItemClickListener mOnClickListener;

    /**
     * Interface to declare method for click event handling
     */
    public interface LibraryCategoryGridListItemClickListener {
        void onItemClicked(ItemData data);
    }


    /**
     * View holder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * View
         */
        private TextGridListItem mView;
        /**
         * Data
         */
        private ItemData mData;

        ViewHolder(View v) {
            super(v);
            mView = v.findViewById(R.id.grid_text_item_view);
        }

        void setData(ItemData data, final LibraryCategoryGridListItemClickListener onClickListener) {
            mData = data;
            mView.setCoverImage(mView.getContext().getResources().getDrawable(data.getAction1ResId()));
            if (data.getAction1DrawableUri() != null) {
                LoadIconTask loadArt = new LoadIconTask(data.getAction1DrawableUri(), mView);
                loadArt.execute();
            }
            mView.setPrimaryText(data.getPrimaryText());
            mView.setSecondaryText(data.getSecondaryText());

            mView.setEnabled(data.isEnabled());
            mView.setActivated(data.isSelected());

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onItemClicked(mData);
                }
            });
        }
    }

    public LibraryCategoryGridListAdapter(List<ItemData> dataList,
                                          LibraryCategoryGridListItemClickListener listener) {
        mDataList = dataList;
        mOnClickListener = listener;
    }


    @NonNull
    @Override
    public LibraryCategoryGridListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                        int viewType) {
        int viewId = R.layout.psa_grid_list_text_item;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewId, parent, false);

        return new LibraryCategoryGridListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryCategoryGridListAdapter.ViewHolder holder, int position) {
        holder.setData(mDataList.get(position), mOnClickListener);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }


    public static class LoadIconTask extends AsyncTask<Void, Void, Bitmap> {
        private static final String LOG_TAG = "LoadIconTask";
        private Uri mUri;
        private TextGridListItem mView;

        public LoadIconTask(@NonNull Uri url, TextGridListItem v) {
            mUri = url;
            mView = v;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            final Uri bitmapUri = mUri;
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(mView.getContext().getContentResolver(), bitmapUri);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed to download artwork from path: " + mUri.getPath());
            }
            return bitmap;

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                Drawable bitmapDrawable = new BitmapDrawable(bitmap);
                mView.setCoverImage(bitmapDrawable);
                if (!mUri.getScheme().equals(ContentResolver.SCHEME_ANDROID_RESOURCE)) {
                    mView.setPrimaryText("");
                    mView.setSecondaryText("");
                }
            }
        }
    }
}
