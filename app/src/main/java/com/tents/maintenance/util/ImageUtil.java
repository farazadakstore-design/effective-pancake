package com.tents.maintenance.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Photo capture/import pipeline for fault & AC-unit photos. Mirrors the original
 * web app's compressImage(file, 900, 0.6): downscale to max width 900px and
 * re-encode as JPEG quality ~60, so a full report with several photos stays small
 * enough to embed in a PDF and share over WhatsApp.
 */
public class ImageUtil {

    private static final int MAX_WIDTH = 900;
    private static final int JPEG_QUALITY = 60;
    private static final int DECODE_BOUND = 1600;

    public static String savePhoto(Context ctx, Uri sourceUri, String prefix) throws IOException {
        Bitmap bitmap = decodeSampledBitmap(ctx, sourceUri, DECODE_BOUND);
        if (bitmap == null) throw new IOException("Could not decode image");
        bitmap = fixOrientation(ctx, sourceUri, bitmap);
        bitmap = resizeMaxWidth(bitmap, MAX_WIDTH);

        File dir = new File(ctx.getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, prefix + "_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
        }
        return out.getAbsolutePath();
    }

    public static File newCaptureFile(Context ctx) throws IOException {
        File dir = new File(ctx.getCacheDir(), "images");
        if (!dir.exists()) dir.mkdirs();
        return File.createTempFile("capture_", ".jpg", dir);
    }

    private static Bitmap decodeSampledBitmap(Context ctx, Uri uri, int reqSize) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, bounds);
        }
        int inSampleSize = 1;
        int halfW = bounds.outWidth / 2;
        int halfH = bounds.outHeight / 2;
        while ((halfW / inSampleSize) >= reqSize || (halfH / inSampleSize) >= reqSize) {
            inSampleSize *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Math.max(1, inSampleSize);
        try (InputStream is2 = ctx.getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(is2, null, opts);
        }
    }

    private static Bitmap fixOrientation(Context ctx, Uri uri, Bitmap bitmap) {
        try (InputStream is = ctx.getContentResolver().openInputStream(uri)) {
            if (is == null) return bitmap;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            float degrees = 0;
            boolean flip = false;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90: degrees = 90; break;
                case ExifInterface.ORIENTATION_ROTATE_180: degrees = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: degrees = 270; break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: flip = true; break;
                default: return bitmap;
            }
            Matrix m = new Matrix();
            if (degrees != 0) m.postRotate(degrees);
            if (flip) m.postScale(-1, 1);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
        } catch (Exception e) {
            return bitmap;
        }
    }

    private static Bitmap resizeMaxWidth(Bitmap src, int maxW) {
        if (src.getWidth() <= maxW) return src;
        int newH = Math.round(src.getHeight() * (maxW / (float) src.getWidth()));
        return Bitmap.createScaledBitmap(src, maxW, newH, true);
    }

    public static String saveBitmap(Context ctx, Bitmap bitmap, String prefix) throws IOException {
        File dir = new File(ctx.getFilesDir(), "photos");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, prefix + "_" + System.currentTimeMillis() + ".png");
        try (FileOutputStream fos = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return out.getAbsolutePath();
    }
}
