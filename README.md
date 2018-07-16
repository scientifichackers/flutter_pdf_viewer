# flutter pdf viewer

**Android Only!**

A native Pdf viewer for flutter, built on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer), which is based on [PdfiumAndroid](https://github.com/barteksc/PdfiumAndroid)

## Features

- Inline Pdf Viewing is not supported. ( A new activity is opened ).
- Can do *fast*, native XOR - decryption of files.
- Night Mode.
- Password protected pdf.
- ScrollBar
- Pinch to zoom

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
/// (Transformed to a File Uri in native code)
Future<void> FlutterPdfViewer.loadFilePath(
    String filePath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  })


/// Load Pdf from raw bytes.
///
/// Note - This has a performance limitation,
/// because flutter uses a message channel to pass data to native code.
/// (Serialization of large byte arrays can be expensive)
Future<void> loadBytes(
    Uint8List pdfBytes, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  })

/// Load Pdf from Flutter's asset folder
Future<void> loadAsset(
    String assetPath, {
    String password,
    bool nightMode,
    String xorDecryptKey,
  })
```

## Thanks

- [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) for proving this wonderful, and simple to use library.
- [#10348](https://github.com/flutter/flutter/issues/10348).



---

<a href="https://www.buymeacoffee.com/u75YezVri" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/black_img.png" alt="Buy Me A Coffee" style="height: auto !important;width: auto !important;" ></a>

[🐍🏕️](http://www.pycampers.com/)
