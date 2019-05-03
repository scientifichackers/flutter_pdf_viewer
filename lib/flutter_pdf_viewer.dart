import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:crypto/crypto.dart';
import 'package:flutter/services.dart';

MethodChannel _platform = const MethodChannel('flutter_pdf_viewer');

String _sha1(str) => sha1.convert(utf8.encode(str)).toString();

/// Describes a page containing a video
///
/// The [pageIndex] is the page which will contain an overlay video.
/// (Page indices start from `0`)
///
/// The various constructors can be used to describe the source of the Video file.
class VideoPage {
  int pageIndex;
  String mode;
  String xorDecryptKey;
  String src;

  VideoPage.fromFile(
    this.pageIndex,
    String filePath, {
    this.xorDecryptKey,
  }) {
    mode = "fromFile";
    src = filePath;
  }

  VideoPage.fromAsset(
    this.pageIndex,
    String assetPath, {
    this.xorDecryptKey,
  }) {
    mode = "fromAsset";
    src = assetPath;
  }
}

/// Represents a general configuration object for tweaking how the PDF is viewed.
///
/// - [password]
///     The password in case of a password protected PDF.
/// - [xorDecryptKey]
///     The encryption key for a XOR encrypted file.
/// - [nightMode]
///     Whether to display PDF in night mode, with inverted colors.
/// - [enableSwipe]
///     Whether to allow swipe gesture to navigate across pages.
/// - [swipeHorizontal]
///     Whether to enable horizontal, instead of default, vertical navigation.
/// - [autoSpacing]
///     Whether to add dynamic spacing to fit each page on its own on the screen.
/// - [pageFling]
///     Whether to make a fling gesture change only a single page like ViewPager
/// - [pageSnap]
///     Whether to snap pages to screen boundaries.
/// - [enableImmersive]
///     Enables immersive mode, that hides the system UI.
///     This requires an API level of at least 19 (Kitkat 4.4).
/// - [videoPages]
///     A list of [VideoPage] objects, to be played as an overlay on the pdf.
/// - [pages]
///     A list of integers that indicates which pages have to be shown.
///     (page numbers start from 0)
///
///     If this is not supplied, then all pages are shown.
/// - [autoPlay]
///     Whether to automatically play the video when the user arrives at the page associated with the video.
///     This may lead to bad UX if used without [slideShow] enabled.
/// - [slideShow]
///     Simulate a slideshow-like view, by enabling [swipeHorizontal], [autoSpacing], [pageFling] & [pageSnap].
/// - [pdfId]
///     Use this PDF identifier for recording analytics, instead of the automatically generated one.
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
  List<int> pages;
  bool forceLandscape;
  String pdfId;

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
    this.forceLandscape: false,
    slideShow: false,
    this.videoPages,
    this.pages,
    this.pdfId,
  }) {
    if (slideShow) {
      swipeHorizontal = autoSpacing = pageFling = pageSnap = true;
    }
  }

  /// creates a shallow copy of this config object.
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
      pages: pages,
      forceLandscape: forceLandscape,
    );
  }

  Map<String, dynamic> toMap() {
    Map<int, Map<String, String>> videoPagesMap = {};
    videoPages?.forEach((videoPage) {
      videoPagesMap[videoPage.pageIndex] = {
        'mode': videoPage.mode,
        'src': videoPage.src,
        'xorDecryptKey': videoPage.xorDecryptKey
      };
    });

    return {
      'password': password,
      'xorDecryptKey': xorDecryptKey,
      'nightMode': nightMode,
      'enableSwipe': enableSwipe,
      'swipeHorizontal': swipeHorizontal,
      'autoSpacing': autoSpacing,
      'pageFling': pageFling,
      'pageSnap': pageSnap,
      'enableImmersive': enableImmersive,
      'autoPlay': autoPlay,
      'videoPages': videoPagesMap,
      'pages': pages != null ? Int32List.fromList(pages) : null,
      'forceLandscape': forceLandscape,
    };
  }
}

