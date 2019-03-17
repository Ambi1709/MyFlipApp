package com.android.car.radio;


import com.harman.psa.widget.PSABaseFragment;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.os.Bundle;
import com.android.car.media.R;
import com.android.car.media.MediaActivity;

import com.harman.psa.widget.PSAAppBarButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class MainRadioFragment extends PSABaseFragment {
	private static final String TAG = "MainRadio";

	private View mRootView;

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.radio_main_screen, container, false);

        return mRootView;
    }


        @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}