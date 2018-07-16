import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/services.dart';

class FlutterPdfViewer {
  static const MethodChannel _channel =
      const MethodChannel('flutter_pdf_viewer');

  static Future<void> loadFilePath(
    String filePath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  }) =>
      _channel.invokeMethod(
        'fromFile',
        {
          'filePath': filePath,
          'password': password,
          'nightMode': nightMode,
          'xorDecryptKey': xorDecryptKey,
        },
      );

  static Future<void> loadBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  }) =>
      _channel.invokeMethod(
        'fromBytes',
        {
          'pdfBytes': pdfBytes,
          'password': password,
          'nightMode': nightMode,
          'xorDecryptKey': xorDecryptKey,
        },
      );

  static Future<void> loadAsset(
    String assetPath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  }) =>
      _channel.invokeMethod(
        'fromAsset',
        {
          'assetPath': assetPath,
          'password': password,
          'nightMode': nightMode,
          'xorDecryptKey': xorDecryptKey,
        },
      );
}
