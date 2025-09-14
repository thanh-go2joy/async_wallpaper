package com.codenameakshay.async_wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

public class AsyncWallpaperPlugin implements MethodCallHandler {
    private final Context context;

    private AsyncWallpaperPlugin(Context context) {
        this.context = context;
    }

    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "async_wallpaper");
        channel.setMethodCallHandler(new AsyncWallpaperPlugin(registrar.context()));
    }

    @Override
    public void onMethodCall(MethodCall call, @NonNull Result result) {
        if (call.method.equals("setWallpaper")) {
            String url = call.argument("url");
            int wallpaperLocation = call.argument("wallpaperLocation");

            if (url == null) {
                result.error("INVALID_URL", "URL cannot be null", null);
                return;
            }

            try {
                File file = new File(url);
                if (!file.exists()) {
                    result.error("FILE_NOT_FOUND", "File not found at: " + url, null);
                    return;
                }

                Uri uri = Uri.fromFile(file);

                // ✅ Detect OEM để tránh reset app
                String manufacturer = Build.MANUFACTURER.toLowerCase();
                if (manufacturer.contains("oppo") ||
                    manufacturer.contains("realme") ||
                    manufacturer.contains("vivo")) {

                    // 👉 Luôn fallback sang chooser intent
                    Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
                    intent.setDataAndType(uri, "image/*");
                    intent.putExtra("mimeType", "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(intent, "Set as:"));

                    result.success(true);
                    return;
                }

                // 👉 Default: set bằng WallpaperManager
                WallpaperManager wm = WallpaperManager.getInstance(context);
                if (wallpaperLocation == 1) {
                    wm.setStream(context.getContentResolver().openInputStream(uri), null, true, WallpaperManager.FLAG_SYSTEM);
                } else if (wallpaperLocation == 2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setStream(context.getContentResolver().openInputStream(uri), null, true, WallpaperManager.FLAG_LOCK);
                } else if (wallpaperLocation == 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    wm.setStream(context.getContentResolver().openInputStream(uri), null, true,
                            WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                } else {
                    wm.setStream(context.getContentResolver().openInputStream(uri));
                }

                result.success(true);

            } catch (IOException e) {
                result.error("IO_ERROR", e.getMessage(), null);
            } catch (Exception e) {
                result.error("ERROR", e.getMessage(), null);
            }
        } else {
            result.notImplemented();
        }
    }
}
