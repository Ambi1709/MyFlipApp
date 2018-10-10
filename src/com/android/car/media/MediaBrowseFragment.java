package com.android.car.media;

import android.os.Bundle;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.PSAAppBarButton;
import com.android.car.app.CarDrawerAdapter;
import com.android.car.media.drawer.MediaDrawerController;
import com.android.car.view.PagedListView;
import android.widget.ProgressBar;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;

public class MediaBrowseFragment extends PSABaseFragment{
	private static final String TAG = "MediaBrowse";

	private PagedListView mDrawerList;
	private ProgressBar mProgressBar;

    private MediaDrawerController mDrawerController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_browse_fragment, container, false);

        return v;
    }
}