Future<String> _launchPdfActivity(
  String mode,
  String src,
  PdfViewerConfig config,
  String callSignature,
) async {
  final args = (config ?? PdfViewerConfig()).toMap();
  final pdfId = config?.pdfId ?? _sha1("$callSignature:$args");
  args.addAll({'mode': mode, 'src': src, 'pdfId': pdfId});
  await _platform.invokeMethod("launchPdfActivity", args);
  return pdfId;
}

Map<int, Duration> _serializeAnalyticsEntries(entries) {
  return Map<int, Duration>.from(
    entries.map((page, elapsed) {
      return MapEntry(page, Duration(milliseconds: elapsed));
    }),
  );
}

class PdfViewer {
  /// Enable recording of page by page analytics.
  ///
  /// The [period] is the time interval between 2 successive analytics recordings.
  /// A smaller Duration, results in more fine-grained timestamps, at the cost of resource usage.
  static Future<void> enableAnalytics(Duration period) {
    return _platform.invokeMethod("enableAnalytics", period.inMilliseconds);
  }

  /// Disable recording of analytics.
  static Future<void> disableAnalytics() {
    return _platform.invokeMethod("disableAnalytics", null);
  }

  /// Returns the stored analytics.
  ///
  /// [pdfId] is a [String] returned by all the `PdfViewer.load*()` methods.
  /// It is a unique identifier assigned to a PDF document by the library,
  /// based on the function arguments etc.
  ///
  /// If the [pdfId] is not provided or set to `null`,
  /// the analytics are returned for the currently,
  /// or most recently opened PDF document.
  ///
  /// If [all] is set to `true`, then [pdfId] is ignored,
  /// and analytics for all PDFs are returned.
  ///
  /// These are not be persisted on disk, only in-memory.
  ///
  ///
  /// The returned value is a mapping from [pdfId] to a mapping,
  /// from `pageIndex` to the time [Duration] spent on that page.
  /// (Page indices start from `0`)
  ///
  /// ```
  /// {
  ///   pdfId: {
  ///     pageIndex: Duration
  ///   }
  /// }
  /// ```
  static Future<Map<String, Map<int, Duration>>> getAnalytics({
    String pdfId,
    bool all: false,
  }) async {
    var result;
    if (all) {
      result = await _platform.invokeMethod("getAllAnalytics");
    } else {
      result = await _platform.invokeMethod("getAnalytics", pdfId);
    }
    if (result == null) return {};

    return Map<String, Map<int, Duration>>.from(
      result.map((pdfId, entries) {
        return MapEntry(pdfId, _serializeAnalyticsEntries(entries));
      }),
    );
  }

  /// Load Pdf from [filePath].
  ///
  /// Uses Android's `Uri.parse()` internally. (After adding the `file://` prefix)
  static Future<String> loadFile(
    String filePath, {
    PdfViewerConfig config,
  }) async {
    return await _launchPdfActivity(
      'fromFile',
      'file://' + filePath,
      config,
      'file:$filePath',
    );
  }

  /// Load Pdf from raw bytes.
  static Future<String> loadBytes(
    Uint8List pdfBytes, {
    PdfViewerConfig config,
  }) async {
    final size = pdfBytes.length;

    final server = await ServerSocket.bind(InternetAddress.loopbackIPv4, 0);
    server.listen((client) {
      client.add(pdfBytes);
      client.close();
      server.close();
    });

    return await _launchPdfActivity(
      'fromBytes',
      "${server.address.address},${server.port},$size",
      config,
      'bytes:${sha1.convert(pdfBytes.sublist(0, 64))}:$size',
    );
  }

  /// Load Pdf from Flutter's asset folder
  static Future<String> loadAsset(
    String assetPath, {
    PdfViewerConfig config,
  }) async {
    return await _launchPdfActivity(
      'fromAsset',
      assetPath,
      config,
      'asset:$assetPath',
    );
  }
}
