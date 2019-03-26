package com.android.car.media.widget.ui;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import android.net.Uri;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
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

    private static final String TAG = "CoverFlowPager";

    private static final int OFFSCREEN_PAGE_LIMIT = 4;
    private static final int OFFSCREEN_PAGE_LIMIT_BRAND1 = 5;
    private static final float PAGE_SCALE_FACTOR_COEFFICIENT = 0.6f;

    private int mCurrentPosition = 0;
    private boolean mScrolled = false;
    private boolean mNeedUpdatePosition = false;
    private Handler mHandler = new Handler();
    protected String mThemeBrand = "brand0";

    private CoverFlowAdapter mPageAdapter;

    private CoverFlowSelectionListener mSelectionListener;

    public interface CoverFlowSelectionListener {
        void selectNext();
        void selectPrev();
    }

    private OnPageChangeListener mOnPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mCurrentPosition = getCurrentItem();
            mScrolled = true;
        }

        @Override
        public void onPageSelected(int position) {
            if (! mNeedUpdatePosition) {
                if (position > mCurrentPosition) {
                    if (mSelectionListener != null) {
                        mSelectionListener.selectNext();
                    }
                } else if (position < mCurrentPosition) {
                    if (mSelectionListener != null) {
                        mSelectionListener.selectPrev();
                    }
                }
                mCurrentPosition = position;
            }else{
                mCurrentPosition = getCurrentItem();
                mNeedUpdatePosition = false;
            }
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

    @Override
    public void setCurrentItem(int position){
        setCurrentItemPrivate(position, false, false);
    }
    @Override
    public void setCurrentItem(int position, boolean smooothScroll){
        setCurrentItemPrivate(position, smooothScroll, false);
    }

    private void setCurrentItemPrivate(int position, boolean smoothScroll, boolean manual){
        mNeedUpdatePosition = !manual;
        super.setCurrentItem(position, smoothScroll);
    }


    public void refreshData(@Nullable CarMediaWidgetData widgetData){
        if (widgetData == null){
            mPageAdapter = new CoverFlowAdapter(getContext(), new ArrayList<Uri>(), clickListener);
            setAdapter(mPageAdapter);
        }else {
            mPageAdapter = new CoverFlowAdapter(getContext(), widgetData.getUrlList(), clickListener);
            setAdapter(mPageAdapter);
            setCurrentItemPrivate(widgetData.getCurrentPosition(), false, false);
        }
    }

    private CoverFlowAdapter.OnClickItemListener clickListener = new CoverFlowAdapter.OnClickItemListener() {
        @Override
        public void onClickItem(int position) {
            if (mCurrentPosition == position) {
                mScrolled = false;
            }
            mNeedUpdatePosition = false;
            setCurrentItemPrivate(position, true, true);
        }
    };

    private void init() {
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

    public void clear(){
        CarMediaWidgetData data = new CarMediaWidgetData();
        mPageAdapter = new CoverFlowAdapter(getContext(), data.getUrlList(), clickListener);
        setAdapter(mPageAdapter);
        setCurrentItemPrivate(data.getCurrentPosition(), false, false);
    }

    public void setCoverFlowSelectionListener(CoverFlowSelectionListener listener){
        mSelectionListener = listener;
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
}

