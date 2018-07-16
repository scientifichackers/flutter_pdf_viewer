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

[Dart Pub](https://pub.dartlang.org/packages/flutter_pdf_viewer#-readme-tab-)

License: MIT

## Example

Put `test.pdf` at `assets/test.pdf`

```yaml
// pubspec.yaml

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

### Result
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
/// since flutter uses a message channel to send data to native code.
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

---

<a href="https://www.buymeacoffee.com/u75YezVri" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/black_img.png" alt="Buy Me A Coffee" style="height: auto !important;width: auto !important;" ></a>

[🐍🏕️](http://www.pycampers.com/)
