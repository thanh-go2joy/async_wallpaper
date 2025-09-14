package com.go2joy.async_wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import android.util.Pair;

/** AsyncWallpaperPlugin */
public class AsyncWallpaperPlugin implements FlutterPlugin, MethodCallHandler {
  private MethodChannel channel;
  static Result res;
  private Context context;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "async_wallpaper");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    res = result;
    if (call.method.equals("setWallpaper")) {
      byte[] imageData = call.argument("imageData");
      String wallpaperType = call.argument("wallpaperType");

      if (imageData != null && wallpaperType != null) {
        Bitmap bitmap = Utils.bytesToBitmap(imageData);
        new SetWallPaperTask(context).execute(new Pair<>(bitmap, wallpaperType));
      } else {
        result.error("INVALID_ARGUMENTS", "Image data or wallpaper type missing", null);
      }
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }
}

/**
 * Task async set wallpaper
 */
class SetWallPaperTask extends AsyncTask<Pair<Bitmap, String>, Boolean, Boolean> {

  private final Context mContext;

  public SetWallPaperTask(final Context context) {
    mContext = context;
  }

  @Override
  protected final Boolean doInBackground(Pair<Bitmap, String>... pairs) {
    WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
    String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";

    try {
      switch (pairs[0].second) {
        case "1": { // default
          if (needChooser(manufacturer)) {
            openChooser(pairs[0].first, "Apply as:");
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "2": { // lock only
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_LOCK);
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "3": { // home only
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (needChooser(manufacturer)) {
              openChooser(pairs[0].first, "Apply as Home Screen:");
            } else {
              wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_SYSTEM);
            }
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "4": { // both
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(
                pairs[0].first,
                null,
                true,
                WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM
            );
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
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

  private boolean needChooser(String manufacturer) {
    return manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("vivo");
  }

  private void openChooser(Bitmap bitmap, String title) throws IOException {
    Uri uri = getImageUri(mContext, bitmap);
    Intent setWall = new Intent(Intent.ACTION_ATTACH_DATA);
    setWall.setDataAndType(uri, "image/*");
    setWall.putExtra("mimeType", "image/*");
    setWall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    setWall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    Intent chooser = Intent.createChooser(setWall, title);
    chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    mContext.startActivity(chooser);
  }

  private Uri getImageUri(Context context, Bitmap bitmap) throws IOException {
    File cachePath = new File(context.getCacheDir(), "images");
    if (!cachePath.exists()) cachePath.mkdirs();
    File file = new File(cachePath, "wallpaper.png");
    FileOutputStream stream = new FileOutputStream(file);
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    stream.close();
    return FileProvider.getUriForFile(
        context,
        context.getPackageName() + ".fileprovider",
        file
    );
  }
}
