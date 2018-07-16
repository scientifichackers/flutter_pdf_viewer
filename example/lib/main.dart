import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
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
          child: RaisedButton(
            onPressed: () => FlutterPdfViewer.loadAsset('assets/test.pdf'),
            child: Text('OPEN'),
          ),
        ),
      ),
    );
  }
}
