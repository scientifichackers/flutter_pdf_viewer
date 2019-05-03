package com.pycampers.flutterpdfviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pycampers.method_call_dispatcher.MethodCallDispatcher
import com.pycampers.method_call_dispatcher.trySend
import com.pycampers.method_call_dispatcher.trySendError
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.HashMap
import java.util.Timer
import java.util.TimerTask

typealias PageRecords = MutableMap<String, MutableMap<Int, Long>>
typealias VideoPages = HashMap<Int, HashMap<String, String>>

const val ANALYTICS_BROADCAST_ACTION = "pdf_viewer_analytics"
const val PDF_VIEWER_RESULT_ = "pdf_viewer_result"
const val TAG = "FlutterPdfViewer"

val stringArgs = listOf("password", "xorDecryptKey", "pdfId")
val booleanArgs = listOf(
    "nightMode",
    "enableSwipe",
    "swipeHorizontal",
    "autoSpacing",
    "pageFling",
    "pageSnap",
    "enableImmersive",
    "autoPlay",
    "forceLandscape"
)

class FlutterPdfViewerPlugin(val registrar: Registrar) : MethodCallDispatcher() {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "flutter_pdf_viewer")
            channel.setMethodCallHandler(FlutterPdfViewerPlugin(registrar))
        }
    }

    val context: Context = registrar.context()
    var timer = Timer()
    val broadcaster: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)
    var activityPaused = false
    var currentPdfId: String? = null
    var currentPage: Int? = null
    var pageRecords: PageRecords = mutableMapOf()

    fun handleAnalyticsBroadcast(args: Bundle) {
        when (args.getString("name")) {
            "page" -> {
                currentPage = args.getInt("value")
            }
            "activityPaused" -> {
                activityPaused = args.getBoolean("value")
            }
            else -> {
                throw IllegalArgumentException(
                    "Invalid method name - `${args.getString("name")}`"
                )
            }
        }
    }

    init {
        broadcaster.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    handleAnalyticsBroadcast(intent.extras!!)
                }
            },
            IntentFilter(ANALYTICS_BROADCAST_ACTION)
        )
    }

    fun stopAnalyticsTimer() {
        timer.cancel()
        timer.purge()
    }

    fun startAnalyticsTimer(period: Int) {
        val periodAsLong = period.toLong()

        val timerTask = object : TimerTask() {
            override fun run() {
                if (activityPaused) return

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

    fun disableAnalytics(call: MethodCall, result: Result) {
        trySend(result) { stopAnalyticsTimer() }
    }

    fun enableAnalytics(call: MethodCall, result: Result) {
        trySend(result) {
            stopAnalyticsTimer()
            startAnalyticsTimer(call.arguments as Int)
        }
    }

    fun getAnalytics(call: MethodCall, result: Result) {
        trySend(result) {
            val pdfId = call.arguments as String? ?: currentPdfId
            mapOf(pdfId to pageRecords[pdfId])
        }
    }

    fun getAllAnalytics(call: MethodCall, result: Result) {
        trySend(result) { pageRecords }
    }

    fun launchPdfActivity(call: MethodCall, result: Result) {
        val intent = Intent(context, PdfActivity::class.java)

        val resultId = intent.hashCode()
        intent.putExtra("resultId", resultId)
        call.argument<IntArray>("pages")?.let { intent.putExtra("pages", it) }
        stringArgs.forEach { intent.putExtra(it, call.argument<String>(it)) }
        booleanArgs.forEach { intent.putExtra(it, call.argument<Boolean>(it)!!) }

        val mode = call.argument<String>("mode")!!
        intent.putExtra("mode", mode)

        var src = call.argument<String>("src")!!
        src = when (mode) {
            "fromFile" -> Uri.parse(src).path!!
            "fromAsset" -> registrar.lookupKeyForAsset(src)
            "fromBytes" -> src
            else -> throw IllegalArgumentException("invalid mode: $mode.")
        }
        intent.putExtra("src", src)

        val videoPages = call.argument<VideoPages>("videoPages") ?: hashMapOf()
        videoPages.forEach {
            val vsrc = it.value["src"]!!
            it.value["src"] = when (it.value["mode"]) {
                "fromFile" -> Uri.parse(vsrc).path!!
                "fromAsset" -> registrar.lookupKeyForAsset(vsrc)
                else -> throw IllegalArgumentException("invalid mode: $mode.")
            }
        }
        intent.putExtra("videoPages", videoPages)

        broadcaster.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val extras = intent.extras!!
                    if (extras.containsKey("error")) {
                        trySendError(
                            result,
                            extras.getString("error"),
                            extras.getString("message"),
                            extras.getString("stacktrace")
                        )
                    } else {
                        currentPdfId = extras.getString("pdfId")
                        trySend(result)
                    }
                    broadcaster.unregisterReceiver(this)
                }
            },
            IntentFilter("$PDF_VIEWER_RESULT_$resultId")
        )

        registrar.activity().startActivity(intent)
    }
}
