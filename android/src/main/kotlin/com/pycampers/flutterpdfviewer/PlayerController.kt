package com.pycampers.flutterpdfviewer

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener
import com.github.barteksc.pdfviewer.listener.OnTapListener
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.ui.PlayerView


class PlayerController(
        val context: Context,
        val videoPages: HashMap<*, *>?,
        val autoPlay: Boolean,
        val pdfView: PDFView,
        val playerView: PlayerView,
        val closeButton: ImageButton
) : OnPageChangeListener, OnTapListener {

    var exoPlayer: ExoPlayer? = null
    var isPlaying = false

    var wasFakeJump = false
    var lastVideoPage: Int? = null

    val myApp = context as MyApp

    companion object {
        @JvmStatic
        val TAG: String = PlayerController::class.java.simpleName
    }

    init {
        //
        // setup gestures to hide/show the close button
        //

        class GestureListener : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                if (playerView.isControllerVisible) {
                    playerView.hideController()
                    closeButton.visibility = View.GONE
                } else {
                    playerView.showController()
                    closeButton.visibility = View.VISIBLE
                }
                return super.onSingleTapUp(e)
            }
        }

        val gestureDetector = GestureDetector(context, GestureListener())
        playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        //
        // define close button behavior
        //

        closeButton.setOnClickListener {
            hidePlayer()
            stopPlayer()

            lastVideoPage?.let { page ->
                wasFakeJump = true
                pdfView.stopFling()
                pdfView.jumpTo(page)
            }
        }
    }

    fun hidePlayer() {
        pdfView.visibility = View.VISIBLE
        playerView.visibility = View.GONE
        closeButton.visibility = View.GONE
    }

    fun showPlayer() {
        pdfView.visibility = View.GONE
        playerView.visibility = View.VISIBLE
        closeButton.visibility = View.VISIBLE
    }

    fun startPlayer(mediaSource: MediaSource) {
        exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        playerView.player = exoPlayer
        exoPlayer?.prepare(mediaSource)
        exoPlayer?.playWhenReady = true
        isPlaying = true
    }

    fun stopPlayer() {
        exoPlayer?.release()
        isPlaying = false
    }

    var shouldPlayVideo = false
    lateinit var videoToByPlayed: HashMap<*, *>

    override fun onPageChanged(page: Int, pageCount: Int) {
        shouldPlayVideo = false
        if (wasFakeJump) {
            wasFakeJump = false
            return
        }

        myApp.withLock {
            myApp.currentPage = page + 1
            myApp.pageStartTime = System.nanoTime()
        }

        val video = videoPages!![page + 1] as HashMap<*, *>?
        if (video == null) {
            if (isPlaying) {
                hidePlayer()
                stopPlayer()
            }
            return
        }
0
        if (autoPlay) {
            playVideo(video, page)
        } else {
            videoToByPlayed = video
            shouldPlayVideo = true
        }
    }

    fun playVideo(video: HashMap<*, *>, page: Int) {
        showPlayer()
        startPlayer(getMediaSourceFromUri(context, getUriForVideoPage(context, video)))
        lastVideoPage = page
    }

    override fun onTap(e: MotionEvent?): Boolean {
        if (shouldPlayVideo) {
            playVideo(videoToByPlayed, pdfView.currentPage)
            shouldPlayVideo = false
            return true
        }
        return false
    }
}