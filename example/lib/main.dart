import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_pdf_viewer/flutter_pdf_viewer.dart';
import 'package:path_provider/path_provider.dart';

const OPTIONS = [
  'null',
  'nightMode: true',
  'enableSwipe: false',
  'swipeHorizontal: true',
  'autoSpacing: true',
  'pageFling: true',
  'pageSnap: true',
  'enableImmersive: true',
  'autoPlay: true',
  'slideshow: true',
  'XOR encrypted',
  'forceLandscape: true',
];

const MODES = [
  "loadAsset",
  "loadFile",
  "loadBytes",
];

List<DropdownMenuItem> buildDropDownItems(List<String> choices) {
  final items = <DropdownMenuItem>[];
  for (int i = 0; i < choices.length; i++) {
    items.add(
      DropdownMenuItem(child: Text(choices[i]), value: i),
    );
  }
  return items;
}

Future<Uint8List> assetToBytes(String src) async {
  final bytes = await rootBundle.load(src);
  return bytes.buffer.asUint8List();
}

Future<String> assetToFile(String src) async {
  final bytes = await assetToBytes(src);
  final tempDir = await getTemporaryDirectory();
  final file = File("${tempDir.path}/$src");
  await file.parent.create(recursive: true);
  await file.writeAsBytes(bytes);
  print(">> ${file.path} ${await file.exists()}");
  return file.path;
}

class PdfLoaderSection extends StatefulWidget {
  @override
  PdfLoaderSectionState createState() => new PdfLoaderSectionState();
}

class PdfLoaderSectionState extends State<PdfLoaderSection> {
  var option = 0;
  var mode = 0;

  Future<void> onPressed() async {
    String prefix = option == 10 ? "xor_" : "";

    final videoSrc = "assets/${prefix}buck_bunny.mp4";
    final pdfSrc = 'assets/${prefix}test.pdf';
    final key = option == 10 ? "test" : null;

    var videoPages;
    switch (mode) {
      case 0:
        videoPages = [
          VideoPage.fromAsset(8, videoSrc, xorDecryptKey: key),
          VideoPage.fromAsset(9, videoSrc, xorDecryptKey: key),
        ];
        break;
      case 1:
        final file = await assetToFile(videoSrc);
        videoPages = [
          VideoPage.fromFile(8, file, xorDecryptKey: key),
          VideoPage.fromFile(9, file, xorDecryptKey: key),
        ];
        break;
      case 2:
        break;
    }

    final config = PdfViewerConfig(
      nightMode: option == 1,
      enableSwipe: option != 2,
      swipeHorizontal: option == 3,
      autoSpacing: option == 4,
      pageFling: option == 5,
      pageSnap: option == 6,
      enableImmersive: option == 7,
      autoPlay: option == 8,
      forceLandscape: option == 11,
      slideShow: option == 9,
      videoPages: videoPages,
      xorDecryptKey: key,
      atExit: (pageIndex) {
        print(">> atExit($pageIndex)");
      },
    );

    switch (mode) {
      case 0:
        await PdfViewer.loadAsset(pdfSrc, config: config);
        break;
      case 1:
        await PdfViewer.loadFile(await assetToFile(pdfSrc), config: config);
        break;
      case 2:
        await PdfViewer.loadBytes(await assetToBytes(pdfSrc), config: config);
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        DropdownButton(
          value: option,
          items: buildDropDownItems(OPTIONS),
          onChanged: (value) {
            setState(() {
              option = value;
            });
          },
        ),
        DropdownButton(
          value: mode,
          items: buildDropDownItems(MODES),
          onChanged: (value) {
            setState(() {
              mode = value;
            });
          },
        ),
        RaisedButton(
          child: Text("GO"),
          onPressed: onPressed,
        ),
      ],
    );
  }
}

String prettyJson(map) {
  return JsonEncoder.withIndent(' ' * 2).convert(map);
}

class AnalyticsSection extends StatefulWidget {
  @override
  _AnalyticsSectionState createState() => _AnalyticsSectionState();
}

class _AnalyticsSectionState extends State<AnalyticsSection> {
  Map<String, Map<String, String>> analytics = {};

  void setAnalytics(Map<String, Map<int, Duration>> value) {
    setState(() {
      analytics = Map<String, Map<String, String>>.from(
        value.map((k, v) {
          return MapEntry(
            k.toString(),
            Map<String, String>.from(
              v.map((k1, v1) {
                return MapEntry(k1.toString(), v1.toString());
              }),
            ),
          );
        }),
      );
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        RaisedButton(
          child: Text("read PdfViewer.analyticsEntries"),
          onPressed: () {
            setAnalytics(PdfViewer.analyticsEntries);
          },
        ),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Text(prettyJson(analytics)),
        )
      ],
    );
  }
}

void main() {
  PdfViewer.enableAnalytics(Duration(milliseconds: 500));
  PdfViewer.analyticsCallback = (pdfId, pageIndex, paused) {
//    print("))) $pdfId, $pageIndex, $paused");
  };
  runApp(new MyApp());
}

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
              PdfLoaderSection(),
              Container(
                margin: const EdgeInsets.all(10),
                height: 1,
                color: Colors.black,
              ),
              AnalyticsSection(),
            ],
          ),
        ),
      ),
    );
  }
}
