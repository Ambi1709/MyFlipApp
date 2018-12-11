package com.android.car.media.widget;


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.media.browse.MediaBrowser;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.car.media.MediaLibraryController;
import com.android.car.media.R;
import com.harman.psa.widget.verticallist.PsaRecyclerView;
import com.harman.psa.widget.verticallist.adapter.PsaSectionsRecyclerViewAdapter;
import com.harman.psa.widget.verticallist.model.ItemData;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class SubFragment extends Fragment {

    public interface OnChooseItemListener {
        void onChoose(ItemData itemValue);
    }

    public enum Type {ALBUM, ARTIST, SONG}

    private static final String TYPE_ARGUMENT = "type_argument";
    private static final String PATH = "PATH";
    public static final String TYPE_ITEM = "type_item";
    public static final String TYPE_ITEM_ALBUM = "type_item_album";
    public static final String TYPE_ITEM_ARTIST = "type_item_artist";
    public static final String TYPE_ITEM_SONG = "type_item_song";
    private static final int INVALID_ID = -1;
    public static final String ITEM_ID = "ID";

    private static final Uri ART_BASE_URI = Uri.parse("content://media/external/audio/albumart");

    private PsaRecyclerView mRecyclerView;
    private PsaSectionsRecyclerViewAdapter mAdapter;
    private TextView mEmptyView;
    private ProgressBar mProgressBar;
    private GetDataTask mGetDataTask;
    private OnChooseItemListener mChooseItemListener;

    private Type mType;

    public static Fragment newInstance(Type type) {
        final SubFragment fragment = new SubFragment();
        Bundle args = new Bundle();
        args.putSerializable(TYPE_ARGUMENT, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mChooseItemListener = (OnChooseItemListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnChooseItemListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initView(view);

        mType = (Type) getArguments().getSerializable(TYPE_ARGUMENT);

        switch (mType) {
            case ALBUM:
                mGetDataTask = new GetDataTask.Builder()
                        .setResolver(getContext().getContentResolver())
                        .setUri(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI)
                        .setKeyColumn(MediaStore.Audio.AudioColumns.ALBUM_KEY)
                        .setTitleColumn(MediaStore.Audio.AudioColumns.ALBUM)
                        .setAlbumIdColumn(MediaStore.Audio.Albums._ID)
                        .setFragment(this)
                        .setDefaulResId(R.drawable.psa_album_default_art)
                        .setType(TYPE_ITEM_ALBUM)
                        .build();
                break;
            case ARTIST:
                mGetDataTask = new GetDataTask.Builder()
                        .setResolver(getContext().getContentResolver())
                        .setUri(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI)
                        .setKeyColumn(MediaStore.Audio.AudioColumns.ARTIST_KEY)
                        .setTitleColumn(MediaStore.Audio.AudioColumns.ARTIST)
                        .setFragment(this)
                        .setDefaulResId(R.drawable.psa_artist_default_art)
                        .setType(TYPE_ITEM_ARTIST)
                        .build();

                break;
            default:
                mGetDataTask = new GetDataTask.Builder()
                        .setResolver(getContext().getContentResolver())
                        .setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                        .setKeyColumn(MediaStore.Audio.Media.TITLE_KEY)
                        .setTitleColumn(MediaStore.Audio.Media.TITLE)
                        .setAlbumColumn(MediaStore.Audio.AudioColumns.ALBUM)
                        .setArtistColumn(MediaStore.Audio.AudioColumns.ARTIST)
                        .setAlbumIdColumn(MediaStore.Audio.AudioColumns.ALBUM_ID)
                        .setFragment(this)
                        .setDefaulResId(R.drawable.psa_track_default_art)
                        .setType(TYPE_ITEM_SONG)
                        .build();
                break;
        }

        mGetDataTask.execute();
    }

    private void initView(View view) {
        mRecyclerView = view.findViewById(R.id.list);
        mEmptyView = view.findViewById(R.id.empty);
        mProgressBar = view.findViewById(R.id.progress);
        mAdapter = new PsaSectionsRecyclerViewAdapter();
        mEmptyView.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.VISIBLE);
        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setItemClickListener(new PsaSectionsRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(ItemData itemValue) {
                if (mChooseItemListener != null) {
                    mChooseItemListener.onChoose(itemValue);
                }
            }
        });
    }

    private void onUpdateData(List<ItemData> itemData) {
        mProgressBar.setVisibility(View.GONE);
        if (itemData != null && !itemData.isEmpty()) {
            mEmptyView.setVisibility(View.GONE);
            ArrayList<String> keys = new ArrayList<String>();
            HashMap<String, List<ItemData>> map = new HashMap<>();
            keys.add(mType.toString());
            map.put(mType.toString(), itemData);
            mAdapter.setKeysArray(keys);
            mAdapter.setSectionsLists(map);
            mAdapter.notifyDataSetChanged();
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }


    private static class GetDataTask extends AsyncTask<Void, Void, List<ItemData>> {

        private String mKeyColumn;
        private String mTitleColumn;
        private String mArtistColumn;
        private String mAlbumColumn;
        private String mAlbumIdColumn;
        private int mDefaultResId;
        private Uri mUri;
        private ContentResolver mResolver;
        private WeakReference<SubFragment> mFragment;
        private String mType;


        private GetDataTask(Builder builder) {
            mKeyColumn = builder.mKeyColumn;
            mTitleColumn = builder.mTitleColumn;
            mUri = builder.mUri;
            mResolver = builder.mResolver;
            mArtistColumn = builder.mArtistColumn;
            mAlbumColumn = builder.mAlbumColumn;
            mAlbumIdColumn = builder.mAlbumIdColumn;
            mFragment = new WeakReference<>(builder.mFragment);
            mDefaultResId = builder.mDefaultResId;
            mType = builder.mType;
        }


        @Override
        protected List<ItemData> doInBackground(Void... voids) {

            List<ItemData> list = new ArrayList<>();

            Cursor cursor = mResolver.query(mUri, null, null,
                    null, null);
            if (cursor != null) {
                int keyColumn = cursor.getColumnIndex(mKeyColumn);
                int titleColumn = cursor.getColumnIndex(mTitleColumn);

                int artistColumn = -1;
                if (mArtistColumn != null) {
                    artistColumn = cursor.getColumnIndex(mArtistColumn);
                }
                int albumIdColumn = -1;
                if (mAlbumIdColumn != null) {
                    albumIdColumn = cursor.getColumnIndex(mAlbumIdColumn);
                }

                int albumColumn = -1;
                if (mAlbumColumn != null) {
                    albumColumn = cursor.getColumnIndex(mAlbumColumn);
                }

                int pathColumn = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);

                while (cursor.moveToNext()) {

                    Uri albumArtUri = null;
                    if (albumIdColumn != -1) { // find art by album id
                        long id = cursor.getLong(albumIdColumn);
                        albumArtUri = ContentUris.withAppendedId(ART_BASE_URI, id);
                        try {
                            InputStream dummy = mResolver.openInputStream(albumArtUri);
                            dummy.close();
                        } catch (IOException e) {
                            // Ignored because the albumArt is intialized correctly anyway.
                        }
                    }

                    String key = cursor.getString(keyColumn);
                    String title = cursor.getString(titleColumn);
                    StringBuilder subTitle = new StringBuilder();

                    if (artistColumn != -1) {
                        subTitle.append(cursor.getString(artistColumn));
                    }

                    if (albumColumn != -1) {
                        if (!subTitle.toString().isEmpty()) {
                            subTitle.append(" - ");
                        }
                        subTitle.append(cursor.getString(albumColumn));
                    }

                    String path = "";
                    if (pathColumn != -1) {
                        path = cursor.getString(pathColumn);
                    }


                    ItemData.Builder builder = new ItemData.Builder()
                            .setId(key)
                            .setPrimaryText(title)
                            .setAction1DrawableUri(albumArtUri)
                            .setAction1ViewType(ItemData.ACTION_VIEW_TYPE_IMAGEVIEW);

                    if (mDefaultResId != INVALID_ID) {
                        builder.setAction1ResId(mDefaultResId);
                    }

                    if (!subTitle.toString().isEmpty()) {
                        builder.setSecondaryText(subTitle.toString());
                    }

                    ItemData item = builder.build();
                    Bundle bundle = new Bundle();
                    bundle.putString(PATH, path);
                    bundle.putInt(MediaLibraryController.MEDIA_ITEM_TYPE_KEY, MediaBrowser
                            .MediaItem.FLAG_BROWSABLE);
                    bundle.putString(ITEM_ID, key);
                    bundle.putString(TYPE_ITEM, mType);
                    item.setExtras(bundle);
                    list.add(item);
                }
            }

            return list;
        }

        @Override
        protected void onPostExecute(List<ItemData> list) {
            final SubFragment callback = mFragment.get();
            if (callback != null) {
                callback.onUpdateData(list);
            }
        }

        public static class Builder {
            private String mKeyColumn;
            private String mTitleColumn;
            private String mArtistColumn;
            private String mAlbumColumn;
            private String mAlbumIdColumn;
            private Uri mUri;
            private ContentResolver mResolver;
            private SubFragment mFragment;
            private int mDefaultResId = INVALID_ID;
            private String mType;

            public Builder setResolver(ContentResolver resolver) {
                mResolver = resolver;
                return this;
            }

            public Builder setFragment(SubFragment fragment) {
                mFragment = fragment;
                return this;
            }

            public Builder setUri(Uri uri) {
                mUri = uri;
                return this;
            }

            public Builder setType(String type) {
                mType = type;
                return this;
            }

            public Builder setDefaulResId(int id) {
                mDefaultResId = id;
                return this;
            }

            public Builder setKeyColumn(String keyColumn) {
                mKeyColumn = keyColumn;
                return this;
            }

            public Builder setTitleColumn(String titleColumn) {
                mTitleColumn = titleColumn;
                return this;
            }


            public Builder setArtistColumn(String artistColumn) {
                mArtistColumn = artistColumn;
                return this;
            }

            public Builder setAlbumColumn(String albumColumn) {
                mAlbumColumn = albumColumn;
                return this;
            }

            public Builder setAlbumIdColumn(String albumIdColumn) {
                mAlbumIdColumn = albumIdColumn;
                return this;
            }

            public GetDataTask build() {
                if (mUri == null || mKeyColumn == null || mResolver == null
                        || mTitleColumn == null) {
                    throw new IllegalStateException(
                            "uri, keyColumn, resolver and titleColumn are required.");
                }
                return new GetDataTask(this);
            }
        }

    }

    static Uri getUriForResource(Context context, int id) {
        Resources res = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + res.getResourcePackageName(id)
                + "/" + res.getResourceTypeName(id)
                + "/" + res.getResourceEntryName(id));
    }

}
