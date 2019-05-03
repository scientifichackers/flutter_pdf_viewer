package com.pycampers.flutterpdfviewer

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import kotlin.experimental.xor

fun readBytesFromSocket(src: String): ByteArray {
    val parts = src.split(",")
    val address = parts[0]
    val port = parts[1].toInt()
    val size = parts[2].toInt()

    val bytes = ByteArray(size)
    var done = 0

    Socket(address, port).use { socket ->
        socket.getInputStream().use { inputStream ->
            BufferedInputStream(inputStream).use {
                while (done < size) {
                    done += it.read(bytes, done, size - done)
                }
            }
        }
    }

    return bytes
}

fun readBytesFromAsset(context: Context, assetPath: String): ByteArray {
    val inputStream = context.assets.open(assetPath)
    val bytes = ByteArray(inputStream.available())

    inputStream.use { stream ->
        DataInputStream(stream).use {
            it.readFully(bytes)
        }
    }

    return bytes
}

fun readBytesFromFile(path: String): ByteArray {
    val file = File(path)
    val bytes = ByteArray(file.length().toInt())

    FileInputStream(file).use { fileInputStream ->
        DataInputStream(fileInputStream).use {
            it.readFully(bytes)
        }
    }

    return bytes
}

fun xorEncryptDecrypt(bytes: ByteArray, key: String) {
    val keyAsIntList = key.map { it.toByte() }
    val keyLength = key.length
    for (i in 0 until bytes.size) {
        bytes[i] = bytes[i] xor keyAsIntList[i % keyLength]
    }
}

fun copyAssetToFile(context: Context, assetPath: String): File {
    val outFile = File(context.cacheDir, assetPath.hashCode().toString())
    outFile.createNewFile()

    context.assets.open(assetPath).use { input ->
        FileOutputStream(outFile).use { output ->
            input.copyTo(output)
        }
    }

    return outFile
}

fun getMediaSourceFromUri(context: Context, uri: Uri): MediaSource {
    return ExtractorMediaSource
        .Factory(
            DefaultDataSourceFactory(
                context,
                Util.getUserAgent(context, "com.pycampers.flutterpdfviewer")
            )
        )
        .createMediaSource(uri)
}

fun getMediaSourceFromBytes(bytes: ByteArray): MediaSource {
    return ExtractorMediaSource
        .Factory(DataSource.Factory { ByteArrayDataSource(bytes) })
        .createMediaSource(Uri.EMPTY)
}

fun getMediaSourceForVideo(context: Context, video: HashMap<*, *>): MediaSource {
    val src = video["src"] as String
    val mode = video["mode"] as String
    val xorDecryptKey = video["xorDecryptKey"] as String?

    xorDecryptKey?.let {
        val bytes = when (mode) {
            "fromFile" -> {
                Log.d(TAG, "loading encrypted video from file { $src }...")
                readBytesFromFile(src)
            }
            "fromAsset" -> {
                Log.d(TAG, "loading encrypted video from asset { $src }...")
                readBytesFromAsset(context, src)
            }
            else -> throw IllegalArgumentException("invalid mode: $mode")
        }
        xorEncryptDecrypt(bytes, it)
        return getMediaSourceFromBytes(bytes)
    }

    val uri = when (mode) {
        "fromFile" -> {
            val uri = Uri.parse(src)
            Log.d(TAG, "loading video from file { $uri }...")
            uri
        }
        "fromAsset" -> {
            Log.d(TAG, "loading video from asset { $src }...")
            Uri.fromFile(copyAssetToFile(context, src))
        }
        else -> throw IllegalArgumentException("invalid mode: $mode")
    }
    return getMediaSourceFromUri(context, uri)
}

