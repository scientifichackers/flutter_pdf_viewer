import 'package:flutter/material.dart';
import 'package:flutter_pdf_viewer/main.dart';

void main() => runApp(new MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      home: new Scaffold(
        appBar: new AppBar(
          title: const Text('Plugin example app'),
        ),
        body: HomePage(),
      ),
    );
  }
}

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  @override
  Widget build(BuildContext context) {
    double pdfHeight = MediaQuery.of(context).size.height / 4;

    return Column(
      children: <Widget>[
        Container(
          height: pdfHeight,
          child: PDFView.fromAsset(
            'assets/lorem-ipsum.pdf',
            onLoad: () {
              print("loaded pdf 1!");
            },
          ),
        ),
        Container(
          height: pdfHeight,
          child: PDFView.fromAsset(
            'assets/test.pdf',
            swipeHorizontal: true,
            pageFling: true,
            onLoad: () {
              print("loaded pdf 2!");
            },
          ),
        ),
        Container(
          height: pdfHeight,
          child: PDFView.fromUrl(
              'http://www.usingcsp.com/cspbook.pdf',
              onDownload: (f) {
                print("downloaded file! $f");
              },
              onLoad: () {
                print("loaded pdf 3!");
              }),
        ),
      ],
    );
  }
}
