package com.android.car.media.widget.ui;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.car.media.MediaConstants;
import com.android.car.media.widget.CarMediaWidgetData;
import com.android.car.media.R;

import com.harman.psa.widget.PSAUtils;
import com.harman.psa.widget.button.PSALabelButton;

import java.util.ArrayList;

@RemoteViews.RemoteView
public class CoverFlowPager extends ViewPager implements ViewPager.PageTransformer {

    private static final int OFFSCREEN_PAGE_LIMIT = 3;
    private static final int OFFSCREEN_PAGE_LIMIT_BRAND1 = 5;
    private static final float PAGE_SCALE_FACTOR_COEFFICIENT = 0.6f;

    private int mCurrentPosition = 0;
    private boolean mScrolled = false;
    private Handler mHandler = new Handler();
    protected String mThemeBrand = "brand0";

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mCurrentPosition = getCurrentItem();
            mScrolled = true;
        }

        @Override
        public void onPageSelected(int position) {
            if (position > mCurrentPosition) {
                Intent playIntent = new Intent(MediaConstants.ACTION_NEXT);
                getContext().sendBroadcast(playIntent);
            } else {
                Intent playIntent = new Intent(MediaConstants.ACTION_PREV);
                getContext().sendBroadcast(playIntent);
            }
            mCurrentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    public CoverFlowPager(@NonNull Context context) {
        super(context);
        init();
    }

    public CoverFlowPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeOnPageChangeListener(mOnPageChangeListener);
    }

    private void init() {

      //  HomeApplication application = ((HomeApplication) getContext().getApplicationContext());

     //   mThemeBrand = PSAUtils.getCurrentTheme(getContext());

        CarMediaWidgetData data = new CarMediaWidgetData(); //application.getCarMediaWidgetData();

        CoverFlowAdapter pagerAdapter = new CoverFlowAdapter(getContext(), data.getUrlList(), new CoverFlowAdapter.OnClickItemListener() {
            @Override
            public void onClickItem(int position) {
                if (mCurrentPosition == position) {
                    mScrolled = false;
                    mHandler.postDelayed(new MediaRunnable(), MediaConstants.WIDGET_SCROLLING_DELAY);
                }
                setCurrentItem(position, true);
            }
        });

        setAdapter(pagerAdapter);
        setCurrentItem(data.getCurrentPosition());

        switch (mThemeBrand) {
            case "brand0":
                setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT);
                setPageMargin(getResources().getDimensionPixelOffset(R.dimen.media_widget_pager_dimen));
                break;
            case "brand1":
                setOffscreenPageLimit(OFFSCREEN_PAGE_LIMIT_BRAND1);
                setPageMargin(getResources().getDimensionPixelOffset(R.dimen.media_widget_pager_dimen_brand1));
                break;
        }

        setClipChildren(false);

        setPageTransformer(false, this);
        addOnPageChangeListener(mOnPageChangeListener);
    }

    @Override
    public void transformPage(@NonNull View view, float position) {
        ViewPager viewPager = (ViewPager) view.getParent();
        int leftInScreen = view.getLeft() - viewPager.getScrollX();
        int centerXInViewPager = leftInScreen + view.getMeasuredWidth() / 2;
        int offsetX = centerXInViewPager - viewPager.getMeasuredWidth() / 2;
        float offsetRate = (float) offsetX / viewPager.getMeasuredWidth();
        float scaleFactor = 1 - Math.abs(offsetRate);

        if (scaleFactor > 0) {
            view.setScaleX((float) (scaleFactor * PAGE_SCALE_FACTOR_COEFFICIENT));
            view.setScaleY((float) (scaleFactor * PAGE_SCALE_FACTOR_COEFFICIENT));
            view.setTranslationX(-offsetRate);

            view.setAlpha(1f);

            switch (mThemeBrand) {
                case "brand0":
                    break;
                case "brand1":
                    view.setRotationY(180 * offsetRate);

                    if (Math.abs(scaleFactor) < 1f) {
                        // hide image, show just background
                        ((ViewGroup) view).getChildAt(0).setVisibility(INVISIBLE);

                        // define side elements
                        if (scaleFactor < 0.7) {
                            view.setBackgroundColor(Color.TRANSPARENT);
                        } else if (scaleFactor < 0.85) {
                            view.setBackgroundColor(getResources().getColor(R.color.psa_media_widget_back_item_background_2));
                        } else {
                            view.setBackgroundColor(getResources().getColor(R.color.psa_media_widget_back_item_background_1));
                        }
                    } else {
                        ((ViewGroup) view).getChildAt(0).setVisibility(VISIBLE);
                    }
                    break;
            }
        } else {
            view.setAlpha(0.5f);
        }

        switch (mThemeBrand) {
            case "brand0":
                ViewCompat.setElevation(view, scaleFactor);
                break;
            case "brand1":
                ViewCompat.setElevation(view, scaleFactor);
                view.setOutlineProvider(null);
                break;
        }
    }

    private class MediaRunnable implements Runnable {
        @Override
        public void run() {
            if (!mScrolled) {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(MediaConstants.MEDIA_PLAYER_PACKAGE, MediaConstants.MEDIA_PLAYER_CLASS));
                //original flags were FLAG_ACTIVITY_NEW_TASK and FLAG_ACTIVITY_MULTIPLE_TASK but if use only this flag we will have many instance of Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
                        Intent.FLAG_ACTIVITY_NO_HISTORY);

                getContext().startActivity(intent);
            }
        }
    }
}

