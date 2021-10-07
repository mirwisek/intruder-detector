package com.intruder.detector

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

@Throws(Exception::class)
fun Image.jpegImageToBitmap() : Bitmap {
    try {
        val buffer: ByteBuffer = this.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    } catch (e: Exception) {
        throw(e)
    }
}

fun log(msg: String, tag: String = "ffnet") {
    Log.i(tag, msg)
}