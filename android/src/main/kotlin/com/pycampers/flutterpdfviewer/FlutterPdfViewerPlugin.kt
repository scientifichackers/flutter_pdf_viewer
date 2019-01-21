package com.pycampers.flutterpdfviewer

import android.content.Intent
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*

class FlutterPdfViewerPlugin private constructor(val registrar: Registrar) : MethodCallHandler {

    var timer = Timer()
    val pdfApp = registrar.context() as PdfApp

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_pdf_viewer")
            channel.setMethodCallHandler(FlutterPdfViewerPlugin(registrar))
        }
    }

    fun removeTimer() {
        timer.cancel()
        timer.purge()
    }

    fun scheduleTimer(period: Int) {
        val periodAsLong = period.toLong()

        val timerTask = object : TimerTask() {
            override fun run() {
                pdfApp.withLock {
                    if (pdfApp.paused) {
                        return
                    }
                    pdfApp.withLock {
                        pdfApp.currentpdfId?.let { pdfId ->
                            pdfApp.currentPage?.let { page ->
                                pdfApp.pageRecords[pdfId]?.let {
                                    it[page] = it[page]!! + periodAsLong
                                }
                            }
                        }

                        println(pdfApp.pageRecords)
                    }
                }
            }
        }

        timer = Timer()
        timer.scheduleAtFixedRate(timerTask, 0, periodAsLong)
    }

    fun tryAnalyticsMethods(call: MethodCall, result: Result): Boolean {
        when (call.method) {
            "disableAnalytics" -> {
                removeTimer()
                result.success(true)
            }
            "enableAnalytics" -> {
                removeTimer()
                scheduleTimer(call.arguments as Int)
                result.success(true)
            }
            "getAnalytics" -> {
                pdfApp.withLock {
                    result.success(
                            pdfApp.pageRecords[call.arguments as String? ?: pdfApp.currentpdfId]
                    )
                }
            }
            else -> {
                return false
            }
        }
        return true
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (tryAnalyticsMethods(call, result)) {
            return
        }

        val intent = Intent(registrar.context(), PdfActivity::class.java)
        val videoPages = call.argument<HashMap<Int, HashMap<String, String>>>("videoPages")

        videoPages?.mapValues { it: Map.Entry<Int, HashMap<String, String>> ->
            if (it.value["mode"] == "fromAsset") {
                it.value["src"] = registrar.lookupKeyForAsset(it.value["src"])
            }
            it.value
        }

        intent.putExtra("name", call.method)

        intent.putExtra("password", call.argument<String>("password"))
        intent.putExtra("xorDecryptKey", call.argument<String>("xorDecryptKey"))
        intent.putExtra("pdfId", call.argument<String>("pdfId"))
        intent.putExtra("videoPages", videoPages)
        listOf(
                "nightMode",
                "enableSwipe",
                "swipeHorizontal",
                "autoSpacing",
                "pageFling",
                "pageSnap",
                "enableImmersive",
                "autoPlay"
        ).forEach {
            intent.putExtra(it, call.argument<Boolean>(it)!!)
        }

        when (call.method) {
            "fromFile" -> {
                intent.putExtra("src", call.argument<String>("src")!!)
            }
            "fromBytes" -> {
                intent.putExtra("src", call.argument<Int>("src")!!)
            }
            "fromAsset" -> {
                intent.putExtra(
                        "src",
                        registrar.lookupKeyForAsset(call.argument<String>("src")!!)
                )
            }
            else -> {
                result.notImplemented()
                return
            }
        }

        registrar.activity().startActivity(intent)
        result.success(true)
    }
}
