package com.android.car.media.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.content.ComponentName;
import android.view.View;
import com.android.car.media.MediaActivity;
import com.android.car.media.R;
import com.android.car.media.Utils;
import com.harman.psa.widget.verticallist.model.ItemData;
import com.harman.psa.widget.widget.PSAIconWidget;

import android.util.Log;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Parcelable;

public class MediaWidget1x1 extends PSAIconWidget {
    public static final String TAG = "MediaWidget1x1";
    public static final String PLAY_FROM_WIDGET = "play_from_widget";
    private static final String SP_PATH = "PATH";
    private static final String SP_TITLE = "TITLE";
    private static final String SP_SUBTITLE = "SUBTITLE";
    private static final String SP_KEY = "KEY";
    private static final String SP_ART = "ART";
    private static final String SP_VIEWTYPE = "VIEWTYPE";
    private static final String SP_RESID = "RESID";
    private static final String SP_TYPE = "TYPE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Intent intent = new Intent(context, MediaActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent
                .FLAG_UPDATE_CURRENT);
        setOnClickIntent(pendingIntent);

        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            ItemData itemData = getDataById(context, appWidgetId);
            if (itemData != null) {
                updateAppWidget(context, appWidgetManager, appWidgetId, itemData);
            }
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int
            appWidgetId, ItemData itemData) {
        saveData(context, appWidgetId, itemData);
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), com.harman.psa.widget
                .R.layout.psa_icon_widget);
        Intent intent = new Intent(context, MediaActivity.class);
        intent.putExtra(PLAY_FROM_WIDGET, itemData.getExtras());
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
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
            String primaryText = itemData.getPrimaryText();
            String secondaryText = itemData.getSecondaryText();
            remoteViews.setTextViewText(com.harman.psa.widget.R.id.primary_widget_text, primaryText);
            remoteViews.setTextViewText(com.harman.psa.widget.R.id.secondary_widget_text, secondaryText);
            if ((primaryText == null) || primaryText.isEmpty()) {
                remoteViews.setViewVisibility(com.harman.psa.widget.R.id.primary_widget_text, View.GONE);
            } else {
                remoteViews.setViewVisibility(com.harman.psa.widget.R.id.primary_widget_text, View.VISIBLE);
            }
            if ((secondaryText == null) || secondaryText.isEmpty()) {
                remoteViews.setViewVisibility(com.harman.psa.widget.R.id.secondary_widget_text, View.GONE);
            } else {
                remoteViews.setViewVisibility(com.harman.psa.widget.R.id.secondary_widget_text, View.VISIBLE);
            }

        }

        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static ItemData getDataById(Context context, int appWidgetId) {
        SharedPreferences sp = context.getSharedPreferences(Constants.LOCAL_MEDIA_PLAYER_PACKAGE, Context.MODE_PRIVATE);

        String key = sp.getString(SP_KEY + appWidgetId, null);
        if (key == null) return null;

        String title = sp.getString(SP_TITLE + appWidgetId, null);
        String subTitle = sp.getString(SP_SUBTITLE + appWidgetId, null);
        String albumArtUri = sp.getString(SP_ART + appWidgetId, null);
        int action1ViewType = sp.getInt(SP_VIEWTYPE + appWidgetId, 0);
        int action1ResId = sp.getInt(SP_RESID + appWidgetId, SubFragment.INVALID_ID);
        String path = sp.getString(SP_PATH + appWidgetId, null);
        String type = sp.getString(SP_TYPE + appWidgetId, null);

        Bundle bundle = new Bundle();
        bundle.putString(SubFragment.PATH, path);
        bundle.putString(SubFragment.ITEM_ID, key);
        bundle.putString(SubFragment.TYPE_ITEM, type);

        ItemData.Builder builder = new ItemData.Builder()
                .setId(key)
                .setPrimaryText(title)
                .setSecondaryText(subTitle)
                .setAction1DrawableUri(Uri.parse(albumArtUri))
                .setAction1ViewType(action1ViewType)
                .setAction1ResId(action1ResId);

        ItemData item = builder.build();
        item.setExtras(bundle);
        return item;
    }

    private static void saveData(Context context, int appWidgetId, ItemData itemData) {
        SharedPreferences sp = context.getSharedPreferences(Constants.LOCAL_MEDIA_PLAYER_PACKAGE, Context.MODE_PRIVATE);
        Editor edit = sp.edit();
        edit.putString(SP_KEY + appWidgetId, itemData.getId());
        edit.putString(SP_TITLE + appWidgetId, itemData.getPrimaryText());
        edit.putString(SP_SUBTITLE + appWidgetId, itemData.getSecondaryText());
        Uri uri = itemData.getAction1DrawableUri();
        if (uri != null) {
            edit.putString(SP_ART + appWidgetId, uri.toString());
        }
        edit.putInt(SP_VIEWTYPE + appWidgetId, itemData.getAction1ViewType());
        edit.putInt(SP_RESID + appWidgetId, itemData.getAction1ResId());

        Bundle bundle = itemData.getExtras();
        edit.putString(SP_PATH + appWidgetId, bundle.getString(SubFragment.PATH));
        edit.putString(SP_TYPE + appWidgetId, bundle.getString(SubFragment.TYPE_ITEM));

        edit.apply();
    }

    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences sp = context.getSharedPreferences(Constants.LOCAL_MEDIA_PLAYER_PACKAGE, Context.MODE_PRIVATE);
        Editor edit = sp.edit();

        for (int i = 0; i < appWidgetIds.length; i++) {
            int appWidgetId = appWidgetIds[i];
            edit.remove(SP_KEY + appWidgetId);
            edit.remove(SP_TITLE + appWidgetId);
            edit.remove(SP_SUBTITLE + appWidgetId);
            edit.remove(SP_ART + appWidgetId);
            edit.remove(SP_VIEWTYPE + appWidgetId);
            edit.remove(SP_RESID + appWidgetId);
            edit.remove(SP_PATH + appWidgetId);
            edit.remove(SP_TYPE + appWidgetId);
        }

        edit.apply();
    }
}
