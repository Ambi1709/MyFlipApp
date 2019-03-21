package com.android.car.media;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    public static final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // The default width and height for an image. These are used if the art view has not laid
    // out by the time a Bitmap needs to be created to fit in it.
    public static final int DEFAULT_ALBUM_ART_WIDTH = 320;
    public static final int DEFAULT_ALBUM_ART_HEIGHT = 320;

    /**
     * The preferred ordering for bitmap to fetch. The metadata at lower indexes are preferred to
     * those at higher indexes.
     */
    private static final String[] PREFERRED_BITMAP_TYPE_ORDER = {
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
    };

    public static Bitmap getMetadataBitmap(MediaMetadata metadata) {
        // Get the best art bitmap we can find
        for (String bitmapType : PREFERRED_BITMAP_TYPE_ORDER) {
            Bitmap bitmap = metadata.getBitmap(bitmapType);
            if (bitmap != null) {
                return bitmap;
            }
        }
        return null;
    }

    /**
     * The preferred ordering for metadata URIs to fetch. The metadata at lower indexes are
     * preferred to those at higher indexes.
     */
    private static final String[] PREFERRED_URI_ORDER = {
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
            MediaMetadata.METADATA_KEY_ART_URI,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
    };

    public static Uri getMetadataIconUri(MediaMetadata metadata, Context context) {
        // Get the best Uri we can find
        String iconUri = "";
        for (String bitmapUri : PREFERRED_URI_ORDER) {
            iconUri = metadata.getString(bitmapUri);
            if (!TextUtils.isEmpty(iconUri)) {
                return Uri.parse(iconUri);
            }
        }
        iconUri = Utils.getUriForResource(context, R.drawable.psa_media_playlist_default_icon).toString();
        return Uri.parse(iconUri);
    }

    public static Uri getUriForResource(Context context, int id) {
        Resources res = context.getResources();
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE
                + "://" + res.getResourcePackageName(id)
                + "/" + res.getResourceTypeName(id)
                + "/" + res.getResourceEntryName(id));
    }

    public static boolean hasRequiredPermissions(Context context) {
        for (String permission : PERMISSIONS) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Application hasn't all required permissions.");
                return false;
            }
        }
        return true;
    }

    public static Bitmap getBitmapIcon(Context context, String iconUri) {
        return getBitmapIcon(context, Uri.parse(iconUri));
    }

    public static Bitmap getBitmapIcon(Context context, Uri iconUri) {
        Bitmap bmp = null;
        if (iconUri != null) {
            try {
                bmp = MediaStore.Images.Media.getBitmap(context.getContentResolver(), iconUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bmp;
    }

}
