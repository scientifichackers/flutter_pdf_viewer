import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';

import 'downloader.dart';

const int PDF_BYTES_PORT = 4567;

class Video {
  String mode;
  String xorDecryptKey;
  String src;

  Video.fromFile(
    String filePath, {
    String xorDecryptKey,
  }) {
    mode = "fromFile";
    src = filePath;
    this.xorDecryptKey = xorDecryptKey;
  }

  Video.fromAsset(
    String assetPath, {
    String xorDecryptKey,
  }) {
    mode = "fromAsset";
    src = assetPath;
    this.xorDecryptKey = xorDecryptKey;
  }

  Video.fromUrl(
    String url, {
    bool cache: true,
    Function onDownload,
    String xorDecryptKey,
  }) {
    mode = "fromUrl";
    src = url;
    this.xorDecryptKey = xorDecryptKey;

    downloadAsFile(url, cache: cache).then((filePath) {
      if (onDownload != null) onDownload(filePath);
    });
  }

  toJson() {
    return {'mode': mode, 'src': src, 'xorDecryptKey': xorDecryptKey};
  }
}

class PdfViewerConfig {
  String password;
  String xorDecryptKey;
  bool nightMode;
  bool enableSwipe;
  bool swipeHorizontal;
  bool autoSpacing;
  bool pageFling;
  bool pageSnap;
  bool enableImmersive;
  bool autoPlay;
  Map<int, Video> videoPages;

  PdfViewerConfig({
    this.password,
    this.xorDecryptKey,
    this.nightMode: false,
    this.enableSwipe: true,
    this.swipeHorizontal: false,
    this.autoSpacing: false,
    this.pageFling: false,
    this.pageSnap: false,
    this.enableImmersive: false,
    this.autoPlay: false,
    slideShow: false,
    this.videoPages,
  }) {
    if (slideShow) {
      swipeHorizontal = autoSpacing = pageFling = pageSnap = true;
    }
  }

  PdfViewerConfig copy() {
    return PdfViewerConfig(
      password: password,
      xorDecryptKey: xorDecryptKey,
      nightMode: nightMode,
      enableSwipe: enableSwipe,
      swipeHorizontal: swipeHorizontal,
      autoSpacing: autoSpacing,
      pageFling: pageFling,
      pageSnap: pageSnap,
      enableImmersive: enableImmersive,
      videoPages: videoPages,
    );
  }
}

MethodChannel channel = const MethodChannel('flutter_pdf_viewer');

String _sha1(str) => sha1.convert(utf8.encode(str)).toString();

_invokeMethod(
    String name, dynamic src, PdfViewerConfig config, String pdfHash) {
  if (config == null) {
    config = PdfViewerConfig();
  }

  return channel.invokeMethod(
    name,
    {
      'src': src,
      'password': config.password,
      'xorDecryptKey': config.xorDecryptKey,
      'nightMode': config.nightMode,
      'enableSwipe': config.enableSwipe,
      'swipeHorizontal': config.swipeHorizontal,
      'autoSpacing': config.autoSpacing,
      'pageFling': config.pageFling,
      'pageSnap': config.pageSnap,
      'enableImmersive': config.enableImmersive,
      'autoPlay': config.autoPlay,
      'videoPages': config.videoPages?.map((int key, Video value) {
        return MapEntry(key, value.toJson());
      }),
      'pdfHash': pdfHash,
    },
  );
}

class PdfViewer {
  /// Load Pdf from given file path.
  /// Uses Android's `Uri.parse()`.
  /// (Note: Adds the `file://` prefix)
  static Future loadFile(
    String filePath, {
    PdfViewerConfig config,
    Function onLoad,
  }) async {
    await _invokeMethod(
        'fromFile', 'file://' + filePath, config, _sha1('file;$filePath'));
    if (onLoad != null) await onLoad();
  }

  /// Load Pdf from raw bytes.
  static Future loadBytes(
    Uint8List pdfBytes, {
    PdfViewerConfig config,
    Function onLoad,
  }) async {
    int pdfBytesSize = pdfBytes.length;

    ServerSocket pdfServer =
        await ServerSocket.bind('localhost', PDF_BYTES_PORT);
    pdfServer.listen(
      (Socket client) {
        client.add(pdfBytes);
        client.close();
        pdfServer.close();
      },
    );

    await _invokeMethod('fromBytes', pdfBytesSize, config,
        _sha1('bytes;${sha1.convert(pdfBytes.sublist(0, 64))}'));
    if (onLoad != null) await onLoad();
  }

  /// Load Pdf from Flutter's asset folder
  static Future loadAsset(
    String assetPath, {
    PdfViewerConfig config,
    Function onLoad,
  }) async {
    await _invokeMethod(
        'fromAsset', assetPath, config, _sha1("asset;$assetPath"));
    if (onLoad != null) await onLoad();
  }

  // Download file from `url` and then show it
  static Future loadUrl(
    String url, {
    PdfViewerConfig config,
    bool cache: true,
    Function onDownload,
    Function onLoad,
  }) async {
    String filePath = await downloadAsFile(url, cache: cache);
    if (onDownload != null) await onDownload(filePath);
    await _invokeMethod(
        'fromFile', 'file://' + filePath, config, _sha1("url;$url"));
    if (onLoad != null) await onLoad();
  }
}
