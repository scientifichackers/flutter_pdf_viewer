import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';

const int _PDF_BYTES_PORT = 4567;

/// Describes a page containing a video
///
/// The [pageNumber] is the page which will contain an overlay video. (Page numbers start from `1`)
///
/// The various constructors can be used to describe the source of the Video file.
class VideoPage {
  int pageNumber;
  String mode;
  String xorDecryptKey;
  String src;

  VideoPage.fromFile(
    this.pageNumber,
    String filePath, {
    this.xorDecryptKey,
  }) {
    mode = "fromFile";
    src = filePath;
  }

  VideoPage.fromAsset(
    this.pageNumber,
    String assetPath, {
    this.xorDecryptKey,
  }) {
    mode = "fromAsset";
    src = assetPath;
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
  List<VideoPage> videoPages;

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
  String name,
  dynamic src,
  PdfViewerConfig config,
  String pdfHash,
) async {
  if (config == null) {
    config = PdfViewerConfig();
  }

  Map<int, Map<String, String>> videoPagesMap = {};
  config.videoPages?.forEach((videoPage) {
    videoPagesMap[videoPage.pageNumber] = {
      'mode': videoPage.mode,
      'src': videoPage.src,
      'xorDecryptKey': videoPage.xorDecryptKey
    };
  });

  await channel.invokeMethod(
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
      'videoPages': videoPagesMap,
      'pdfHash': pdfHash,
    },
  );
}

class PdfViewer {
  /// Enable recording of page by page analytics.
  ///
  /// The [period] is the time interval between 2 successive analytics recordings.
  /// A small Duration, results in more fine-grained timestamps, at the cost of resource usage.
  static Future<void> enableAnalytics(Duration period) {
    return channel.invokeMethod(
      "enableAnalytics",
      period.inMilliseconds,
    );
  }

  /// Disable recording of analytics.
  static Future<void> disableAnalytics() {
    return channel.invokeMethod("disableAnalytics", null);
  }

  /// Returns the stored analytics.
  ///
  /// The analytics are returned for the currently, or most recently opened PDF document.
  /// These will not be persisted on disk, only in-memory.
  ///
  /// The returned value is a Map of page numbers to time spent on that page.
  /// (Page numbers start from `1`)
  static Future<Map<int, Duration>> getAnalytics() async {
    var map = (await channel.invokeMethod("getAnalytics")).map((page, elapsed) {
      return MapEntry(page, Duration(milliseconds: elapsed));
    });
    return Map<int, Duration>.from(map);
  }

  /// Load Pdf from [filePath].
  ///
  /// Uses Android's `Uri.parse()` internally. (After adding the `file://` prefix)
  static Future<void> loadFile(
    String filePath, {
    PdfViewerConfig config,
  }) async {
    await _invokeMethod(
      'fromFile',
      'file://' + filePath,
      config,
      _sha1('file;$filePath'),
    );
  }

  /// Load Pdf from raw bytes.
  static Future<void> loadBytes(Uint8List pdfBytes,
      {PdfViewerConfig config}) async {
    int pdfBytesSize = pdfBytes.length;

    ServerSocket pdfServer =
        await ServerSocket.bind('localhost', _PDF_BYTES_PORT);
    pdfServer.listen(
      (Socket client) {
        client.add(pdfBytes);
        client.close();
        pdfServer.close();
      },
    );

    await _invokeMethod(
      'fromBytes',
      pdfBytesSize,
      config,
      _sha1('bytes;${sha1.convert(pdfBytes.sublist(0, 64))}'),
    );
  }

  /// Load Pdf from Flutter's asset folder
  static Future<void> loadAsset(
    String assetPath, {
    PdfViewerConfig config,
  }) async {
    await _invokeMethod(
      'fromAsset',
      assetPath,
      config,
      _sha1("asset;$assetPath"),
    );
  }
}
