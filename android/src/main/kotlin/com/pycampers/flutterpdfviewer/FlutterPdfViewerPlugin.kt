package com.pycampers.flutterpdfviewer

import android.content.Context
import android.net.Uri
import android.view.View
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import java.io.File
import java.io.IOException


class PdfViewFactory(val registrar: Registrar) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    val messenger: BinaryMessenger = registrar.messenger()

    override fun create(context: Context, id: Int, o: Any?): PlatformView {
        return FlutterPdfViewerPlugin(registrar, context, messenger, id)
    }
}

class FlutterPdfViewerPlugin constructor(
        val registrar: Registrar,
        val context: Context,
        messenger: BinaryMessenger,
        id: Int
) : MethodCallHandler, PlatformView {

    val pdfView: PDFView = PDFView(context, null)
    val scrollHandle: DefaultScrollHandle = DefaultScrollHandle(context)

    init {
        val methodChannel = MethodChannel(messenger, "flutter_pdf_viewer/pdfview_$id")
        methodChannel.setMethodCallHandler(this)
    }

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            registrar
                    .platformViewRegistry()
                    .registerViewFactory("flutter_pdf_viewer/pdfview", PdfViewFactory(registrar))
        }
    }

    override fun getView(): View {
//        if (pdfView.isRecycled) {
//
//        }
        return pdfView
    }

    override fun dispose() {}

    override fun onMethodCall(methodCall: MethodCall, result: Result) {
        PdfViewerThread(this, methodCall, result).run()
    }

    @Throws(IOException::class)
    fun fromFile(methodCall: MethodCall): PDFView.Configurator {
        val xorDecryptKey = extractXorDecryptKey(methodCall)
        val filePath = methodCall.argument<String>("src")

        return if (xorDecryptKey == null) {
            pdfView.fromFile(File(Uri.parse(filePath).path!!))
        } else {
            pdfView.fromBytes(
                    xorEncryptDecrypt(readBytesFromFile(filePath), xorDecryptKey)
            )
        }
    }

    @Throws(IOException::class)
    fun fromAsset(methodCall: MethodCall): PDFView.Configurator {
        val xorDecryptKey = extractXorDecryptKey(methodCall)
        val assetPath = registrar.lookupKeyForAsset(
                methodCall.argument<String>("src")!!
        )

        return if (xorDecryptKey == null) {
            pdfView.fromAsset(assetPath)
        } else {
            pdfView.fromBytes(
                    xorEncryptDecrypt(readBytesFromAsset(context, assetPath), xorDecryptKey)
            )
        }
    }

    @Throws(IOException::class)
    fun fromBytes(methodCall: MethodCall): PDFView.Configurator {
        val xorDecryptKey = extractXorDecryptKey(methodCall)
        val pdfBytes = readBytesFromSocket(methodCall.argument<Int>("src")!!)

        return if (xorDecryptKey == null) {
            pdfView.fromBytes(pdfBytes)
        } else {
            pdfView.fromBytes(xorEncryptDecrypt(pdfBytes, xorDecryptKey))
        }
    }
}


