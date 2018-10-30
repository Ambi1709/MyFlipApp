package com.android.car.media;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.car.media.LibraryCategory;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.gridlist.layout.IconItemsGridListGroupAdapter;
import com.harman.psa.widget.gridlist.model.GridItemDataProvider;
import com.harman.psa.widget.toast.PSAToast;

import java.util.ArrayList;
import java.util.List;

public class MediaBrowseFragment extends PSABaseFragment implements
        IconItemsGridListGroupAdapter.GridListGroupItemClickListener {
    private static final String TAG = "MediaBrowseFragment";

    private static final boolean VERTICAL_GRID = true;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    List<GridItemDataProvider> categoriesList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_browse_fragment, container, false);

        mRecyclerView = v.findViewById(R.id.library_grid);
        mRecyclerView.setHasFixedSize(true);

        fillcategoriesList();

        mAdapter = new IconItemsGridListGroupAdapter(categoriesList, this, VERTICAL_GRID);

        mRecyclerView.setAdapter(mAdapter);

        return v;
    }

    @Override
    public void onItemClicked(GridItemDataProvider data) {
        PSAToast.makeText(getContext(), "Item clicked!", Toast.LENGTH_SHORT).show();
    }

    private void fillcategoriesList() {
        String artistLabel = getContext().getResources().getString(R.string.library_category_artists);
        Drawable artistIcon = getContext().getResources().getDrawable(R.drawable.psa_media_library_artist_icon);
        LibraryCategory artistCategory = new LibraryCategory(artistLabel, artistIcon);
        categoriesList.add(artistCategory);

        String playlistLabel = getContext().getResources().getString(R.string.library_category_playlists);
        Drawable playlistIcon = getContext().getResources().getDrawable(R.drawable.psa_media_library_playlist_icon);
        LibraryCategory playlistCategory = new LibraryCategory(playlistLabel, playlistIcon);
        categoriesList.add(playlistCategory);

        String folderLabel = getContext().getResources().getString(R.string.library_category_folders);
        Drawable folderIcon = getContext().getResources().getDrawable(R.drawable.psa_media_library_folder_icon);
        LibraryCategory folderCategory = new LibraryCategory(folderLabel, folderIcon);
        categoriesList.add(folderCategory);

        String albumLabel = getContext().getResources().getString(R.string.library_category_albums);
        Drawable albumIcon = getContext().getResources().getDrawable(R.drawable.psa_media_library_album_icon);
        LibraryCategory albumCategory = new LibraryCategory(albumLabel, albumIcon);
        categoriesList.add(albumCategory);

        String genreLabel = getContext().getResources().getString(R.string.library_category_genres);
        Drawable genreIcon = getContext().getResources().getDrawable(R.drawable.psa_media_library_genre_icon);
        LibraryCategory genreCategory = new LibraryCategory(genreLabel, genreIcon);
        categoriesList.add(genreCategory);


    }


}