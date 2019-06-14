package com.pycampers.flutterpdfviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.pycampers.plugin_scaffold.createPluginScaffold
import com.pycampers.plugin_scaffold.trySend
import com.pycampers.plugin_scaffold.trySendError
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.HashMap

typealias VideoPages = HashMap<Int, HashMap<String, String>>

const val ANALYTICS_BROADCAST_ACTION = "pdf_viewer_analytics"
const val PDF_VIEWER_RESULT_ACTION = "pdf_viewer_result"
const val PDF_VIEWER_JUMP_ACTION = "pdf_viewer_jump"
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

class FlutterPdfViewerPlugin {
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val methods = FlutterPdfViewerMethods(registrar)
            val channel = createPluginScaffold(
                registrar.messenger(),
                "com.pycampers.flutter_pdf_viewer",
                methods
            )
            methods.analyticsCallback = { pdfId, pageIndex, activityPaused ->
                channel.invokeMethod("analyticsCallback", listOf(pdfId, pageIndex, activityPaused))
            }
            methods.atExit = { pdfId, pageIndex ->
                println("atExit!")
                channel.invokeMethod("atExit$pdfId", pageIndex)
            }
        }
    }
}

class FlutterPdfViewerMethods(val registrar: Registrar) {
    val context: Context = registrar.context()
    val broadcaster: LocalBroadcastManager = LocalBroadcastManager.getInstance(context)

    var analyticsCallback: ((String, Int, Boolean) -> Unit)? = null
    var atExit: ((String, Int) -> Unit)? = null

    var enabledAnalytics = false
    var activityPaused = false
    var pageIndex = 0

    fun invokeAnalyticsCallback(args: Bundle) {
        if (!enabledAnalytics) return
        analyticsCallback?.invoke(args.getString("pdfId")!!, pageIndex, activityPaused)
    }

    fun handleAnalyticsBroadcast(args: Bundle) {
        when (args.getString("name")) {
            "onDestroy" -> {
                atExit?.invoke(args.getString("pdfId")!!, pageIndex)
                return
            }
            "onPageChanged" -> {
                pageIndex = args.getInt("pageIndex")
            }
            "onPause" -> {
                activityPaused = true
            }
            "onResume" -> {
                activityPaused = false
            }
            else -> {
                throw IllegalArgumentException(
                    "Invalid method name - `${args.getString("name")}`"
                )
            }
        }
        invokeAnalyticsCallback(args)
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

    fun disableAnalytics(call: MethodCall, result: Result) {
        enabledAnalytics = false
        result.success(null)
    }

    fun enableAnalytics(call: MethodCall, result: Result) {
        enabledAnalytics = true
        result.success(null)
    }

    fun jumpToPage(call: MethodCall, result: Result) {
        broadcaster.sendBroadcast(
            Intent(PDF_VIEWER_JUMP_ACTION).run {
                putExtra("pageIndex", call.arguments as Int)
            }
        )
        result.success(null)
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
                    val args = intent.extras!!
                    if (args.containsKey("error")) {
                        trySendError(
                            result,
                            args.getString("error"),
                            args.getString("message"),
                            args.getString("stacktrace")
                        )
                    } else {
                        trySend(result)
                    }
                    broadcaster.unregisterReceiver(this)
                }
            },
            IntentFilter("$PDF_VIEWER_RESULT_ACTION$resultId")
        )

        registrar.activity().startActivity(intent)
    }
}