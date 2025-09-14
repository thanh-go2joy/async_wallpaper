package com.go2joy.async_wallpaper;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
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

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "async_wallpaper");
    channel.setMethodCallHandler(this);
    context = flutterPluginBinding.getApplicationContext();
  }

  private Context context;

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
        case "1": {
          // Home + Lock (default)
          if (manufacturer.contains("oppo") ||
              manufacturer.contains("realme") ||
              manufacturer.contains("vivo")) {

            Uri tempUri = getImageUri(mContext, pairs[0].first);
            Intent setWall = new Intent(Intent.ACTION_ATTACH_DATA);
            setWall.setDataAndType(tempUri, "image/*");
            setWall.putExtra("mimeType", "image/*");
            setWall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            setWall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Intent chooser = Intent.createChooser(setWall, "Apply as:");
            chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(chooser);

          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "2": {
          // Lock screen only
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_LOCK);
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "3": {
          // Home screen only
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (manufacturer.contains("oppo") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("vivo")) {
              // fallback chooser
              Uri tempUri = getImageUri(mContext, pairs[0].first);
              Intent setWall = new Intent(Intent.ACTION_ATTACH_DATA);
              setWall.setDataAndType(tempUri, "image/*");
              setWall.putExtra("mimeType", "image/*");
              setWall.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
              setWall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              Intent chooser = Intent.createChooser(setWall, "Apply as Home Screen:");
              chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              mContext.startActivity(chooser);
            } else {
              wallpaperManager.setBitmap(pairs[0].first, null, true, WallpaperManager.FLAG_SYSTEM);
            }
          } else {
            wallpaperManager.setBitmap(pairs[0].first);
          }
          break;
        }
        case "4": {
          // Both home & lock
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

  private Uri getImageUri(Context inContext, Bitmap inImage) {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
    String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "wallpaper", null);
    return Uri.parse(path);
  }
}
