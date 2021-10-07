package com.intruder.detector

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.Image
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max

class CameraHelper(
    private val owner: AppCompatActivity,
    private val context: Context,
    private val viewFinder: PreviewView,
    private val onImageReceived: (image: Image) -> Unit
) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null


    fun start() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(owner, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    fun stop() {
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({

            cameraProvider = cameraProviderFuture.get()

            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCameraUseCases() {

        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val previewView = getPreviewUseCase()
        val openCVAnalyzer = getOpenCVAnalyzer(onImageReceived)

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                owner,
                cameraSelector,
                previewView,
                openCVAnalyzer
            )

            previewView.setSurfaceProvider(viewFinder.surfaceProvider)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed $exc")
        }
    }

    fun toggleFlash(isOn: Boolean) {
        camera!!.cameraControl.enableTorch(isOn)
    }

    private fun aspectRatio(): Int {
        val displayMetrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        with(displayMetrics) {
            val previewRatio = max(widthPixels, heightPixels).toDouble() / widthPixels.coerceAtMost(
                heightPixels
            )
            if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
                return AspectRatio.RATIO_4_3
            }
            return AspectRatio.RATIO_16_9
        }
    }

    private fun hasBackCamera() =
        cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false

    private fun hasFrontCamera() =
        cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false

    private fun getPreviewUseCase(): Preview {
        return Preview.Builder()
            .setTargetAspectRatio(aspectRatio())
            .setTargetRotation(viewFinder.display.rotation)
            .build()
    }

    private fun getOpenCVAnalyzer(onImageReceived: (image: Image) -> Unit): ImageAnalysis {
        val analyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(aspectRatio())
            .setTargetRotation(viewFinder.display.rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setImageQueueDepth(10)
            .build()

        analyzer.setAnalyzer(cameraExecutor, OpenCVAnalyzer { image ->
            onImageReceived(image)
        })

        return analyzer
    }

    private class OpenCVAnalyzer(val listener: (image: Image) -> Unit) : ImageAnalysis.Analyzer {
//        private val listeners = ArrayList<BarcodeListener>().apply { listener?.let { add(it) } }

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            imageProxy.image?.let { mediaImage ->
                listener.invoke(mediaImage)
            }

//            if (mediaImage != null) {
//                val image =
//                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//                val scanner = BarcodeScanning.getClient(options)
//                scanner.process(image)
//                    .addOnSuccessListener { barcodes ->
//                        barcodes.forEach { barcode ->
//                            listeners.forEach { it(barcode.displayValue) }
//                        }
//                        imageProxy.close()
//                    }
//                    .addOnFailureListener {
//                        imageProxy.close()
//                    }
            imageProxy.close()
//            }
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    context, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 42
        val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        const val TAG = "CameraHelper"
    }

}
