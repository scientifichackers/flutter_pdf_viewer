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
    val myApp = registrar.context() as MyApp

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
                if (myApp.paused) {
                    return
                }
                myApp.pageRecord[myApp.currentPage ?: return] =
                        myApp.pageRecord.getOrPut(myApp.currentPage ?: return) { 2L } + periodAsLong

                println(myApp.pageRecord)
            }
        }

        timer = Timer()
        timer.scheduleAtFixedRate(timerTask, 0, periodAsLong)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "disableAnalytics" -> {
                removeTimer()
            }
            "enableAnalytics" -> {
                removeTimer()
                scheduleTimer(call.arguments as Int)
                return result.success(true)
            }
            "getAnalytics" -> {
                return result.success(myApp.pageRecord)
            }
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
        intent.putExtra("pdfHash", call.argument<String>("pdfHash"))
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
