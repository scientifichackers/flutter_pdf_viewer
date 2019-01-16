import 'dart:core';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'downloader.dart';

Widget createPDFView(Function callback) {
  return AndroidView(
    viewType: 'flutter_pdf_viewer/pdfview',
    onPlatformViewCreated: (int id) {
      callback(MethodChannel('flutter_pdf_viewer/pdfview_$id'));
    },
  );
}

_invokeMethod(
  MethodChannel channel,
  String name,
  List args,
) {
  return channel.invokeMethod(
    name,
    {
      'src': args[0],
      'password': args[1],
      'nightMode': args[2],
      'xorDecryptKey': args[3],
      'swipeHorizontal': args[4],
      'pageFling': args[5],
      'enableSwipe': args[6],
    },
  );
}

class PDFView {
  static Widget fromFile(
    String filePath, {
    String password,
    String xorDecryptKey,
    bool nightMode: false,
    bool swipeHorizontal: false,
    bool pageFling: true,
    bool enableSwipe: true,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      await _invokeMethod(
        channel,
        "fromFile",
        [
          filePath,
          password,
          nightMode,
          xorDecryptKey,
          swipeHorizontal,
          pageFling,
          enableSwipe,
        ],
      );
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromUrl(
    String url, {
    String password,
    bool nightMode: false,
    String xorDecryptKey,
    bool swipeHorizontal: false,
    bool pageFling: true,
    bool enableSwipe: true,
    Function onDownload,
    Function onLoad,
    bool cache: true,
  }) {
    return createPDFView((MethodChannel channel) async {
      String filePath = await downloadAsFile(url);
      if (onDownload != null) await onDownload(filePath);
      var x = await _invokeMethod(
        channel,
        "fromFile",
        [
          filePath,
          password,
          nightMode,
          xorDecryptKey,
          swipeHorizontal,
          pageFling,
          enableSwipe,
        ],
      );
      print('retvalue: $x');
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromAsset(
    String assetPath, {
    String password,
    bool nightMode: false,
    String xorDecryptKey,
    bool swipeHorizontal: false,
    bool pageFling: true,
    bool enableSwipe: true,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      await _invokeMethod(
        channel,
        "fromAsset",
        [
          assetPath,
          password,
          nightMode,
          xorDecryptKey,
          swipeHorizontal,
          pageFling,
          enableSwipe,
        ],
      );
      if (onLoad != null) await onLoad();
    });
  }

  static Widget fromBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode: false,
    String xorDecryptKey,
    bool swipeHorizontal: false,
    bool pageFling: true,
    bool enableSwipe: true,
    Function onLoad,
  }) {
    return createPDFView((MethodChannel channel) async {
      int pdfBytesSize = pdfBytes.length;

      ServerSocket server = await ServerSocket.bind('0.0.0.0', 4567);
      server.listen(
        (Socket client) {
          client.add(pdfBytes);
          client.close();
          server.close();
        },
      );

      await _invokeMethod(
        channel,
        "fromBytes",
        [
          pdfBytesSize,
          password,
          nightMode,
          xorDecryptKey,
          swipeHorizontal,
          pageFling,
          enableSwipe,
        ],
      );
      if (onLoad != null) await onLoad();
    });
  }
}
