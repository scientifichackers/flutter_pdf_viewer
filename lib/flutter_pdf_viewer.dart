import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';

import 'downloader.dart' as downloader;

class FlutterPdfViewer {
  static const MethodChannel _channel =
      const MethodChannel('flutter_pdf_viewer');

  static const downloadAsBytes = downloader.downloadAsBytes;
  static const downloadAsFile = downloader.downloadAsFile;

  static Future<void> loadFilePath(
    String filePath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
  }) {
    return _channel.invokeMethod(
      'fromFile',
      {
        'filePath': 'file://' + filePath,
        'password': password,
        'nightMode': nightMode,
        'xorDecryptKey': xorDecryptKey,
        'swipeHorizontal': swipeHorizontal,
      },
    );
  }

  static Future<void> loadBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
  }) async {
    int pdfBytesSize = pdfBytes.length;

    ServerSocket server = await ServerSocket.bind('0.0.0.0', 4567);
    server.listen(
      (Socket client) {
        client.add(pdfBytes);
        client.close();
        server.close();
      },
    );

    await _channel.invokeMethod(
      'fromBytes',
      {
        'pdfBytesSize': pdfBytesSize,
        'password': password,
        'nightMode': nightMode,
        'xorDecryptKey': xorDecryptKey,
        'swipeHorizontal': swipeHorizontal,
      },
    );
  }

  static Future<void> loadAsset(
    String assetPath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
  }) {
    return _channel.invokeMethod(
      'fromAsset',
      {
        'assetPath': assetPath,
        'password': password,
        'nightMode': nightMode,
        'xorDecryptKey': xorDecryptKey,
        'swipeHorizontal': swipeHorizontal,
      },
    );
  }
}
