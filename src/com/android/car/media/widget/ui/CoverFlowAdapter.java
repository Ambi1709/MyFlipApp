package com.android.car.media.widget.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.viewpager.widget.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.car.media.Utils;
import com.android.car.media.R;


import java.util.List;


public class CoverFlowAdapter extends PagerAdapter {

    private static final String TAG = "CoverFlowAdapter";

    public interface OnClickItemListener {
        void onClickItem(int position);
    }

    private Context mContext;
    private List<Uri> mList;
    private OnClickItemListener mListener;

    public CoverFlowAdapter(Context context, List<Uri> listItems, OnClickItemListener listener) {
        mContext = context;
        mList = listItems;
        mListener = listener;
    }

    public void updateList(List<Uri> listItems){
        mList = listItems;
        notifyDataSetChanged();
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_cover, null);

        LinearLayout linMain = view.findViewById(R.id.linMain);
        ImageView imageCover = view.findViewById(R.id.imageCover);
        linMain.setTag(position);

        Bitmap bitmap = Utils.getBitmapIcon(mContext, mList.get(position));
        if (bitmap != null) {
            imageCover.setImageBitmap(bitmap);
        } else {
            imageCover.setImageResource(R.drawable.psa_track_default_art);
        }

        container.addView(view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onClickItem(position);
                }
            }
        });

        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return (view == object);
    }


}