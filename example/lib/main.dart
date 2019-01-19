import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_pdf_viewer/main.dart';

void main() => runApp(new MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Flutter PDF Viewer'),
        ),
        body: new Center(
          child: ListView(
            children: [
              FromAsset(),
              FromUrl(),
              FromBytes(),
            ],
          ),
        ),
      ),
    );
  }
}

var choices = [
  'default',
  'nightMode: true',
  'enableSwipe: false',
  'swipeHorizontal: true',
  'autoSpacing: true',
  'pageFling: true',
  'pageSnap: true',
  'enableImmersive: true',
  'slideshow',
];

class FromAsset extends StatefulWidget {
  List<DropdownMenuItem> get items {
    List<DropdownMenuItem> items = [];
    for (int i = 0; i < choices.length; i++) {
      items.add(
        DropdownMenuItem(child: Text(choices[i]), value: i),
      );
    }
    return items;
  }

  @override
  FromAssetState createState() {
    return new FromAssetState();
  }
}

class FromAssetState extends State<FromAsset> {
  int _value = 0;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Padding(
          padding: const EdgeInsets.all(10.0),
          child: DropdownButton(
            value: _value,
            items: widget.items,
            onChanged: (value) {
              setState(() {
                _value = value;
              });
            },
          ),
        ),
        RaisedButton(
          child: Text("loadAsset()"),
          onPressed: () {
            PdfViewer.loadAsset(
              'assets/meghshala.pdf',
              onLoad: () {
                print('PDF loaded!');
              },
              config: PdfViewerConfig(
                nightMode: _value == 1,
                swipeHorizontal: _value == 3 || _value == 8,
                autoSpacing: _value == 4 || _value == 8,
                pageFling: _value == 5 || _value == 8,
                pageSnap: _value == 6 || _value == 8,
                enableImmersive: _value == 7,
                videoPages: {8: VideoPage.fromAsset("assets/hard_water.mp4")},
              ),
            );
          },
        ),
      ],
    );
  }
}

class FromUrl extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: RaisedButton(
        onPressed: () async {
          Scaffold.of(context).showSnackBar(
            SnackBar(
              content: Text('Downloading...'),
              duration: Duration(days: 24),
            ),
          );
          PdfViewer.loadUrl(
            'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf',
            onDownload: (filePath) {
              Scaffold.of(context).hideCurrentSnackBar();
              print('Downloaded - $filePath');
            },
            onLoad: () {
              print('PDF loaded!');
            },
          );
        },
        child: Text('loadUrl(cache: True)'),
      ),
    );
  }
}

class FromBytes extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: RaisedButton(
        onPressed: () async {
          Scaffold.of(context).showSnackBar(
            SnackBar(
              content: Text('Downloading...'),
              duration: Duration(days: 24),
            ),
          );

          Uint8List bytes = await downloadAsBytes(
            'https://mozilla.github.io/pdf.js/web/compressed.tracemonkey-pldi-09.pdf',
          );

          print('Downloaded as bytes');
          Scaffold.of(context).hideCurrentSnackBar();

          PdfViewer.loadBytes(
            bytes,
            onLoad: () {
              print('PDF loaded!');
            },
          );
        },
        child: Text('downloadAsBytes(cache: false) -> loadBytes()'),
      ),
    );
  }
}
