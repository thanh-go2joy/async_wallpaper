package com.thanh.async_wallpaper;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.InputStream;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class AsyncWallpaperPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    private Context context;
    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        context = flutterPluginBinding.getApplicationContext();
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "async_wallpaper");
        channel.setMethodCallHandler(this);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("setWallpaper")) {
            String filePath = call.argument("filePath");
            int location = call.argument("location");

            if (filePath == null) {
                result.error("INVALID_PATH", "File path is null", null);
                return;
            }

            try {
                File file = new File(filePath);
                Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);

                if (isOppoOrVivo()) {
                    // 👉 fallback chooser để tránh reset app
                    Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.setDataAndType(uri, "image/*");
                    intent.putExtra("mimeType", "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (activity != null) {
                        activity.startActivity(Intent.createChooser(intent, "Set as:"));
                        result.success(true);
                    } else {
                        result.error("NO_ACTIVITY", "Activity is null", null);
                    }
                } else {
                    // 👉 auto set trực tiếp
                    ContentResolver cr = context.getContentResolver();
                    InputStream is = cr.openInputStream(uri);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    WallpaperManager wm = WallpaperManager.getInstance(context);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wm.setBitmap(bitmap, null, true, location);
                    } else {
                        wm.setBitmap(bitmap);
                    }
                    result.success(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                result.error("SET_FAILED", e.getMessage(), null);
            }
        } else {
            result.notImplemented();
        }
    }

    private boolean isOppoOrVivo() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("oppo") || manufacturer.contains("vivo");
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.activity = null;
    }
}
