import 'dart:async';

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
              LoadFromUrlButton(),
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

class LoadFromUrlButton extends StatelessWidget {
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

        Future<void> showPdfFuture = await FlutterPdfViewer.loadUrl(
          'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf',
        );

        Scaffold.of(context).hideCurrentSnackBar();

        await showPdfFuture;
      },
      child: Text('open from url'),
    );
  }
}
