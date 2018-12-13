package com.android.car.media.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.android.car.media.MediaActivity;
import com.android.car.media.R;
import com.android.car.media.Utils;
import com.harman.psa.widget.widget.PSAIconText2x1Widget;

import java.util.List;

public class MediaWidget2x1 extends PSAIconText2x1Widget {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.startsWith(Constants.LOCAL_MEDIA_PLAYER_PACKAGE)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisAppWidget = new ComponentName(context, this.getClass().getName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);
            update(context, appWidgetManager, appWidgetIds, intent.getExtras());
        } else {
            super.onReceive(context, intent);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, null);
    }

    private void update(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds,
                        Bundle extras) {
        Intent intent = new Intent(context, MediaActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        setOnClickIntent(pendingIntent);

        if (extras != null) {
            if (extras.containsKey(Constants.ARGUMENT_PLAYER_TITLE)) {
                setPrimaryText(extras.getString(Constants.ARGUMENT_PLAYER_TITLE));
            }

            if (extras.containsKey(Constants.ARGUMENT_PLAYER_SUB_TITLE)) {
                setSecondaryText(extras.getString(Constants.ARGUMENT_PLAYER_SUB_TITLE));
            }

            if (extras.containsKey(Constants.ARGUMENT_PLAYER_ICON)) {
                List<String> urlList = extras.getStringArrayList(Constants.ARGUMENT_PLAYER_ICON);

                int position = -1;

                if (extras.containsKey(Constants.ARGUMENT_PLAYER_ICON_POSITION)) {
                    position = extras.getInt(Constants.ARGUMENT_PLAYER_ICON_POSITION);
                }

                String url = null;
                if (urlList != null && !urlList.isEmpty() && position >= 0) {
                    url = urlList.get(position);
                }

                if (url != null && !url.isEmpty()) {
                    Bitmap bitmap = Utils.getBitmapIcon(context, url);
                    if (bitmap != null) {
                        setImage(bitmap);
                    } else {
                        setImageResource(R.drawable.psa_track_default_art);
                    }
                } else {
                    setImageResource(R.drawable.psa_track_default_art);
                }
            } else {
                setImageResource(R.drawable.psa_track_default_art);
            }
        } else {
            setPrimaryText(context.getString(R.string.default_media_widget_text));
            setImageResource(R.drawable.psa_track_default_art);
        }


        setNightMode(true);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }
}
