package com.android.car.media;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.media.LibraryCategoryGridItemData;
import com.harman.psa.widget.gridlist.item.IconGridListItem;

import java.io.IOException;
import java.util.List;


/**
 * DefaultItemsGridListGroupAdapter is used to bind mData set to views to present it on the screen
 */
public class LibraryGridListAdapter extends RecyclerView.Adapter<LibraryGridListAdapter.ViewHolder> {
    /**
     * Set of specific mData
     */
    private final List<LibraryCategoryGridItemData> mDataList;
    /**
     * Listener of onClick event
     */
    private LibraryGridListItemClickListener mOnClickListener;

    /**
     * Interface to declare method for click event handling
     */
    public interface LibraryGridListItemClickListener {
        void onItemClicked(LibraryCategoryGridItemData data);
    }


    /**
     * View holder
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /**
         * View
         */
        private IconGridListItem mView;
        /**
         * Data
         */
        private LibraryCategoryGridItemData mData;

        public ViewHolder(View v) {
            super(v);
            mView = v.findViewById(R.id.grid_icon_item_view);
        }

        public void setData(LibraryCategoryGridItemData data, final LibraryGridListItemClickListener onClickListener) {
            mData = data;
            mView.setCoverImage(mView.getContext().getResources().getDrawable(R.drawable.psa_media_library_album_icon));

            if (data.getImageUri() != null) {
                LoadIconTask loadIcon = new LoadIconTask(data.getImageUri(), mView);
                loadIcon.execute();
            }
            mView.setLabelText(data.getPrimaryText());

            mView.setEnabled(data.isEnabled());
            mView.setActivated(data.isActive());

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickListener.onItemClicked(mData);
                }
            });
        }

        public void setImage(Drawable drawable) {
            mView.setCoverImage(drawable);
        }
    }

    public LibraryGridListAdapter(List<LibraryCategoryGridItemData> dataList,
                                  LibraryGridListItemClickListener listener) {
        mDataList = dataList;
        mOnClickListener = listener;
    }


    @NonNull
    @Override
    public LibraryGridListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                int viewType) {
        int viewId = R.layout.psa_grid_list_icon_item;
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewId, parent, false);

        return new LibraryGridListAdapter.ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryGridListAdapter.ViewHolder holder, int position) {
        holder.setData(mDataList.get(position), mOnClickListener);
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public void updateDataList(List<LibraryCategoryGridItemData> dataList) {
        mDataList.clear();
        mDataList.addAll(dataList);
        notifyDataSetChanged();
    }

    public static class LoadIconTask extends AsyncTask<Void, Void, Bitmap> {
        private static final String LOG_TAG = "LoadIconTask";
        private Uri mUri;
        private IconGridListItem mView;

        public LoadIconTask(@NonNull Uri url, IconGridListItem v) {
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
            }
        }
    }
}