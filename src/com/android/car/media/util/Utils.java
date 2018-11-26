package com.android.car.media;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;

public class Utils {

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
                return false;
            }
        }
        return true;
    }

}