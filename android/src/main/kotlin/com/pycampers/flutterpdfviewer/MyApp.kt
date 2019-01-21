package com.pycampers.flutterpdfviewer

import io.flutter.app.FlutterApplication

class MyApp : FlutterApplication() {
    var currentPage: Int? = null
    var pageStartTime: Long? = null
    var pageRecord: MutableMap<Int, Long> = mutableMapOf()
    var paused = false
}
