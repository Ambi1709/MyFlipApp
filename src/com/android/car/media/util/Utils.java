package com.android.car.media;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;

public class Utils {
    private static final String TAG = Utils.class.getSimpleName();
    public static final String[] PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    static Uri getUriForResource(Context context, int id) {
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
