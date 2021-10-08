package com.intruder.detector

import android.graphics.RectF

/**
 * DetectionResult
 *      A class to store the visualization info of a detected object.
 */
data class DetectionResult(
    val boundingBox: RectF,
    val text: String
)