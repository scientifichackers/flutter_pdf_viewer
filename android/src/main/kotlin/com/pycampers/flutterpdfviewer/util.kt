package com.pycampers.flutterpdfviewer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.io.*
import java.net.Socket
import kotlin.experimental.xor


@Throws(IOException::class)
fun readBytesFromSocket(size: Int, port: Int): ByteArray {
    val bytes = ByteArray(size)
    var done = 0

    Socket("localhost", port).use {
        it.getInputStream().use {
            BufferedInputStream(it).use {
                while (done < size) {
                    done += it.read(bytes, done, size - done)
                }
            }
        }
    }

    return bytes
}

@Throws(IOException::class)
fun readBytesFromAsset(context: Context, assetPath: String): ByteArray {
    val inputStream = context.assets.open(assetPath)
    val bytes = ByteArray(inputStream.available())

    inputStream.use {
        DataInputStream(it).use {
            it.readFully(bytes)
        }
    }

    return bytes
}

@Throws(IOException::class)
fun readBytesFromFile(pathname: String?): ByteArray {
    val file = File(pathname!!)
    val bytes = ByteArray(file.length().toInt())

    FileInputStream(file).use {
        DataInputStream(it).use {
            it.readFully(bytes)
        }
    }

    return bytes
}

fun xorEncryptDecrypt(bytes: ByteArray, key: String): ByteArray {
    val keyAsInt = key.map { it.toByte() }
    val len = key.length

    bytes.mapIndexed { index, byte -> byte xor keyAsInt[index % len] }

    return bytes
}


fun getMediaSourceFromUri(context: Context, uri: Uri): MediaSource {
    return ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(
                    context,
                    Util.getUserAgent(context, "com.pycampers.flutterpdfviewer")
            )
    ).createMediaSource(uri)
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

fun getUriForVideoPage(context: Context, videoPage: HashMap<*, *>): Uri {
    val src = videoPage["src"] as String
    val mode = videoPage["mode"] as String
//    val xorDecryptKey = videoPage["xorDecryptKey"] as String

    return when (mode) {
        "fromAsset" -> Uri.fromFile(copyAssetToFile(context, src))
        "fromFile" -> Uri.parse(src)
        "fromUrl" -> Uri.parse(src)
        else -> {
            throw IllegalArgumentException(
                    "provided videoPage has incorrect mode (`$mode`)"
            )
        }
    }
}

