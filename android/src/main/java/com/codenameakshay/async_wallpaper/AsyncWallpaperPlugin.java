package com.codenameakshay.async_wallpaper;

import android.app.Activity;
import android.app.Application;
import android.app.WallpaperManager;
import android.graphics.BitmapFactory;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * AsyncWallpaperPlugin
 */
public class AsyncWallpaperPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private MethodChannel channel;
    public static Context context;
    private Activity activity;
    public static MethodChannel.Result res;

    private boolean redirectToLiveWallpaper;
    private boolean goToHome;

    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
            Log.i("Arguments ", "Image Downloaded (default)");
            SetWallPaperTask setWallPaperTask = new SetWallPaperTask(context);
            setWallPaperTask.execute(new Pair<>(resource, "1"));
        }
        @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
    };
    private Target target1 = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
            Log.i("Arguments ", "Image Downloaded (lock)");
            SetWallPaperTask setWallPaperTask = new SetWallPaperTask(context);
            setWallPaperTask.execute(new Pair<>(resource, "2"));
        }
        @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
    };
    private Target target2 = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
            Log.i("Arguments ", "Image Downloaded (home)");
            SetWallPaperTask setWallPaperTask = new SetWallPaperTask(context);
            setWallPaperTask.execute(new Pair<>(resource, "3"));
        }
        @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
    };
    private Target target3 = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap resource, Picasso.LoadedFrom from) {
            Log.i("Arguments ", "Image Downloaded (both)");
            SetWallPaperTask setWallPaperTask = new SetWallPaperTask(context);
            setWallPaperTask.execute(new Pair<>(resource, "4"));
        }
        @Override public void onBitmapFailed(Exception e, Drawable errorDrawable) {}
        @Override public void onPrepareLoad(Drawable placeHolderDrawable) {}
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "async_wallpaper");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        redirectToLiveWallpaper = false;
        goToHome = false;
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
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        if (redirectToLiveWallpaper && goToHome) {
            home();
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        activity = null;
    }

    public void home() {
        if (activity != null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        res = result;
        String url = call.argument("url");
        goToHome = call.argument("goToHome") != null && (Boolean) call.argument("goToHome");

        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + Build.VERSION.RELEASE);
                break;

            case "set_wallpaper":
                Picasso.get().load(url).into(target);
                break;

            case "set_wallpaper_file":
                Picasso.get().load("file://" + url).into(target);
                break;

            case "set_lock_wallpaper":
                Picasso.get().load(url).into(target1);
                if (goToHome) home();
                break;

            case "set_home_wallpaper":
                Picasso.get().load(url).into(target2);
                if (goToHome) home();
                break;

            case "set_both_wallpaper":
                Picasso.get().load(url).into(target3);
                if (goToHome) home();
                break;

            case "set_lock_wallpaper_file":
                Picasso.get().load("file://" + url).into(target1);
                if (goToHome) home();
                break;

            case "set_home_wallpaper_file":
                Picasso.get().load("file://" + url).into(target2);
                if (goToHome) home();
                break;

            case "set_both_wallpaper_file":
                Picasso.get().load("file://" + url).into(target3);
                if (goToHome) home();
                break;

            case "set_video_wallpaper":
                copyFile(new File(url), new File(activity.getFilesDir(), "file.mp4"));
                redirectToLiveWallpaper = false;
                VideoLiveWallpaper videoLiveWallpaper = new VideoLiveWallpaper();
                videoLiveWallpaper.setToWallPaper(context);
                result.success(true);
                break;

            case "open_wallpaper_chooser":
                VideoLiveWallpaper chooser = new VideoLiveWallpaper();
                chooser.openWallpaperChooser(context);
                result.success(true);
                break;

            default:
                result.notImplemented();
        }
    }

    public void copyFile(File fromFile, File toFile) {
        try (FileInputStream in = new FileInputStream(fromFile);
             FileOutputStream out = new FileOutputStream(toFile);
             FileChannel inChannel = in.getChannel();
             FileChannel outChannel = out.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class SetWallPaperTask extends AsyncTask<Pair<Bitmap, String>, Boolean, Boolean> {
    private final Context mContext;

    public SetWallPaperTask(final Context context) {
        mContext = context;
    }

    @Override
    protected final Boolean doInBackground(Pair<Bitmap, String>... pairs) {
        String mode = pairs[0].second;
        Bitmap bitmap = pairs[0].first;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);

        try {
            switch (mode) {
                case "1": // default
                    wallpaperManager.setBitmap(bitmap);
                    break;
                case "2": // lock
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    }
                    break;
                case "3": // home
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                    }
                    break;
                case "4": // both
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        wallpaperManager.setBitmap(bitmap, null, true,
                                WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM);
                    }
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        AsyncWallpaperPlugin.res.success(aBoolean);
    }
}
