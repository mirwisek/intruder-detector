package com.intruder.detector

import android.graphics.Matrix
import android.view.Surface
import android.view.TextureView

private fun TextureView.updateTransform() {
    val mx = Matrix()
    val w = this.measuredWidth.toFloat()
    val h = this.measuredHeight.toFloat()
    val cX = w / 2f
    val cY = h / 2f
    val rotationDgr: Int
    val rotation = this.rotation.toInt()
    rotationDgr = when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> return
    }
    mx.postRotate(rotationDgr.toFloat(), cX, cY)
    this.setTransform(mx)
}