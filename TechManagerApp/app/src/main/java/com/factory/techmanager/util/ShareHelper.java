package com.factory.techmanager.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Wraps Android sharing intents: file share (WhatsApp-first with chooser fallback) and wa.me text links. */
public class ShareHelper {

    public static void shareFileToWhatsApp(Context ctx, File file, String mime, String caption) {
        Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        if (caption != null) intent.putExtra(Intent.EXTRA_TEXT, caption);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setPackage("com.whatsapp");
        try {
            ctx.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            shareFileGeneric(ctx, uri, mime, caption);
        }
    }

    public static void shareFileGeneric(Context ctx, File file, String mime, String caption) {
        Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", file);
        shareFileGeneric(ctx, uri, mime, caption);
    }

    private static void shareFileGeneric(Context ctx, Uri uri, String mime, String caption) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        if (caption != null) intent.putExtra(Intent.EXTRA_TEXT, caption);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            ctx.startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, Lang.t("لا يوجد تطبيق للمشاركة", "No app available to share"), Toast.LENGTH_SHORT).show();
        }
    }

    /** Opens wa.me with a pre-filled text message (asks the user to pick a chat). */
    public static void sendWhatsAppText(Context ctx, String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.name());
            Uri uri = Uri.parse("https://wa.me/?text=" + encoded);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            ctx.startActivity(intent);
        } catch (Exception e) {
            shareTextGeneric(ctx, text);
        }
    }

    public static void shareTextGeneric(Context ctx, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            ctx.startActivity(Intent.createChooser(intent, null));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(ctx, Lang.t("لا يوجد تطبيق للمشاركة", "No app available to share"), Toast.LENGTH_SHORT).show();
        }
    }
}
