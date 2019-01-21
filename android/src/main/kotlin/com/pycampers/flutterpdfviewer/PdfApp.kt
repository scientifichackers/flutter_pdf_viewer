package com.pycampers.flutterpdfviewer

import io.flutter.app.FlutterApplication
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PdfApp : FlutterApplication() {
    var paused = false
    val lock = ReentrantLock()

    var currentpdfId: String? = null
    var pageRecords: MutableMap<String, MutableMap<Int, Long>> =
            mutableMapOf<String, MutableMap<Int, Long>>().withDefault { mutableMapOf() }

    inline fun <T> withLock(action: () -> T): T {
        return lock.withLock { action() }
    }

    private var _currentPage: Int? = null
    var currentPage: Int?
        get() {
            return _currentPage
        }
        set(value) {
            currentpdfId?.let { pdfId ->
                value?.let { page ->
                    pageRecords[pdfId]!![page] = 0L
                }
            }
            _currentPage = value
        }
}
