package com.pycampers.flutterpdfviewer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.HashMap


typealias PageRecords = MutableMap<String, MutableMap<Int, Long>>
typealias VideoPages = HashMap<Int, HashMap<String, String>>

const val ANALYTICS_BROADCAST_ACTION = "pdf_viewer_analytics"


class FlutterPdfViewerPlugin(val registrar: Registrar) : MethodCallHandler {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_pdf_viewer")
            channel.setMethodCallHandler(FlutterPdfViewerPlugin(registrar))
        }

        @SuppressLint("StaticFieldLeak")
        @JvmStatic
        var instance: FlutterPdfViewerPlugin? = null
    }

    val context: Context = registrar.context()
    var timer = Timer()
    val broadcastManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    var activityPaused = false
    var currentPdfId: String? = null
    var currentPage: Int? = null
    var pageRecords: PageRecords = mutableMapOf()

    fun handleBroadcast(args: Bundle) {
        when (args.getString("name")) {
            "page" -> {
                currentPage = args.getInt("value")
            }
            "activityPaused" -> {
                activityPaused = args.getBoolean("value")
            }
            "pdfId" -> {
                currentPdfId = args.getString("value")
            }
            else -> {
                throw IllegalArgumentException(
                        "Invalid method name - `${args.getString("name")}`"
                )
            }
        }
    }

    init {
        instance = this
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
                        if (pageRecords[pdfId] == null) {
                            pageRecords[pdfId] = mutableMapOf()
                        }
                        pageRecords[pdfId]!!.let {
                            val stored = it[page]
                            it[page] = if (stored != null) stored + periodAsLong else 0
                        }
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
        intent.putExtra("name", call.method)

        val videoPages = call.argument<VideoPages>("videoPages")
                ?.mapValues {
                    if (it.value["mode"] == "fromAsset") {
                        it.value["src"] = registrar.lookupKeyForAsset(it.value["src"])
                    }
                    it.value
                }
        intent.putExtra("videoPages", videoPages as HashMap)

        listOf(
                "nightMode",
                "enableSwipe",
                "swipeHorizontal",
                "autoSpacing",
                "pageFling",
                "pageSnap",
                "enableImmersive",
                "autoPlay",
                "forceLandscape"
        ).forEach { intent.putExtra(it, call.argument<Boolean>(it)!!) }
        listOf(
                "password",
                "xorDecryptKey",
                "pdfId"
        ).forEach { intent.putExtra(it, call.argument<String>(it)) }

        call.argument<IntArray>("pages")?.let { intent.putExtra("pages", it) }

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
