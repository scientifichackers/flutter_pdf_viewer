# flutter pdf viewer

A native Pdf viewer for flutter, built on [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer).

[![Sponsor](https://img.shields.io/badge/Sponsor-jaaga_labs-red.svg?style=for-the-badge)](https://www.jaaga.in/labs)

[![pub package](https://img.shields.io/pub/v/flutter_pdf_viewer.svg?style=for-the-badge)](https://pub.dartlang.org/packages/flutter_pdf_viewer)

*P.S. Android Only!*

## Features

-   Night Mode
-   Scroll Bar
-   Horizontal swipe
-   Pinch to zoom
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

License: MIT

#### NOTE: You must add these lines at `android/app/build.gradle`.
(This is required by [ExoPlayer](https://github.com/google/ExoPlayer), which is used to play videos).
```
android {
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

## Generating 64-bit APKs

The Underlying native library tends to blow up the APK size. So, you can build a separate APK for each CPU architecture.

This will also fix an issue with flutter tooling, where 64-bit ARM devices don't work.

Add the following section in `android/app/build.gradle` - 
```
android {
    defaultConfig {
        ndk {
            abiFilters "<arch>"
        }
    }
}
```

- For 32-bit APK, replace `<arch>` with `armeabi-v7a`, and run `$ flutter build apk --release` as usual.

- For 64-bit APK, replace `<arch>` with `arm64-v8a`, and run `$ flutter build apk --release --target-platform=android-arm64`.

Now you have 2 Apks, which you will need to publish separately to the Play Store. For that you need to tweak the `android:versionCode` property to have slightly different values for each build.

The exact "Rules for multiple APKs" can be found [here](https://developer.android.com/google/play/publishing/multiple-apks).

## Thanks

-   [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) for proving this wonderful, and simple to use library.
-   [#10348](https://github.com/flutter/flutter/issues/10348).

---

[🐍🏕️](http://www.pycampers.com/)
