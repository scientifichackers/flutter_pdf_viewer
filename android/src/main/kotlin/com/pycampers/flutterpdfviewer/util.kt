package com.pycampers.flutterpdfviewer

import android.content.Context
import io.flutter.plugin.common.MethodCall
import java.io.*
import java.net.Socket
import kotlin.experimental.xor


@Throws(IOException::class)
fun readBytesFromSocket(pdfBytesSize: Int): ByteArray {
    val bytes = ByteArray(pdfBytesSize)

    val socket = Socket("0.0.0.0", 4567)
    val inputStream = socket.getInputStream()
    val bufferedInputStream = BufferedInputStream(inputStream)

    var readTillNow = 0
    while (readTillNow < pdfBytesSize) {
        readTillNow += bufferedInputStream.read(bytes, readTillNow, pdfBytesSize - readTillNow)
    }

    bufferedInputStream.close()
    inputStream.close()
    socket.close()

    return bytes
}

@Throws(IOException::class)
fun readBytesFromAsset(context: Context, pathname: String): ByteArray {
    val inputStream = context.assets.open(pathname)
    val bytes = ByteArray(inputStream.available())

    val dataInputStream = DataInputStream(inputStream)
    dataInputStream.readFully(bytes)

    dataInputStream.close()
    inputStream.close()

    return bytes
}

@Throws(IOException::class)
fun readBytesFromFile(pathname: String?): ByteArray {
    val file = File(pathname!!)
    val bytes = ByteArray(file.length().toInt())

    val dataInputStream = DataInputStream(FileInputStream(file))
    dataInputStream.readFully(bytes)

    dataInputStream.close()

    return bytes
}

fun xorEncryptDecrypt(bytes: ByteArray, key: String): ByteArray {
    val keyAsInt = key.map { it.toByte() }
    val len = key.length
    bytes.mapIndexed { index, byte -> byte xor keyAsInt[index % len] }

    return bytes
}



fun extractXorDecryptKey(methodCall: MethodCall): String? {
    return methodCall.argument<String>("xorDecryptKey")
}
