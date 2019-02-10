import 'package:flutter/material.dart';
import 'package:flutter_pdf_viewer/flutter_pdf_viewer.dart';

void main() {
  runApp(new MyApp());
  PdfViewer.enableAnalytics(Duration(milliseconds: 500));
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
              FromAsset(),
              AnalyticsView(),
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
  'autoPlay: true',
  'slideshow',
  'XOR encrypted',
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
    String prefix = _value == 10 ? "xor_" : "";

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
              'assets/${prefix}test.pdf',
              config: PdfViewerConfig(
                  nightMode: _value == 1,
                  swipeHorizontal: _value == 3 || _value == 9,
                  autoSpacing: _value == 4 || _value == 9,
                  pageFling: _value == 5 || _value == 9,
                  pageSnap: _value == 6 || _value == 9,
                  enableImmersive: _value == 7,
                  autoPlay: _value == 8,
                  videoPages: [
                    VideoPage.fromAsset(
                      8,
                      "assets/${prefix}buck_bunny.mp4",
                      xorDecryptKey: _value == 10 ? "test" : null,
                    ),
                    VideoPage.fromAsset(
                      9,
                      "assets/${prefix}buck_bunny.mp4",
                      xorDecryptKey: _value == 10 ? "test" : null,
                    ),
                  ],
                  xorDecryptKey: _value == 10 ? "test" : null),
            );
          },
        ),
      ],
    );
  }
}

class AnalyticsView extends StatefulWidget {
  @override
  _AnalyticsViewState createState() => _AnalyticsViewState();
}

class _AnalyticsViewState extends State<AnalyticsView> {
  Map _analytics = {};

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Column(
        children: <Widget>[
              RaisedButton(
                child: Text("getAnalytics(null)"),
                onPressed: () async {
                  Map records = await PdfViewer.getAnalytics(null);
                  setState(() => _analytics = records);
                },
              ),
            ] +
            _analytics.keys
                .map((page) => Text("$page - ${_analytics[page]}"))
                .toList(),
      ),
    );
  }
}
