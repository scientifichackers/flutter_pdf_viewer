import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';

const int _PDF_BYTES_PORT = 4567;

/// Called periodically, to report the time user spends on a given page.
///
/// - [currentPage]
///     The page number user is currently on. (Page numbers start from `1`)
/// - [timeOnPage]
///     The time spent on this page since last page change.
///     Note: This will be different from *total* time spent on a page.
typedef AnalyticsCallback(int currentPage, Duration timeOnPage);

/// Called once, when the PDF file is successfully downloaded.
typedef OnDownload(String filePath);

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
  AnalyticsCallback analyticsCallback;

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
    this.analyticsCallback,
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
  /// Set the callback to be called periodically, with analytics information.
  ///
  /// The callback is called only when the user is on the PDF activity.
  ///
  /// [period] is the time duration between any 2 successive calls.
  ///
  /// The given [callback] will replace the currently registered callback, if any.
  /// To remove a previous handler, simply pass [null].
  ///
  /// For more information, look at [AnalyticsCallback].
  static Future<void> enableAnalytics(Duration period) {
    return channel.invokeMethod(
      "enableAnalytics",
      period.inMilliseconds,
    );
  }

  static Future<void> disableAnalytics() {
    return channel.invokeMethod("disableAnalytics", null);
  }

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
