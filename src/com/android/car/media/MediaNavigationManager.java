package com.android.car.media;

import android.content.Context;

import androidx.fragment.app.FragmentManager;

import android.util.Log;

import com.android.car.media.R;
import com.android.car.media.widget.WidgetMediaPlayerFragment;
import com.android.car.media.widget.WidgetRadioFragment;
import com.android.car.radio.MainRadioFragment;
import com.harman.psa.widget.PSABaseNavigationManager;
import com.harman.psa.widget.PSATabBarManager;
import com.harman.psa.widget.tabview.PSATabView;

import java.util.HashMap;

public class MediaNavigationManager extends PSABaseNavigationManager implements PSATabBarManager.OnTabChangeListener {
    private static final String TAG = "MediaNavigationManager";

    private int mActiveApp = MediaConstants.MEDIA_APP;
    private int mActiveTab = MediaTab.PLAYER.ordinal();
    private PSATabBarManager mTabBarManager;
    private boolean mIsReadyToInteract;
    private boolean mShowWidgetView = false;

    private static enum MediaTab {
        PLAYER,
        TRACKLIST,
        LIBRARY,
        SETTINGS
    }

    private static enum RadioTab {
        PLAYER,
        STATION_LIST,
        FAVOURITES,
        SETTINGS
    }

    private HashMap<Integer, Integer> mTabLabels = new HashMap<>();

    public MediaNavigationManager(Context context, FragmentManager fragmentManager, int contentContainer) {
        super(context, fragmentManager, contentContainer);
    }

    public void lockWidgetView() {
        mIsReadyToInteract = false;
        mShowWidgetView = true;
    }

    public void setActiveApp(int activeApp) {
        mActiveApp = activeApp;
        refreshTabLabels();
    }

    public void showActiveApp(int activeTab, boolean dontUpdateFragment) {
        if (mShowWidgetView && !dontUpdateFragment) {
            clearBackStack();
            if (mActiveApp == MediaConstants.RADIO_APP) {
                showFragment(new WidgetRadioFragment());
            } else {
                showFragment(new WidgetMediaPlayerFragment());
            }
        } else {
            mIsReadyToInteract = true;
            mTabBarManager.setSelectedTab(activeTab);
            if (!dontUpdateFragment) {
                onTabChanged(null, activeTab);
            } else {
                mActiveTab = activeTab;
            }
        }

    }

    public void showActiveApp() {
        showActiveApp(mActiveTab, false);
    }

    private void refreshTabLabels() {
        mTabLabels = new HashMap<>();
        if (mActiveApp == MediaConstants.RADIO_APP) {
            mTabLabels.put(RadioTab.PLAYER.ordinal(), R.string.player_radio_tab);
            mTabLabels.put(RadioTab.STATION_LIST.ordinal(), R.string.station_list_radio_tab);
            mTabLabels.put(RadioTab.FAVOURITES.ordinal(), R.string.favourites_radio_tab);
            mTabLabels.put(RadioTab.SETTINGS.ordinal(), R.string.settings_media_tab);
        } else {
            mTabLabels.put(MediaTab.PLAYER.ordinal(), R.string.player_media_tab);
            mTabLabels.put(MediaTab.TRACKLIST.ordinal(), R.string.tracklist_media_tab);
            mTabLabels.put(MediaTab.LIBRARY.ordinal(), R.string.library_media_tab);
            mTabLabels.put(MediaTab.SETTINGS.ordinal(), R.string.settings_media_tab);
        }
    }

    private void openMediaTab(int tabNumber) {
        clearBackStack();
        MediaTab tab = MediaTab.values()[tabNumber];
        switch (tab) {
            case PLAYER:
                showFragment(new MediaPlaybackFragment());
                break;
            case TRACKLIST:
                showFragment(new MediaPlaylistFragment());
                break;
            case LIBRARY:
                showFragment(new MediaBrowseFragment());
                break;
            case SETTINGS:
                showFragment(new MediaSettingsFragment());
                break;
            default:
                Log.e(TAG, "Failed to show screen: unknown tab number");
        }
    }


    private void openRadioTab(int tabNumber) {
        clearBackStack();
        RadioTab tab = RadioTab.values()[tabNumber];
        switch (tab) {
            case PLAYER:
            case STATION_LIST:
            case FAVOURITES:
            case SETTINGS:
            default:
                showFragment(new MainRadioFragment());
                break;
        }
    }

    @Override
    public void onTabChanged(PSATabView tab, int tabNumber) {
        if (!mIsReadyToInteract) {
            return;
        }
        mActiveTab = tabNumber;
        if (mActiveApp == MediaConstants.RADIO_APP) {
            openRadioTab(tabNumber);
        } else {
            openMediaTab(tabNumber);
        }
    }

    public int getActiveTab() {
        return mActiveTab;
    }

    public void formMediaTabBar(PSATabBarManager tabManager, Context context) {
        mTabBarManager = tabManager;
        mTabBarManager.removeAllTabs();
        PSATabView tabView;
        if (mActiveApp == MediaConstants.RADIO_APP) {
            for (RadioTab radioTab : RadioTab.values()) {
                tabView = new PSATabView(context);
                String tabLabel = context.getResources().getString(mTabLabels.get(radioTab.ordinal()));
                tabView.setTitle(tabLabel);
                mTabBarManager.addTab(tabView);
            }
        } else {
            for (MediaTab mediaTab : MediaTab.values()) {
                tabView = new PSATabView(context);
                String tabLabel = context.getResources().getString(mTabLabels.get(mediaTab.ordinal()));
                tabView.setTitle(tabLabel);
                mTabBarManager.addTab(tabView);
            }
        }
    }

    public void openPlayerTab(String sourceId) {
        if (mShowWidgetView) {
            return;
        }
        mActiveApp = MediaConstants.MEDIA_APP;
        refreshTabLabels();
        showFragment(MediaPlaybackFragment.newInstance(sourceId));
    }

    public void setTabBarEnabled(boolean isEnabled) {
        for (int i = 0; i < MediaTab.values().length; ++i) {
            mTabBarManager.getTabAt(i).enable(isEnabled);
        }
    }
}
