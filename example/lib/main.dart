import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_pdf_viewer/flutter_pdf_viewer.dart';

void main() => runApp(new MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: new Center(
          child: ListView(
            children: [
              LoadFromAssetButton(),
              LoadUrlAsFile(),
              LoadUrlAsBytes(),
            ],
          ),
        ),
      ),
    );
  }
}

class LoadFromAssetButton extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return RaisedButton(
      onPressed: () => FlutterPdfViewer.loadAsset('assets/test.pdf'),
      child: Text('open from assets'),
    );
  }
}

class LoadUrlAsFile extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return RaisedButton(
      onPressed: () async {
        Scaffold.of(context).showSnackBar(
          SnackBar(
            content: Text('Downloading...'),
            duration: Duration(days: 24),
          ),
        );

        String filePath = await FlutterPdfViewer.downloadAsFile(
          'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf',
        );

        print("filePath: '$filePath'");

        Scaffold.of(context).hideCurrentSnackBar();

        FlutterPdfViewer.loadFilePath(filePath);
      },
      child: Text('download + load as file (cached)'),
    );
  }
}

class LoadUrlAsBytes extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return RaisedButton(
      onPressed: () async {
        Scaffold.of(context).showSnackBar(
          SnackBar(
            content: Text('Downloading...'),
            duration: Duration(days: 24),
          ),
        );

        Uint8List bytes = await FlutterPdfViewer.downloadAsBytes(
          'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf',
        );

        Scaffold.of(context).hideCurrentSnackBar();

        FlutterPdfViewer.loadBytes(bytes);
      },
      child: Text('download + load as bytes (not cached)'),
    );
  }
}
