package com.codenameakshay.async_wallpaper;

import android.app.Activity;
import android.app.Application;
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * AsyncWallpaperPlugin
 */
public class AsyncWallpaperPlugin extends Application implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    public static android.content.Context context;
    private Activity activity;
    public static MethodChannel.Result res;

    private boolean goToHome;
    private boolean safeMode = false; // mặc định = false

    private Target makeTarget(final String flag) {
        return new Target() {
            @Override
            public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
                SetWallPaperTask task = new SetWallPaperTask(context, safeMode);
                task.execute(new Pair(resource, flag));
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) { }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) { }
        };
    }

    private Target target = makeTarget("1");
    private Target target1 = makeTarget("2");
    private Target target2 = makeTarget("3");
    private Target target3 = makeTarget("4");

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "async_wallpaper");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        goToHome = false;
    }

    @Override
    public void onDetachedFromActivity() { }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding flutterPluginBinding) { }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding flutterPluginBinding) {
        activity = flutterPluginBinding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() { }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        res = result;
        String url;

        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;

            case "set_wallpaper":
            case "set_wallpaper_file":
            case "set_lock_wallpaper":
            case "set_home_wallpaper":
            case "set_both_wallpaper":
                url = call.argument("url");
                goToHome = call.argument("goToHome");
                Boolean safe = call.argument("safeMode");
                safeMode = (safe != null && safe);

                if (call.method.equals("set_wallpaper")) {
                    Picasso.get().load(url).into(target);
                } else if (call.method.equals("set_wallpaper_file")) {
                    Picasso.get().load("file://" + url).into(target);
                } else if (call.method.equals("set_lock_wallpaper")) {
                    Picasso.get().load(url).into(target1);
                } else if (call.method.equals("set_home_wallpaper")) {
                    Picasso.get().load(url).into(target2);
                } else if (call.method.equals("set_both_wallpaper")) {
                    Picasso.get().load(url).into(target3);
                }
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}

class SetWallPaperTask extends AsyncTask<Pair<Bitmap, String>, Boolean, Boolean> {
    private final android.content.Context mContext;
    private final boolean safeMode;

    public SetWallPaperTask(final android.content.Context context, boolean safeMode) {
        mContext = context;
        this.safeMode = safeMode;
    }

    @Override
    protected final Boolean doInBackground(Pair<Bitmap, String>... pairs) {
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
        try {
            switch (pairs[0].second) {
                case "1": // generic set
                    if (safeMode) openChooser(pairs[0].first);
                    else wallpaperManager.setBitmap(pairs[0].first);
                    break;

                case "2": // lock screen
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !safeMode) {
                        wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_LOCK);
                    } else {
                        openChooser(pairs[0].first);
                    }
                    break;

                case "3": // home screen
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !safeMode) {
                        wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_SYSTEM);
                    } else {
                        openChooser(pairs[0].first);
                    }
                    break;

                case "4": // both
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !safeMode) {
                        wallpaperManager.setBitmap(
                                pairs[0].first,
                                null,
                                true,
                                WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM
                        );
                    } else {
                        openChooser(pairs[0].first);
                    }
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        AsyncWallpaperPlugin.res.success(aBoolean);
    }

    // open chooser
    private void openChooser(Bitmap bitmap) {
        Uri tempUri = getImageUri(mContext, bitmap);
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.setDataAndType(tempUri, "image/*");
        intent.putExtra("mimeType", "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(Intent.createChooser(intent, "Set as Wallpaper"));
    }

    // convert bitmap -> Uri
    private Uri getImageUri(android.content.Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = android.provider.MediaStore.Images.Media.insertImage(
                inContext.getContentResolver(),
                inImage,
                "wallpaper_" + System.currentTimeMillis(),
                null
        );
        return Uri.parse(path);
    }
}
