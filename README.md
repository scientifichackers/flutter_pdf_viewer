# flutter pdf viewer

**Android Only!**

A native Pdf viewer for flutter, built on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer), which is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid)

## Features

- Night Mode
- Scroll Bar
- Horizontal swipe
- Pinch to zoom
- Download PDF from URL

## Security

- Features *fast*, in-memory native XOR - decryption of files.
- Password protected pdf.
- Download and display PDF from URL without ever touching the disk!

## Drawbacks

- <s>Inline Pdf Viewing is not supported. ( A new activity is opened ).  See [#1](https://github.com/pycampers/flutter_pdf_viewer/issues/1).</s>

A proof of concept for inline pdfs is ready at the [inline](https://github.com/pycampers/flutter_pdf_viewer/tree/inline) branch.


## Install

To use this plugin, follow the [installation instructions](https://pub.dartlang.org/packages/flutter_pdf_viewer#-installing-tab-).

[![pub package](https://img.shields.io/pub/v/flutter_pdf_viewer.svg)](https://pub.dartlang.org/packages/flutter_pdf_viewer)

License: MIT

## Example

Put `test.pdf` at `assets/test.pdf`

```yaml
# pubspec.yaml

flutter:
    ...

    assets:
        - assets/test.pdf
```

```dart
// main.dart

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
          child: RaisedButton(
            onPressed: () => FlutterPdfViewer.loadAsset('assets/test.pdf'),
            child: Text('OPEN'),
          ),
        ),
      ),
    );
  }
}
```

**Alternatively,**
```sh
$ git clone https://github.com/pycampers/flutter_pdf_viewer.git
$ cd flutter_pdf_viewer/example
$ flutter run
```

## Preview
<img src="https://i.imgur.com/Uhmk09s.png" height="400" />

## API

```dart
import 'package:flutter_pdf_viewer/flutter_pdf_viewer.dart';



/// Load Pdf from file path.
/// (Uses the native file URI parser)
Future<void> FlutterPdfViewer.loadFilePath(
    String filePath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
})


/// Load Pdf from raw bytes.
Future<void> FlutterPdfViewer.loadBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
})


/// Load Pdf from Flutter's asset folder
Future<void> FlutterPdfViewer.loadAsset(
    String assetPath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
    bool swipeHorizontal,
})


// Download from url as a file (cached to disk)
Future<String> FlutterPdfViewer.downloadAsFile(String url, {bool cache: true})


// Download from file as bytes (in-memory)
Future<Uint8List> FlutterPdfViewer.downloadAsBytes(String url)
```

## Thanks

- [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) for proving this wonderful, and simple to use library.
- [#10348](https://github.com/flutter/flutter/issues/10348).



---

<a href="https://www.buymeacoffee.com/u75YezVri" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/black_img.png" alt="Buy Me A Coffee" style="height: auto !important;width: auto !important;" ></a>

[🐍🏕️](http://www.pycampers.com/)
