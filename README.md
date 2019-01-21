# flutter pdf viewer

A native Pdf viewer for flutter, built on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer).

*P.S. Android Only!*

## Features

-   Night Mode
-   Scroll Bar
-   Horizontal swipe
-   Pinch to zoom
-   Download PDF from URL
-   Show inline Videos in Pdf
-   Immersive mode
-   Page by page analytics

## Security

-   Features _fast_ native speed XOR - decryption of files.
-   Password protected pdf.
-   Download and display PDF from URL without ever touching the disk!

## Drawbacks

-   <s>Inline Pdf Viewing is not supported. ( A new activity is opened ). See [#1](https://github.com/pycampers/flutter_pdf_viewer/issues/1).</s>

    A proof of concept for inline PDFs is available at the [inline](https://github.com/pycampers/flutter_pdf_viewer/tree/inline) branch.

## Install

To use this plugin, follow the [installation instructions](https://pub.dartlang.org/packages/flutter_pdf_viewer#-installing-tab-).

[![pub package](https://img.shields.io/pub/v/flutter_pdf_viewer.svg)](https://pub.dartlang.org/packages/flutter_pdf_viewer)

License: MIT

#### NOTE: You must add these lines at `android/app/build.gradle`.

```
android {
    ...

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

## Example

```sh
$ git clone https://github.com/pycampers/flutter_pdf_viewer.git
$ cd flutter_pdf_viewer/example
$ flutter run
```

## Preview

<img src="https://i.imgur.com/Uhmk09s.png" height="400" />

## Thanks

-   [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) for proving this wonderful, and simple to use library.
-   [#10348](https://github.com/flutter/flutter/issues/10348).

---

[🐍🏕️](http://www.pycampers.com/)
