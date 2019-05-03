package com.pycampers.flutterpdfviewer

import android.os.Bundle
import android.util.Log
import android.view.View
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import java.io.File

// This thread is used to do heavy tasks, like loading the PDF from disk, decrypting it etc.
class PdfActivityThread(
    val activity: PdfActivity,
    val opts: Bundle,
    val pdfView: PDFView,
    val playerController: PlayerController,
    val scrollHandle: DefaultScrollHandle
) : Thread() {
    val mode = opts.getString("mode")!!
    val xorDecryptKey: String? = opts.getString("xorDecryptKey")

    fun buildConfigurator(): PDFView.Configurator? {
        val src = opts.getString("src")!!

        xorDecryptKey?.let {
            val bytes = when (mode) {
                "fromFile" -> {
                    Log.d(TAG, "loading encrypted pdf from file { $src }...")
                    readBytesFromFile(src)
                }
                "fromBytes" -> {
                    Log.d(TAG, "loading encrypted pdf from bytes...")
                    readBytesFromSocket(src)
                }
                "fromAsset" -> {
                    Log.d(TAG, "loading encrypted pdf from assets { $src }...")
                    readBytesFromAsset(activity.applicationContext, src)
                }
                else -> throw IllegalArgumentException("invalid mode: $mode.")
            }
            xorEncryptDecrypt(bytes, it)
            return pdfView.fromBytes(bytes)
        }

        return when (mode) {
            "fromFile" -> {
                Log.d(TAG, "loading pdf from file { $src }...")
                pdfView.fromFile(File(src))
            }
            "fromBytes" -> {
                Log.d(TAG, "loading pdf from bytes...")
                pdfView.fromBytes(readBytesFromSocket(src))
            }
            "fromAsset" -> {
                Log.d(TAG, "loading pdf from assets { $src }...")
                pdfView.fromAsset(src)
            }
            else -> throw IllegalArgumentException("invalid mode: $mode.")
        }
    }

    override fun run() {
        pdfView.visibility = View.VISIBLE
        var configurator = buildConfigurator()!!

        configurator = configurator
            .password(opts.getString("password"))
            .nightMode(opts.getBoolean("nightMode"))
            .enableSwipe(opts.getBoolean("enableSwipe"))
            .swipeHorizontal(opts.getBoolean("swipeHorizontal"))
            .autoSpacing(opts.getBoolean("autoSpacing"))
            .pageFling(opts.getBoolean("pageFling"))
            .pageSnap(opts.getBoolean("pageSnap"))
            .onLoad(activity)
            .onError(activity)
            .onRender(activity)
            .scrollHandle(scrollHandle)

        if (opts.containsKey("pages")) {
            configurator = configurator.pages(*opts.getIntArray("pages"))
        }

        if (playerController.videoPages != null) {
            configurator = configurator
                .onPageChange(playerController)
                .onTap(playerController)
        }

        configurator.load()
    }
}