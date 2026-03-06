package com.example.vivofontswapper.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * 从 Uri 解析实际文件路径，或复制到 cache 目录返回可用路径
 */
public class RealPathUtil {

    private static final String TAG = "RealPathUtil";

    public static String getRealPath(Context context, Uri uri) {
        if (uri == null) return null;

        // content:// scheme
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String authority = uri.getAuthority();

                if ("com.android.externalstorage.documents".equals(authority)) {
                    String[] split = docId.split(":");
                    if ("primary".equalsIgnoreCase(split[0])) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if ("com.android.providers.downloads.documents".equals(authority)) {
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.parseLong(docId));
                    return queryColumn(context, contentUri, MediaStore.MediaColumns.DATA);
                } else if ("com.android.providers.media.documents".equals(authority)) {
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    else if ("video".equals(type)) contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    else if ("audio".equals(type)) contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    if (contentUri != null) {
                        return queryColumn(context, contentUri, MediaStore.MediaColumns.DATA,
                                "_id=?", new String[]{split[1]});
                    }
                }
            }
            // Try direct query
            String path = queryColumn(context, uri, MediaStore.MediaColumns.DATA);
            if (path != null) return path;
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // Fallback: copy to cache and return cache path
        return copyToCacheAndGetPath(context, uri);
    }

    private static String queryColumn(Context context, Uri uri, String column) {
        return queryColumn(context, uri, column, null, null);
    }

    private static String queryColumn(Context context, Uri uri, String column,
                                       String selection, String[] selectionArgs) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{column}, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "queryColumn failed", e);
        }
        return null;
    }

    private static String copyToCacheAndGetPath(Context context, Uri uri) {
        try {
            String fileName = getFileNameFromUri(context, uri);
            if (fileName == null) fileName = "font_" + System.currentTimeMillis() + ".ttf";

            File cacheFile = new File(context.getCacheDir(), fileName);
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {
                if (in == null) return null;
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            return cacheFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "copyToCacheAndGetPath failed", e);
            return null;
        }
    }

    private static String getFileNameFromUri(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception e) {
            Log.e(TAG, "getFileNameFromUri failed", e);
        }
        return null;
    }
}
