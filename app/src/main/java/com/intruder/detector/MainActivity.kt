package com.intruder.detector

import android.annotation.SuppressLint
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import com.intruder.detector.databinding.ActivityMainBinding
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.task.vision.detector.ObjectDetector


class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS =
        arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    var textureView: TextureView? = null

//    var currentImageType = Imgproc.COLOR_RGB2GRAY
    private lateinit var detector: ObjectDetector

    companion object {
        private const val MAX_FONT_SIZE = 96F
        const val TAG = "ffnet"
    }

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

        val options = ObjectDetector.ObjectDetectorOptions.builder()
//            .setMaxResults(3000)
            .setScoreThreshold(0.3f)
            .build()

        detector = ObjectDetector.createFromFileAndOptions(
            this,
            "lite-model_object_detection_mobile_object_localizer_v1_1_metadata_2.tflite",
            options
        )

//        val h = Handler(Looper.myLooper()!!)
//        // Just for testing flash, controls
//        h.postDelayed({
//            cameraHelper.toggleFlash(true)
//            h.postDelayed({
//                cameraHelper.toggleFlash(false)
//            }, 3000L)
//        }, 1000L)


//        val baseLoaderCB = object : BaseLoaderCallback(this) {
//            override fun onManagerConnected(status: Int) {
//                when (status) {
//                    LoaderCallbackInterface.SUCCESS -> {
//                        Log.i("ffnet", "Successfully connected")
//                        try {
//                            initializeOpenCVDependencies()
//                        } catch (e: IOException) {
//                            e.printStackTrace()
//                        }
//                    }
//                    else -> {
//                        super.onManagerConnected(status)
//                    }
//                }
//            }
//        }

//        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCB)
    }

//    private fun onImageReceived(image: Image) {
//
////        val bitmap = image.jpegImageToBitmap()
////
////        val mat = Mat()
////        Utils.bitmapToMat(bitmap, mat)
////        Imgproc.cvtColor(mat, mat, currentImageType)
////        Utils.matToBitmap(mat, bitmap)
////        log("Getting images")
////        runOnUiThread { ivBitmap.setImageBitmap(bitmap) }
//    }


    @SuppressLint("UnsafeOptInUsageError")
    private fun onImageReceived(imageProxy: ImageProxy) {
        log("Called onImageReceived")
        var bitmap: Bitmap? = null
//        try {
//            bitmap = image.jpegImageToBitmap()
//        } catch (e: Exception) {
//            log("Skipping a frame ${e.message}")
//            return
//        }
//
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//        Imgproc.cvtColor(mat, mat, currentImageType)
//        Utils.matToBitmap(mat, bitmap)
//        log("Getting images")
//        runOnUiThread { ivBitmap.setImageBitmap(bitmap) }


        imageProxy.image?.let { mediaImage ->
            if(mediaImage.format == ImageFormat.YUV_420_888) {
                bitmap = mediaImage.toBitmap()
            } else {
                bitmap = mediaImage.jpegImageToBitmap()
            }
        }

        if(bitmap == null) {
            log("Bitmap is null")
            return
        }
        // Step 1: Create TFLite's TensorImage object
        val img = TensorImage.fromBitmap(bitmap)

        // Step 3: Feed given image to the detector
        val results = detector.detect(img)
        debugPrint(results)
        // Closing only will allow next frame to be processed
        imageProxy.close()

        // Live detection and tracking
//        val options = ObjectDetectorOptions.Builder()
//            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//            .enableClassification()  // Optional
//            .build()


//        val objDetector = ObjectDetection.getClient(options)

//        objDetector.process(image)
//            .addOnSuccessListener {
//                log("Data $it")
//            }
//            .addOnFailureListener {
//                it.printStackTrace()
//                log("Error: ${it.message}")
//            }


//
//        // Step 4: Parse the detection result and show it
//        val resultToDisplay = results.map {
//            // Get the top-1 category and craft the display text
//            val category = it.categories.first()
//            val text = "${category.label}, ${category.score.times(100).toInt()}%"
//
//            // Create a data object to display the detection result
//            DetectionResult(it.boundingBox, text)
//        }
        // Draw the detection result on the bitmap and show it.
//        val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
//        runOnUiThread {
//            ivBitmap.setImageBitmap(imgWithResult)
//        }
    }

    /**
     * drawDetectionResult(bitmap: Bitmap, detectionResults: List<DetectionResult>
     *      Draw a box around each objects and show the object's name.
     */
    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        detectionResults.forEach {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = it.boundingBox
            canvas.drawRect(box, pen)


            val tagSize = Rect(0, 0, 0, 0)

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(it.text, 0, it.text.length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F
            canvas.drawText(
                it.text, box.left + margin,
                box.top + tagSize.height().times(1F), pen
            )
        }
        return outputBitmap
    }

    /**
     * debugPrint(visionObjects: List<Detection>)
     *      Print the detection result to logcat to examine
     */
    private fun debugPrint(results : List<Detection>) {
        log("Debugging")
        for ((i, obj) in results.withIndex()) {
            val box = obj.boundingBox

            Log.d(TAG, "Detected object: ${i} ")
            Log.d(TAG, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")

            var text = "boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})\n"
            for ((j, category) in obj.categories.withIndex()) {
                Log.d(TAG, "    Label $j: ${category.label} and Name: ${category.displayName}")
                text += "    Label $j: ${category.label} and Name: ${category.displayName}\n"
                val confidence: Int = category.score.times(100).toInt()
                text += "    Confidence: ${confidence}%"
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
            runOnUiThread {
                binding.textView.text = text
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraHelper.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

//    private fun initializeOpenCVDependencies() {
//        System.loadLibrary("opencv_java")
//
//        if (!OpenCVLoader.initDebug())
//            Log.d("ffnet", "Unable to load OpenCV");
//        else
//            Log.d("ffnet", "OpenCV loaded");
//    }


}