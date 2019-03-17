package com.android.car.media;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.harman.psa.widget.PSAAppBarButton;
import com.harman.psa.widget.PSABaseFragment;
import com.harman.psa.widget.verticallist.PsaRecyclerView;

import java.util.Arrays;

public class MediaSettingsFragment extends PSABaseFragment
        implements MediaSettingsViewAdapter.OnItemClickListener {

    private static final String TAG = MediaSettingsFragment.class.getSimpleName();
    private static final String AUDIO_SETTING_NUMBER_KEY = "AUDIO_SETTING_NUMBER";
    private static final String PSA_CAR_SETTINGS_ACTION = "com.android.car.settings.PSACarSettings";

    public enum SettingItem {
        AUDIO_SETTINGS,
        RADIO_OPTIONS,
        SHARED_MEDIA,
        STREAMED_MEDIA
    }

    private PsaRecyclerView mRecyclerView;
    private MediaSettingsViewAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                            @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.psa_media_settings_fragment, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getAppBarView().removeAppBarButton(PSAAppBarButton.Position.LEFT_SIDE_3);

        mRecyclerView = view.findViewById(R.id.settings_list);

        mAdapter = new MediaSettingsViewAdapter();
        mAdapter.setContext(getHostActivity());
        mAdapter.setOnItemClickListener(this);

        mAdapter.setSettingItems(Arrays.asList(SettingItem.values()));

        mRecyclerView.setItemAnimator(null);
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public void onItemClick(SettingItem settingItem) {
        Intent intent = new Intent(PSA_CAR_SETTINGS_ACTION);
        intent.putExtra(AUDIO_SETTING_NUMBER_KEY, settingItem.ordinal());
        startActivity(intent);
    }
}
