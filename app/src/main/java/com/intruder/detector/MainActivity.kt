package com.intruder.detector

import android.content.pm.PackageManager
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.intruder.detector.databinding.ActivityMainBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS =
        arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    var textureView: TextureView? = null

    var currentImageType = Imgproc.COLOR_RGB2GRAY

    private lateinit var cameraHelper: CameraHelper

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        log("Passed camera trait")
        binding.cameraView.post {
            log("Starting camera")
            cameraHelper = CameraHelper(
                owner = this,
                context = this.applicationContext,
                viewFinder = binding.cameraView,
                onImageReceived = ::onImageReceived
            )

            cameraHelper.start()
        }

        val h = Handler(Looper.myLooper()!!)
        // Just for testing flash, controls
        h.postDelayed({
            cameraHelper.toggleFlash(true)
            h.postDelayed({
                cameraHelper.toggleFlash(false)
            }, 3000L)
        }, 1000L)



        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()

        val baseLoaderCB = object : BaseLoaderCallback(this) {
            override fun onManagerConnected(status: Int) {
                when (status) {
                    LoaderCallbackInterface.SUCCESS -> {
                        Log.i("ffnet", "Successfully connected")
                        try {
                            initializeOpenCVDependencies()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        super.onManagerConnected(status)
                    }
                }
            }
        }

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCB)
    }

    private fun onImageReceived(image: Image) {

//        val bitmap = image.jpegImageToBitmap()
//
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//        Imgproc.cvtColor(mat, mat, currentImageType)
//        Utils.matToBitmap(mat, bitmap)
//        log("Getting images")
//        runOnUiThread { ivBitmap.setImageBitmap(bitmap) }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initializeOpenCVDependencies() {
        System.loadLibrary("opencv_java")

        if (!OpenCVLoader.initDebug())
            Log.d("ffnet", "Unable to load OpenCV");
        else
            Log.d("ffnet", "OpenCV loaded");
    }

}