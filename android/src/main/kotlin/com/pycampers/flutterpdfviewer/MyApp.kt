package com.pycampers.flutterpdfviewer

import io.flutter.app.FlutterApplication
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MyApp : FlutterApplication() {
    var currentPage: Int? = null
    var pageStartTime: Long? = null
    var pageRecord: MutableMap<Int, Long> = mutableMapOf()
    var paused = false
    val lock = ReentrantLock()

    inline fun <T> withLock(action: () -> T): T {
        return lock.withLock { action() }
    }
}
