package com.codenameakshay.async_wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class AsyncWallpaperPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private static final String CHANNEL = "async_wallpaper";
    private MethodChannel channel;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("setWallpaper")) {
            String filePath = call.argument("filePath");
            int wallpaperLocation = call.argument("wallpaperLocation"); // HOME / LOCK / BOTH
            setWallpaper(filePath, wallpaperLocation, result);
        } else {
            result.notImplemented();
        }
    }

    private void setWallpaper(String filePath, int wallpaperLocation, Result result) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                result.error("FILE_NOT_FOUND", "Wallpaper file not found", null);
                return;
            }

            String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
            Log.d("AsyncWallpaper", "OEM = " + manufacturer);

            // Dùng chooser cho Oppo, Realme, Vivo (tránh reset/đen màn hình)
            if (manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("vivo")) {

                Uri uri = FileProvider.getUriForFile(activity,
                        activity.getPackageName() + ".fileprovider", file);

                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                intent.setDataAndType(uri, "image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                activity.startActivity(Intent.createChooser(intent, "Set wallpaper using"));
                result.success(true);
                return;
            }

            // Các hãng khác: set trực tiếp
            WallpaperManager wm = WallpaperManager.getInstance(activity);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (wallpaperLocation == 1) { // HOME
                    wm.setBitmap(android.graphics.BitmapFactory.decodeFile(filePath), null, true,
                            WallpaperManager.FLAG_SYSTEM);
                } else if (wallpaperLocation == 2) { // LOCK
                    wm.setBitmap(android.graphics.BitmapFactory.decodeFile(filePath), null, true,
                            WallpaperManager.FLAG_LOCK);
                } else { // BOTH
                    wm.setBitmap(android.graphics.BitmapFactory.decodeFile(filePath));
                }
            } else {
                wm.setBitmap(android.graphics.BitmapFactory.decodeFile(filePath));
            }
            result.success(true);

        } catch (IOException e) {
            result.error("SET_FAILED", "Failed to set wallpaper: " + e.getMessage(), null);
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }
}
