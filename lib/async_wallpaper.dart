// ignore_for_file: constant_identifier_names

import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:fluttertoast/fluttertoast.dart';

class ToastDetails {
  final String message;
  final Color? backgroundColor;
  final double? fontSize;
  final ToastGravity? gravity;
  final Color? textColor;
  final Toast? toastLength;

  ToastDetails({
    required this.message,
    this.backgroundColor,
    this.fontSize,
    this.gravity,
    this.textColor,
    this.toastLength,
  });

  factory ToastDetails.success() {
    return ToastDetails(
      message: '😊 Wallpaper applied successfully.',
      backgroundColor: Colors.green,
    );
  }

  factory ToastDetails.wallpaperChooser() {
    return ToastDetails(
      message: '😊 Wallpaper chooser opened successfully.',
      backgroundColor: Colors.green,
    );
  }

  factory ToastDetails.error() {
    return ToastDetails(
      message: '😢 Wallpaper could not be applied.',
      backgroundColor: Colors.red,
    );
  }
}

class AsyncWallpaper {
  static const MethodChannel _channel = MethodChannel('async_wallpaper');

  static const int HOME_SCREEN = 1;
  static const int LOCK_SCREEN = 2;
  static const int BOTH_SCREENS = 3;

  /// Đặt wallpaper từ [filePath]
  static Future<bool> setWallpaper({
    required String filePath,
    int wallpaperLocation = BOTH_SCREENS,
  }) async {
    final result = await _channel.invokeMethod("setWallpaper", {
      "filePath": filePath,
      "location": wallpaperLocation,
    });
    return result == true;
  }
}
