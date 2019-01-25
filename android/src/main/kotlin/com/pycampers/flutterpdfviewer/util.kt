package com.pycampers.flutterpdfviewer

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.ByteArrayDataSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.*
import java.net.Socket
import kotlin.experimental.xor


fun readBytesFromSocket(size: Int, port: Int): ByteArray {
    val bytes = ByteArray(size)
    var done = 0

    Socket("localhost", port).use { socket ->
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

fun readBytesFromFile(pathname: String?): ByteArray {
    val file = File(pathname!!)
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

    (video["xorDecryptKey"] as String?)?.let {
        val bytes = when (mode) {
            "fromAsset" -> {
                readBytesFromAsset(context, src)
            }
            "fromFile" -> {
                readBytesFromFile(src)
            }
            else -> {
                throw IllegalArgumentException(
                        "provided video has incorrect mode (`$mode`)"
                )
            }
        }
        xorEncryptDecrypt(bytes, it)
        return getMediaSourceFromBytes(bytes)
    }

    val uri = when (mode) {
        "fromAsset" -> {
            Uri.fromFile(copyAssetToFile(context, src))
        }
        "fromFile" -> {
            Uri.parse(src)
        }
        else -> {
            throw IllegalArgumentException(
                    "provided video has incorrect mode (`$mode`)"
            )
        }
    }
    return getMediaSourceFromUri(context, uri)
}

