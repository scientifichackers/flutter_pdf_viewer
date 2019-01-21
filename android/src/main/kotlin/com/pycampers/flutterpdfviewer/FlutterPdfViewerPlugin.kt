package com.pycampers.flutterpdfviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.IllegalArgumentException
import java.util.*


typealias PageRecords = MutableMap<String, MutableMap<Int, Long>>

const val ANALYTICS_BROADCAST_ACTION = "pdf_viewer_analytics"


class FlutterPdfViewerPlugin private constructor(val registrar: Registrar) : MethodCallHandler {

    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_pdf_viewer")
            channel.setMethodCallHandler(FlutterPdfViewerPlugin(registrar))
        }
    }

    val context: Context = registrar.context()
    var timer = Timer()
    val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    var activityPaused = false
    var currentPdfId: String? = null
    var currentPage: Int? = null
    var pageRecords: PageRecords = mutableMapOf()

    fun handleBroadcast(args: Bundle) {
        println("handleBroadcast - $args")

        when (args.getString("name")) {
            "page" -> {
                val page = args.getInt("value")
                currentPage = page
                currentPdfId.let {
                    pageRecords[it]?.let {
                        if (!it.containsKey(page)) {
                            it[page] = 0L
                        }
                    }
                }
            }
            "activityPaused" -> {
                activityPaused = args.getBoolean("value")
            }
            "pdfId" -> {
                currentPdfId = args.getString("value")
                currentPdfId?.let {
                    if (!pageRecords.containsKey(it)) {
                        pageRecords[it] = mutableMapOf()
                    }
                }
            }
            else -> {
                throw IllegalArgumentException(
                        "Invalid method name - `${args.getString("name")}`"
                )
            }
        }
    }

    init {
        broadcastManager.registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        handleBroadcast(intent.extras)
                    }
                },
                IntentFilter(ANALYTICS_BROADCAST_ACTION)
        )
    }

    fun removeTimer() {
        timer.cancel()
        timer.purge()
    }

    fun scheduleTimer(period: Int) {
        val periodAsLong = period.toLong()

        val timerTask = object : TimerTask() {
            override fun run() {
                if (activityPaused) {
                    return
                }

                currentPdfId?.let { pdfId ->
                    currentPage?.let { page ->
                        println(pageRecords[pdfId])
                        pageRecords[pdfId]?.let {
                            it[page] = it[page]!! + periodAsLong
                        }
                    }
                }

                println(pageRecords)
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
                result.success(
                        pageRecords[call.arguments as String? ?: currentPdfId]
                )
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

        val intent = Intent(context, PdfActivity::class.java)
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
