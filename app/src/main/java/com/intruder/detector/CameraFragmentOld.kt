//package com.intruder.detector
//
//package com.intruder.detector
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.IntentFilter
//import android.content.res.Configuration
//import android.graphics.Bitmap
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.hardware.display.DisplayManager
//import android.media.MediaScannerConnection
//import android.os.Build
//import android.os.Bundle
//import android.util.DisplayMetrics
//import android.util.Log
//import android.util.Size
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.webkit.MimeTypeMap
//import android.widget.ImageButton
//import android.widget.ImageView
//import android.widget.ProgressBar
//import androidx.camera.core.*
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.content.ContextCompat
//import androidx.core.net.toUri
//import androidx.core.os.bundleOf
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.LifecycleOwner
//import androidx.localbroadcastmanager.content.LocalBroadcastManager
//import androidx.navigation.Navigation
//import com.google.android.material.floatingactionbutton.FloatingActionButton
//import com.google.mlkit.common.model.LocalModel
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.objects.ObjectDetection
//import com.google.mlkit.vision.objects.ObjectDetector
//import com.intruder.detector.databinding.FragmentCameraBinding
//import org.opencv.android.CameraActivity
//import java.io.File
//import java.io.FileOutputStream
//import java.text.SimpleDateFormat
//import java.util.*
//import java.util.concurrent.Executor
//import kotlin.math.abs
//import kotlin.math.max
//import kotlin.math.min
//
///**
// * Main fragment for this app. Implements all camera operations including:
// * - Viewfinder
// * - Photo taking
// */
//class CameraFragmentOld : Fragment() {
//
//    private lateinit var container: ConstraintLayout
//    private lateinit var viewFinder: PreviewView
//    private lateinit var outputDirectory: File
//    private lateinit var displayManager: DisplayManager
//    private lateinit var mainExecutor: Executor
//
//    private val RESOLUTION = Size(720, 1280)
//
//    private lateinit var imageAnalysis: ImageAnalysis
//    private lateinit var objectDetector: ObjectDetector
//
//
//    private lateinit var photoFile: File
//    private val COMPRESS_QUALITY = 60
//
//    private var displayId: Int = -1
//    private var preview: Preview? = null
//    private var imageCapture: ImageCapture? = null
//    private var camera: Camera? = null
//    private var captureButton: ImageButton? = null
//    private var retryCaptureButton: ImageButton? = null
//    private var progress: ProgressBar? = null
//    private var fabCheck: FloatingActionButton? = null
//    private var imgPreview: ImageView? = null
//
//    /**
//     * We need a display listener for orientation changes that do not trigger a configuration
//     * change, for example if we choose to override config change in manifest or for 180-degree
//     * orientation changes.
//     */
//    private val displayListener = object : DisplayManager.DisplayListener {
//        override fun onDisplayAdded(displayId: Int) = Unit
//        override fun onDisplayRemoved(displayId: Int) = Unit
//        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
//            if (displayId == this@CameraFragment.displayId) {
//                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
//                imageCapture?.targetRotation = view.display.rotation
//            }
//        } ?: Unit
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        mainExecutor = ContextCompat.getMainExecutor(requireContext())
//    }
//
//    override fun onResume() {
//        super.onResume()
//        // Make sure that all permissions are still present, since the
//        // user could have removed them while the app was in paused state.
//        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
//                CameraFragmentDirections.actionCameraToPermissions()
//            )
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        displayManager.unregisterDisplayListener(displayListener)
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//
//        val binding = FragmentCameraBinding.inflate(inflater, container, false)
//
//        return binding.root
//    }
//
//
//    private val imageCroppedAndSavedListener =
//        object : ImageCapture.OnImageCapturedCallback() {
//
//            override fun onCaptureSuccess(imageProxy: ImageProxy) {
//                try {
//
//                    // If the folder selected is an external media directory, this is unnecessary
//                    // but otherwise other apps will not be able to access our images unless we
//                    // scan them using [MediaScannerConnection]
//                    // to get extension
//                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
//                        context?.contentResolver?.getType(photoFile.toUri()))
//                    MediaScannerConnection.scanFile(
//                        context, arrayOf(photoFile.toUri().path), arrayOf(mimeType), null
//                    )
//
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                exception.printStackTrace()
//            }
//
//        }
//
//
//    @SuppressLint("MissingPermission")
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        container = view as ConstraintLayout
//        viewFinder = container.findViewById(R.id.view_finder)
//
//        // Every time the orientation of device changes, recompute layout
//        displayManager = viewFinder.context
//            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
//        displayManager.registerDisplayListener(displayListener, null)
//
//        // Wait for the views to be properly laid out
//
//        viewFinder.post {
//
//            // Keep track of the display in which this view is attached
//            displayId = viewFinder.display.displayId
//
//            // Bind use cases
//            bindCameraUseCases()
//        }
//    }
//
//    /**
//     * Inflate camera controls and update the UI manually upon config changes to avoid removing
//     * and re-adding the view finder from the view hierarchy; this provides a seamless rotation
//     * transition on devices that support it.
//     *
//     * NOTE: The flag is supported starting in Android 8 but there still is a small flash on the
//     * screen for devices that run Android 9 or below.
//     */
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//    }
//
//    /** Declare and bind preview, capture and analysis use cases */
//    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
//    private fun bindCameraUseCases() {
//
//        // Get screen metrics used to setup camera for full screen resolution
//        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
//        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
//
//        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
//        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")
//
//        val rotation = viewFinder.display.rotation
//
//        // Bind the CameraProvider to the LifeCycleOwner
//        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
//        cameraProviderFuture.addListener({
//
//            // CameraProvider
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            // Preview
//            preview = Preview.Builder()
//                // We request aspect ratio but no resolution
//                .setTargetAspectRatio(screenAspectRatio)
//                // Set initial target rotation
//                .setTargetRotation(rotation)
//                .build()
//
//            // Default PreviewSurfaceProvider
//            preview?.setSurfaceProvider(viewFinder.)
//            preview?.setSurfaceProvider(viewFinder.previewSurfaceProvider)
////            preview?.previewSurfaceProvider = viewFinder.previewSurfaceProvider
//
//            // Default ImageBufferFormat is JPEG
//            // can be changed with imageCapture.Builder().setBufferFormat(ImageFormat.YUV_420_888)
//            // YUV has high file size
//
//
//            imageAnalysis = ImageAnalysis.Builder()
//                .setTargetAspectRatio(screenAspectRatio)
////                    .setImageQueueDepth(1000)
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//
//
//            val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
//                val mediaImage = imageProxy.image
//                if (mediaImage != null) {
//                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//                    // Pass image to an ML Kit Vision API
//                    // ...
//                    objectDetector.process(image)
//                        .addOnFailureListener {
//                            log("[Custom Model] = Failed ${it.message}")
//                            it.printStackTrace()
//                        }.addOnSuccessListener {
//                            log("[Custom Model] = Results [${it.size}]:")
//                            it.forEach {  item ->
//                                item.labels.forEach { label ->
//                                    log("Labels[${label.index}] ${label.text} c=${label.confidence}")
//                                }
//                            }
//                        }
//                } else
//                    log("Image is null")
//            }
//
//            imageAnalysis.setAnalyzer(mainExecutor, imageAnalyzer)
//
//            // Must unbind the use-cases before rebinding them.
//            cameraProvider.unbindAll()
//
//            try {
//                // A variable number of use-cases can be passed here -
//                // camera provides access to CameraControl & CameraInfo
//                camera = cameraProvider.bindToLifecycle(
//                    this as LifecycleOwner, cameraSelector, preview, imageAnalysis
//                )
//            } catch (exc: Exception) {
//                Log.e(TAG, "Use case binding failed", exc)
//            }
//
//        }, mainExecutor)
//    }
//
//    /**
//     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
//     *  of preview ratio to one of the provided values.
//     *
//     *  @param width - preview width
//     *  @param height - preview height
//     *  @return suitable aspect ratio
//     */
//    private fun aspectRatio(width: Int, height: Int): Int {
//        val previewRatio = max(width, height).toDouble() / min(width, height)
//        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
//            return AspectRatio.RATIO_4_3
//        }
//        return AspectRatio.RATIO_16_9
//    }
//
//    companion object {
//
//        private const val TAG = "CameraFragment"
//
//        // No more needed, this will just prolong file name,
//        // instead of uid_timestamp as file name, we'll use uid (directory in fb storage) > timestamp
////        val user = FirebaseAuth.getInstance().currentUser?.uid.toString()
//        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
//        private const val PHOTO_EXTENSION = ".jpg"
//        private const val RATIO_4_3_VALUE = 4.0 / 3.0
//        private const val RATIO_16_9_VALUE = 16.0 / 9.0
//
//        /** Helper function used to create a timestamped file */
//        private fun createFile(baseFolder: File) =
//            File(baseFolder, SimpleDateFormat(FILENAME, Locale.US)
//                .format(System.currentTimeMillis()) + PHOTO_EXTENSION)
//    }
//}