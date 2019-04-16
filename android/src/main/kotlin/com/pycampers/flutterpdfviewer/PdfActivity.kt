package com.pycampers.flutterpdfviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnErrorListener
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.github.barteksc.pdfviewer.listener.OnRenderListener
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.PlayerView

const val PDF_BYTES_PORT = 4567

// This thread is used to do heavy tasks, like loading the PDF from disk, decrypting it etc.
class PdfActivityThread(
        val activity: PdfActivity,
        val opts: Bundle,
        val pdfView: PDFView,
        val playerController: PlayerController,
        val scrollHandle: DefaultScrollHandle
) : Thread() {

    val methodName = opts.getString("name")!!
    val xorDecryptKey: String? = opts.getString("xorDecryptKey")

    fun buildConfigurator(): PDFView.Configurator? {
        xorDecryptKey?.let {
            val bytes = when (methodName) {
                "fromFile" -> {
                    readBytesFromFile(opts.getString("src"))
                }
                "fromBytes" -> {
                    readBytesFromSocket(opts.getInt("src"), PDF_BYTES_PORT)
                }
                "fromAsset" -> {
                    readBytesFromAsset(activity.applicationContext, opts.getString("src"))
                }
                else -> {
                    throw IllegalArgumentException("Invalid method name - `$methodName`.")
                }
            }
            xorEncryptDecrypt(bytes, it)
            return pdfView.fromBytes(bytes)
        }

        return when (methodName) {
            "fromFile" -> {
                pdfView.fromUri(Uri.parse(opts.getString("src")))
            }
            "fromBytes" -> {
                pdfView.fromBytes(readBytesFromSocket(opts.getInt("src"), PDF_BYTES_PORT))
            }
            "fromAsset" -> {
                pdfView.fromAsset(opts.getString("src"))
            }
            else -> {
                throw IllegalArgumentException("Invalid method name - `$methodName`.")
            }
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

typealias PageTranslator = MutableMap<Int, Int>?

fun buildPageTranslator(opts: Bundle): PageTranslator {
    if (!opts.containsKey("pages")) {
        return null
    }

    val pageTranslator: PageTranslator = mutableMapOf()

    var fakePage = 0
    for (actualPage in opts.getIntArray("pages")) {
        pageTranslator!![fakePage++] = actualPage
    }

    return pageTranslator
}

class PdfActivity : Activity(), OnLoadCompleteListener, OnRenderListener, OnErrorListener {

    lateinit var progressOverlay: FrameLayout
    lateinit var pdfView: PDFView
    lateinit var closeButton: ImageButton
    lateinit var opts: Bundle
    lateinit var pdfId: String
    var resultId: Int? = null
    lateinit var playerController: PlayerController
    lateinit var localBroadcastManager: LocalBroadcastManager

    var enableImmersive: Boolean = false
    val exoPlayer: ExoPlayer?
        get() = playerController.exoPlayer
    val isPlaying: Boolean
        get() = playerController.isPlaying

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        opts = intent.extras

        if (opts.getBoolean("forceLandscape")) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        enableImmersive = opts.getBoolean("enableImmersive")
        if (enableImmersive) {
            setTheme(android.R.style.Theme_DeviceDefault_NoActionBar_TranslucentDecor)
            goImmersive()
            initGestureDetector()
        } else {
            setTheme(android.R.style.Theme_DeviceDefault_NoActionBar)
        }

        setContentView(R.layout.pdf_viewer_layout)
        progressOverlay = findViewById(R.id.progress_overlay)
        progressOverlay.bringToFront()

        pdfView = findViewById(R.id.pdfView)
        val playerView = findViewById<PlayerView>(R.id.playerView)
        closeButton = findViewById(R.id.closeButton)

        pdfId = opts.getString("pdfId")
        resultId = opts.getInt("resultId")

        playerController = PlayerController(
                buildPageTranslator(opts),
                applicationContext,
                opts.get("videoPages") as HashMap<*, *>?,
                opts.getBoolean("autoPlay"),
                pdfView,
                playerView,
                closeButton
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext)

        PdfActivityThread(
                this,
                opts,
                pdfView,
                playerController,
                DefaultScrollHandle(this)
        ).start()
    }

    lateinit var gestureDetector: GestureDetector

    fun initGestureDetector() {
        class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                goImmersive()
                return super.onSingleTapUp(e)
            }
        }
        gestureDetector = GestureDetector(this, GestureListener())
    }

    fun goImmersive() {
        window.decorView.systemUiVisibility = (
                SYSTEM_UI_FLAG_IMMERSIVE
                        or SYSTEM_UI_FLAG_FULLSCREEN
                        or SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (enableImmersive) gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    override fun onBackPressed() {
        if (isPlaying) {
            closeButton.performClick()
        } else {
            super.onBackPressed()
        }
    }

    override fun loadComplete(nbPages: Int) {
        progressOverlay.visibility = View.GONE

        localBroadcastManager.sendBroadcast(
                Intent("pdf_viewer_result_$resultId")
                        .putExtra("pdfId", pdfId)
        )
    }

    override fun onError(t: Throwable) {
        localBroadcastManager.sendBroadcast(
                Intent("pdf_viewer_result_$resultId")
                        .putExtra("error", t.javaClass.canonicalName)
                        .putExtra("message", t.message)
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }

    var didRenderOnce = false

    // stop player and save the current page to shared preferences
    override fun onStop() {
        super.onStop()
        if (!didRenderOnce) {
            return
        }
        if (isPlaying) {
            exoPlayer?.playWhenReady = false
        }
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        with(sharedPref.edit()) {
            putInt(pdfId, pdfView.currentPage)
            apply()
        }
    }

    // jump to saved page on initial render of PDF
    override fun onInitiallyRendered(nbPages: Int) {
        val sharedPref = getPreferences(Context.MODE_PRIVATE) ?: return
        pdfView.jumpTo(sharedPref.getInt(pdfId, 0))
        didRenderOnce = true
    }

    override fun onPause() {
        localBroadcastManager.sendBroadcast(
                Intent(ANALYTICS_BROADCAST_ACTION)
                        .putExtra("name", "activityPaused")
                        .putExtra("value", true)
        )
        super.onPause()
    }

    override fun onResume() {
        localBroadcastManager.sendBroadcast(
                Intent(ANALYTICS_BROADCAST_ACTION)
                        .putExtra("name", "activityPaused")
                        .putExtra("value", false)
        )
        super.onResume()
    }

    fun nextPage() {
        if (pdfView.currentPage >= pdfView.pageCount - 1) return
        pdfView.jumpTo(pdfView.currentPage + 1)
    }

    fun prevPage() {
        if (pdfView.currentPage <= 0) return
        pdfView.jumpTo(pdfView.currentPage - 1)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> prevPage()
            KeyEvent.KEYCODE_DPAD_DOWN -> nextPage()
            KeyEvent.KEYCODE_DPAD_LEFT -> prevPage()
            KeyEvent.KEYCODE_DPAD_RIGHT -> nextPage()
        }
        return super.onKeyDown(keyCode, event)
    }
}
