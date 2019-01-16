package com.pycampers.flutterpdfviewer

import com.github.barteksc.pdfviewer.PDFView
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.IOException

class PdfViewerThread(
        val instance: FlutterPdfViewerPlugin,
        val methodCall: MethodCall,
        val result: MethodChannel.Result
) : Thread() {

    override fun run() {
        val configurator: PDFView.Configurator

        try {
            configurator = when (methodCall.method) {
                "fromFile" -> {
                    instance.fromFile(methodCall)
                }
                "fromAsset" -> {
                    instance.fromAsset(methodCall)
                }
                "fromBytes" -> {
                    instance.fromBytes(methodCall)
                }
                else -> {
                    result.notImplemented()
                    return
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            result.error(
                    "IOException", "Encountered `IOException` while loading PDF: " + methodCall.toString(), e
            )
            return
        }

        configurator
                .password(methodCall.argument<String>("password"))
                .nightMode(methodCall.argument<Boolean>("nightMode")!!)
                .swipeHorizontal(methodCall.argument<Boolean>("swipeHorizontal")!!)
                .pageFling(methodCall.argument<Boolean>("pageFling")!!)
                .enableSwipe(methodCall.argument<Boolean>("enableSwipe")!!)
                .scrollHandle(instance.scrollHandle)
                .linkHandler(CustomLinkHandler(instance.pdfView))
                .load()

        result.success(true)
    }
}