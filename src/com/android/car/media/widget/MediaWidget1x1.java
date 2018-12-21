package com.android.car.media.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.android.car.media.MediaActivity;
import com.android.car.media.R;
import com.android.car.media.Utils;
import com.harman.psa.widget.verticallist.model.ItemData;
import com.harman.psa.widget.widget.PSAIconWidget;


public class MediaWidget1x1 extends PSAIconWidget {

    public static final String PLAY_FROM_WIDGET = "play_from_widget";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, MediaActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        setOnClickIntent(pendingIntent);
        setImageResource(R.drawable.ic_music);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int
            appWidgetId, ItemData itemData) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), com.harman.psa.widget
                .R.layout.psa_icon_widget);

        Intent intent = new Intent(context, MediaActivity.class);
        intent.putExtra(PLAY_FROM_WIDGET, itemData.getExtras());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(com.harman.psa.widget.R.id.root, pendingIntent);
        Bitmap bitmap = Utils.getBitmapIcon(context, itemData.getAction1DrawableUri());
        if (bitmap != null) {
            remoteViews.setImageViewBitmap(com.harman.psa.widget.R.id.widget_icon, bitmap);
        } else {
            Bundle extras = itemData.getExtras();
            if (extras != null && extras.containsKey(SubFragment.TYPE_ITEM)) {
                String type = extras.getString(SubFragment.TYPE_ITEM);
                switch (type) {
                    case SubFragment.TYPE_ITEM_ALBUM:
                        remoteViews.setImageViewResource(com.harman.psa.widget.R.id.widget_icon,
                                R.drawable.psa_album_default_art);
                        break;
                    case SubFragment.TYPE_ITEM_ARTIST:
                        remoteViews.setImageViewResource(com.harman.psa.widget.R.id.widget_icon,
                                R.drawable.psa_artist_default_art);
                        break;
                    default:
                        remoteViews.setImageViewResource(com.harman.psa.widget.R.id.widget_icon,
                                R.drawable.psa_track_default_art);
                        break;
                }
            } else {
                remoteViews.setImageViewResource(com.harman.psa.widget.R.id.widget_icon, R.drawable
                        .psa_track_default_art);
            }

        }


        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }
}
