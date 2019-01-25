package com.pycampers.flutterpdfviewer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.widget.FrameLayout
import android.widget.ImageButton
import com.github.barteksc.pdfviewer.PDFView
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
                .onRender(activity)
                .scrollHandle(scrollHandle)

        if (playerController.videoPages != null) {
            configurator = configurator
                    .onPageChange(playerController)
                    .onTap(playerController)
        }

        configurator.load()
    }
}

class PdfActivity : Activity(), OnLoadCompleteListener, OnRenderListener {

    lateinit var progressOverlay: FrameLayout
    lateinit var pdfView: PDFView
    lateinit var closeButton: ImageButton
    lateinit var opts: Bundle
    lateinit var pdfId: String
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

        playerController = PlayerController(
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
                Intent(ANALYTICS_BROADCAST_ACTION)
                        .putExtra("name", "pdfId")
                        .putExtra("value", pdfId)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }

    // stop player and save the current page to shared preferences
    override fun onStop() {
        super.onStop()
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
}
