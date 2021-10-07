//package com.p4f.objecttracking
////import android.text.Spannable;
////import android.text.SpannableStringBuilder;
////import android.text.style.ForegroundColorSpan;
//import android.Manifest
//import android.R
//import android.app.Activity
//import android.app.AlertDialog
//import android.app.Dialog
//import android.bluetooth.BluetoothAdapter
//import android.bluetooth.BluetoothDevice
//import android.content.*
//import android.content.pm.PackageManager
//import android.graphics.*
//import android.graphics.Point
//import android.hardware.camera2.*
//import android.media.ImageReader
//import android.os.Bundle
//import android.os.Handler
//import android.os.HandlerThread
//import android.os.IBinder
//import android.util.Log
//import android.util.SparseIntArray
//import android.view.*
//import android.view.TextureView.SurfaceTextureListener
//import android.view.View.OnTouchListener
//import android.widget.*
//import androidx.core.app.ActivityCompat
//import androidx.fragment.app.Fragment
//import com.intruder.detector.databinding.FragmentCameraBinding
//import org.opencv.android.OpenCVLoader
//import org.opencv.android.Utils
//import org.opencv.core.*
//import org.opencv.core.Rect
//import org.opencv.imgcodecs.Imgcodecs
//import org.opencv.imgproc.Imgproc
//import org.opencv.tracking.Tracker
//import org.opencv.tracking.TrackerCSRT
//import org.opencv.tracking.TrackerKCF
//import org.opencv.tracking.TrackerMIL
//import org.opencv.tracking.TrackerMOSSE
//import org.opencv.tracking.TrackerMedianFlow
//import org.opencv.tracking.TrackerTLD
//import java.lang.Exception
//import java.lang.RuntimeException
//import java.util.*
//import java.util.concurrent.Semaphore
//import java.util.concurrent.TimeUnit
//
//class CameraFragment : Fragment() {
//    val TAG = "CameraFragment"
//    private var mTextureView: TextureView? = null
//
//    companion object {
//        private val ORIENTATIONS = SparseIntArray()
//        private const val REQUEST_CAMERA_PERMISSION = 200
//
//        init {
//            ORIENTATIONS.append(Surface.ROTATION_0, 90)
//            ORIENTATIONS.append(Surface.ROTATION_90, 0)
//            ORIENTATIONS.append(Surface.ROTATION_180, 270)
//            ORIENTATIONS.append(Surface.ROTATION_270, 180)
//        }
//    }
//
//    internal enum class Drawing {
//        DRAWING, TRACKING, CLEAR
//    }
//
//    //camera device
//    private var cameraId: String? = null
//    private var cameraDevice: CameraDevice? = null
//    private var previewRequestBuilder: CaptureRequest.Builder? = null
//    private var previewRequest: CaptureRequest? = null
//    private var imageDimension: android.util.Size? = null
//    private var imageReader: ImageReader? = null
//    private var mBackgroundHandler: Handler? = null
//    private var mBackgroundThread: HandlerThread? = null
//    private val CamResolution = android.util.Size(1280, 720)
//    private var mCaptureSession: CameraCaptureSession? = null
//
//    /** this prevent the app from exiting before closing the camera.  */
//    private val cameraOpenCloseLock = Semaphore(1)
//    private var mImageGrabInit: Mat? = null
//    private var mImageGrab: Mat? = null
//    private val mBitmapGrab: Bitmap? = null
//    private var mTrackingOverlay: OverlayView? = null
//    private var mInitRectangle: Rect2d? = null
//    private val mPoints = arrayOfNulls<Point>(2)
//    private var mProcessing = false
//    private var mDrawing = Drawing.DRAWING
//    private var mTargetLocked = false
//    private var mShowCordinate = false
//    private var mTracker: Tracker? = null
//    private var mMenu: Menu? = null
//
//    //bluetooth device
//    private enum class Connected {
//        False, Pending, True
//    }
//
//    private val newline = "\r\n"
//    private val receiveText: TextView? = null
//    private val initialStart = true
//    private var connected = Connected.False
//    private val REQUEST_CONNECT_CODE = 68
//    private var mBluetoothDevAddr: String? = ""
//    private var mSelectedTracker = "TrackerMedianFlow"
//    var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
//        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//            //open your camera here
//            openCamera()
//        }
//
//        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
//            // Transform you image captured size according to the surface width and height
//        }
//
//        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//            return false
//        }
//
//        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                // close the app
//                Toast.makeText(
//                    activity,
//                    "Sorry!!!, you can't use this app without granting permission",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setHasOptionsMenu(true)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        Log.e(TAG, "onResume")
//        startBackgroundThread()
//        if (mTextureView!!.isAvailable) {
//            openCamera()
//        } else {
//            mTextureView!!.surfaceTextureListener = textureListener
//        }
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, activity, null)
//        }
//    }
//
//    override fun onPause() {
//        Log.e(TAG, "onPause")
//        //closeCamera();
//        stopBackgroundThread()
//        super.onPause()
//    }
//
//    override fun onDestroy() {
//        Log.e(TAG, "onDestroy")
//        closeCamera()
//        super.onDestroy()
//    }
//
//    protected fun startBackgroundThread() {
//        mBackgroundThread = HandlerThread("Camera Background")
//        mBackgroundThread!!.start()
//        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
//    }
//
//    protected fun stopBackgroundThread() {
//        mBackgroundThread!!.quitSafely()
//        try {
//            mBackgroundThread!!.join()
//            mBackgroundThread = null
//            mBackgroundHandler = null
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//    }
//
//    /*
//     * UI
//     */
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        val binding = FragmentCameraBinding.inflate(inflater, container, false)
//        mTextureView = binding.texture
////        mTrackingOverlay = binding.trackingOverlay as OverlayView
//        assert(mTextureView != null && mTrackingOverlay != null)
//        mTextureView!!.surfaceTextureListener = textureListener
//        return view
//    }
//
//    private val captureCallback: CameraCaptureSession.CaptureCallback =
//        object : CameraCaptureSession.CaptureCallback() {
//            override fun onCaptureProgressed(
//                session: CameraCaptureSession,
//                request: CaptureRequest,
//                partialResult: CaptureResult
//            ) {
//            }
//
//            override fun onCaptureCompleted(
//                session: CameraCaptureSession,
//                request: CaptureRequest,
//                result: TotalCaptureResult
//            ) {
//            }
//        }
//    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
//        override fun onOpened(camera: CameraDevice) {
//            cameraOpenCloseLock.release()
//            //This is called when the camera is open
//            Log.e(TAG, "onOpened")
//            cameraDevice = camera
//            createCameraPreview()
//        }
//
//        override fun onDisconnected(camera: CameraDevice) {
//            cameraOpenCloseLock.release()
//            cameraDevice!!.close()
//        }
//
//        override fun onError(camera: CameraDevice, error: Int) {
//            cameraOpenCloseLock.release()
//            cameraDevice!!.close()
//            cameraDevice = null
//        }
//    }
//    protected var onImageAvailableListener =
//        ImageReader.OnImageAvailableListener { reader ->
//            val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener
//            if (mProcessing) {
//                image.close()
//                return@OnImageAvailableListener
//            }
//            mProcessing = true
//
//            //            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//            //            byte[] bytes = new byte[buffer.capacity()];
//            //            buffer.get(bytes);
//            //            Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
//            if (mTargetLocked) {
//                // image to byte array
//                val bb = image.planes[0].buffer
//                val data = ByteArray(bb.remaining())
//                bb[data]
//                mImageGrab = Imgcodecs.imdecode(MatOfByte(*data), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
//                Core.transpose(mImageGrab, mImageGrab)
//                Core.flip(mImageGrab, mImageGrab, 1)
//                Imgproc.resize(mImageGrab, mImageGrab, Size(240, 320))
//            }
//            //            Bitmap bmp = null;
//            //            Mat tmp = new Mat (mImageGrab.rows(), mImageGrab.cols(), CvType.CV_8U, new Scalar(4));
//            //            try {
//            //                Imgproc.cvtColor(mImageGrab, tmp, Imgproc.COLOR_RGB2BGRA);
//            //                bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
//            //                Utils.matToBitmap(tmp, bmp);
//            //            }
//            //            catch (CvException e){
//            //                Log.d("Exception",e.getMessage());
//            //            }
//            image.close()
//            processing()
//        }
//
//    private fun processing() {
//        //TODO:do processing
//        // Get the features for tracking
//        if (mTargetLocked) {
//            if (mDrawing == Drawing.DRAWING) {
//                val minX = (Math.min(mPoints[0]!!.x, mPoints[1]!!.x)
//                    .toFloat() / mTrackingOverlay.getWidth() * mImageGrab!!.cols()) as Int
//                val minY = (Math.min(mPoints[0]!!.y, mPoints[1]!!.y)
//                    .toFloat() / mTrackingOverlay.getHeight() * mImageGrab!!.rows()) as Int
//                val maxX = (Math.max(mPoints[0]!!.x, mPoints[1]!!.x)
//                    .toFloat() / mTrackingOverlay.getWidth() * mImageGrab!!.cols()) as Int
//                val maxY = (Math.max(mPoints[0]!!.y, mPoints[1]!!.y)
//                    .toFloat() / mTrackingOverlay.getHeight() * mImageGrab!!.rows()) as Int
//                mInitRectangle = Rect2d(
//                    minX.toDouble(), minY.toDouble(),
//                    (maxX - minX).toDouble(), (maxY - minY).toDouble()
//                )
//                mImageGrabInit = Mat()
//                mImageGrab!!.copyTo(mImageGrabInit)
//                if (mSelectedTracker == "TrackerMedianFlow") {
//                    mTracker = TrackerMedianFlow.create()
//                } else if (mSelectedTracker == "TrackerCSRT") {
//                    mTracker = TrackerCSRT.create()
//                } else if (mSelectedTracker == "TrackerKCF") {
//                    mTracker = TrackerKCF.create()
//                } else if (mSelectedTracker == "TrackerMOSSE") {
//                    mTracker = TrackerMOSSE.create()
//                } else if (mSelectedTracker == "TrackerTLD") {
//                    mTracker = TrackerTLD.create()
//                } else if (mSelectedTracker == "TrackerMIL") {
//                    mTracker = TrackerMIL.create()
//                }
//                mTracker.init(mImageGrabInit, mInitRectangle)
//                mDrawing = Drawing.TRACKING
//
//                //TODO: DEBUG
//                val testRect = Rect(minX, minY, maxX - minX, maxY - minY)
//                val roi = Mat(mImageGrab, testRect)
//                var bmp: Bitmap? = null
//                val tmp = Mat(roi.rows(), roi.cols(), CvType.CV_8U, Scalar(4))
//                try {
//                    Imgproc.cvtColor(roi, tmp, Imgproc.COLOR_RGB2BGRA)
//                    bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888)
//                    Utils.matToBitmap(tmp, bmp)
//                } catch (e: CvException) {
//                    Log.d("Exception", e.message!!)
//                }
//            } else {
//                val trackingRectangle = Rect2d(0, 0, 1, 1)
//                mTracker.update(mImageGrab, trackingRectangle)
//
////                //TODO: DEBUG
////                org.opencv.core.Rect testRect = new org.opencv.core.Rect((int)trackingRectangle.x,
////                                                                        (int)trackingRectangle.y,
////                                                                        (int)trackingRectangle.width,
////                                                                        (int)trackingRectangle.height);
////                Mat roi = new Mat(mImageGrab, testRect);
////                Bitmap bmp = null;
////                Mat tmp = new Mat (roi.rows(), roi.cols(), CvType.CV_8U, new Scalar(4));
////                try {
////                    Imgproc.cvtColor(roi, tmp, Imgproc.COLOR_RGB2BGRA);
////                    bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888);
////                    Utils.matToBitmap(tmp, bmp);
////                }
////                catch (CvException e){
////                    Log.d("Exception",e.getMessage());
////                    mTargetLocked = false;
////                    mDrawing = Drawing.DRAWING;
////                }
//                mPoints[0]!!.x =
//                    (trackingRectangle.x * mTrackingOverlay.getWidth() as Float / mImageGrab!!.cols()
//                        .toFloat()).toInt()
//                mPoints[0]!!.y =
//                    (trackingRectangle.y * mTrackingOverlay.getHeight() as Float / mImageGrab!!.rows()
//                        .toFloat()).toInt()
//                mPoints[1]!!.x =
//                    mPoints[0]!!.x + (trackingRectangle.width * mTrackingOverlay.getWidth() as Float / mImageGrab!!.cols()
//                        .toFloat()).toInt()
//                mPoints[1]!!.y =
//                    mPoints[0]!!.y + (trackingRectangle.height * mTrackingOverlay.getHeight() as Float / mImageGrab!!.rows()
//                        .toFloat()).toInt()
//                mTrackingOverlay.postInvalidate()
//                if (connected == Connected.True) {
//                    val dataBle = Integer.toString((mPoints[0]!!.x + mPoints[1]!!.x) / 2) + "," +
//                            Integer.toString(mTrackingOverlay.getWidth()) + "," +
//                            Integer.toString((mPoints[0]!!.y + mPoints[1]!!.y) / 2) + "," +
//                            Integer.toString(mTrackingOverlay.getHeight())
//                    sendBLE(dataBle)
//                }
//            }
//        } else {
//            if (mTracker != null) {
//                mTracker.clear()
//                mTracker = null
//            }
//        }
//        mProcessing = false
//    }
//
//    protected fun createCameraPreview() {
//        try {
//            val texture = mTextureView!!.surfaceTexture!!
//            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
//            val surface = Surface(texture)
//            previewRequestBuilder =
//                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//            previewRequestBuilder!!.addTarget(surface)
//            imageReader = ImageReader.newInstance(
//                CamResolution.width,
//                CamResolution.height,
//                ImageFormat.JPEG,
//                2
//            )
//            imageReader!!.setOnImageAvailableListener(onImageAvailableListener, mBackgroundHandler)
//            previewRequestBuilder!!.addTarget(imageReader!!.surface)
//            cameraDevice!!.createCaptureSession(
//                Arrays.asList(surface, imageReader!!.surface),
//                object : CameraCaptureSession.StateCallback() {
//                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
//                        //The camera is already closed
//                        if (null == cameraDevice) {
//                            return
//                        }
//                        mCaptureSession = cameraCaptureSession
//                        // Auto focus should be continuous for camera preview.
//                        previewRequestBuilder!!.set(
//                            CaptureRequest.CONTROL_AF_MODE,
//                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
//                        )
//                        // Flash is automatically enabled when necessary.
//                        previewRequestBuilder!!.set(
//                            CaptureRequest.CONTROL_AE_MODE,
//                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
//                        )
//
//                        // Finally, we start displaying the camera preview.
//                        previewRequest = previewRequestBuilder!!.build()
//                        try {
//                            cameraCaptureSession.setRepeatingRequest(
//                                previewRequest!!,
//                                captureCallback,
//                                mBackgroundHandler
//                            )
//                        } catch (e: CameraAccessException) {
//                            e.printStackTrace()
//                        }
//                    }
//
//                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
//                        Toast.makeText(activity, "Configuration change", Toast.LENGTH_SHORT).show()
//                    }
//                },
//                null
//            )
//            for (i in mPoints.indices) {
//                mPoints[i] = Point(0, 0)
//            }
//            mTrackingOverlay.addCallback(
//                object : DrawCallback() {
//                    fun drawCallback(canvas: Canvas) {
//                        if (mDrawing != Drawing.CLEAR) {
//                            val paint = Paint()
//                            paint.color = Color.rgb(0, 0, 255)
//                            paint.strokeWidth = 10f
//                            paint.style = Paint.Style.STROKE
//                            canvas.drawRect(
//                                mPoints[0]!!.x.toFloat(),
//                                mPoints[0]!!.y.toFloat(),
//                                mPoints[1]!!.x.toFloat(),
//                                mPoints[1]!!.y.toFloat(),
//                                paint
//                            )
//                            if (mDrawing == Drawing.TRACKING && mShowCordinate == true) {
//                                paint.color = Color.rgb(0, 255, 0)
//                                canvas.drawLine(
//                                    ((mPoints[0]!!.x + mPoints[1]!!.x) / 2).toFloat(), 0f, (
//                                            (mPoints[0]!!.x + mPoints[1]!!.x) / 2).toFloat(),
//                                    mTrackingOverlay.getHeight(),
//                                    paint
//                                )
//                                canvas.drawLine(
//                                    0f, (
//                                            (mPoints[0]!!.y + mPoints[1]!!.y) / 2).toFloat(),
//                                    mTrackingOverlay.getWidth(), (
//                                            (mPoints[0]!!.y + mPoints[1]!!.y) / 2).toFloat(),
//                                    paint
//                                )
//                                paint.color = Color.YELLOW
//                                paint.strokeWidth = 2f
//                                paint.style = Paint.Style.FILL
//                                paint.textSize = 30f
//                                val strX =
//                                    Integer.toString((mPoints[0]!!.x + mPoints[1]!!.x) / 2) + "/" + Integer.toString(
//                                        mTrackingOverlay.getWidth()
//                                    )
//                                val strY =
//                                    Integer.toString((mPoints[0]!!.y + mPoints[1]!!.y) / 2) + "/" + Integer.toString(
//                                        mTrackingOverlay.getHeight()
//                                    )
//                                canvas.drawText(
//                                    strX,
//                                    ((mPoints[0]!!.x + mPoints[1]!!.x) / 4).toFloat(),
//                                    ((mPoints[0]!!.y + mPoints[1]!!.y) / 2 - 10).toFloat(),
//                                    paint
//                                )
//                                canvas.save()
//                                canvas.rotate(
//                                    90f,
//                                    ((mPoints[0]!!.x + mPoints[1]!!.x) / 2 + 10).toFloat(),
//                                    ((mPoints[0]!!.y + mPoints[1]!!.y) / 4).toFloat()
//                                )
//                                canvas.drawText(
//                                    strY,
//                                    ((mPoints[0]!!.x + mPoints[1]!!.x) / 2 + 10).toFloat(),
//                                    ((mPoints[0]!!.y + mPoints[1]!!.y) / 4).toFloat(),
//                                    paint
//                                )
//                                canvas.restore()
//                            }
//                        } else {
//                        }
//                    }
//                }
//            )
//            mTrackingOverlay.setOnTouchListener(OnTouchListener { view, event ->
//                val X = event.x.toInt()
//                val Y = event.y.toInt()
//                Log.d(TAG, ": " + Integer.toString(X) + " " + Integer.toString(Y))
//                when (event.action and MotionEvent.ACTION_MASK) {
//                    MotionEvent.ACTION_UP -> //                            Log.d(TAG, ": " +  "MotionEvent.ACTION_UP" );
//                        if (!mTargetLocked) {
//                            mDrawing = Drawing.CLEAR
//                            mTrackingOverlay.postInvalidate()
//                        }
//                    MotionEvent.ACTION_POINTER_DOWN -> {
//                        //                            Log.d(TAG, ": " +  "MotionEvent.ACTION_POINTER_DOWN" );
//                        if (mTargetLocked == false) {
//                            if (mPoints[0]!!.x - mPoints[1]!!.x != 0 && mPoints[0]!!.y - mPoints[1]!!.y != 0) {
//                                mTargetLocked = true
//                                val toast = Toast.makeText(
//                                    activity,
//                                    "Target is LOCKED !",
//                                    Toast.LENGTH_LONG
//                                )
//                                toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
//                                toast.show()
//                            } else {
//                                mTargetLocked = false
//                            }
//                        } else {
//                            mTargetLocked = false
//                            val toast =
//                                Toast.makeText(activity, "Target is UNLOCKED !", Toast.LENGTH_LONG)
//                            toast.setGravity(Gravity.TOP or Gravity.CENTER, 0, 0)
//                            toast.show()
//                        }
//                        mDrawing = Drawing.DRAWING
//                        mTrackingOverlay.postInvalidate()
//                    }
//                    MotionEvent.ACTION_POINTER_UP -> {
//                    }
//                    MotionEvent.ACTION_DOWN -> //                            Log.d(TAG, ": " +  "MotionEvent.ACTION_DOWN" );
//                        if (!mTargetLocked) {
//                            mDrawing = Drawing.DRAWING
//                            mPoints[0]!!.x = X
//                            mPoints[0]!!.y = Y
//                            mPoints[1]!!.x = X
//                            mPoints[1]!!.y = Y
//                            mTrackingOverlay.postInvalidate()
//                        }
//                    MotionEvent.ACTION_MOVE -> //                            Log.d(TAG, ": " +  "MotionEvent.ACTION_MOVE" );
//                        if (!mTargetLocked) {
//                            mPoints[1]!!.x = X
//                            mPoints[1]!!.y = Y
//                            mTrackingOverlay.postInvalidate()
//                        }
//                }
//                if (mTargetLocked == true) {
//                    mMenu!!.getItem(2).isEnabled = false
//                } else {
//                    mMenu!!.getItem(2).isEnabled = true
//                }
//                true
//            })
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun openCamera() {
//        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
//        Log.e(TAG, "is camera open")
//        try {
//            cameraId = manager.cameraIdList[0]
//            imageDimension = CamResolution
//            // Add permission for camera and let user grant the permission
//            if (ActivityCompat.checkSelfPermission(
//                    activity!!,
//                    Manifest.permission.CAMERA
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
////                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
//                val builder = AlertDialog.Builder(
//                    activity
//                )
//                builder.setTitle(R.string.camera_permission_title)
//                builder.setMessage(R.string.camera_permission_message)
//                builder.setPositiveButton(
//                    R.string.ok
//                ) { dialog: DialogInterface?, which: Int ->
//                    requestPermissions(
//                        arrayOf(Manifest.permission.CAMERA),
//                        REQUEST_CAMERA_PERMISSION
//                    )
//                }
//                builder.show()
//                return
//            }
//            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
//                throw RuntimeException("Time out waiting to lock camera opening.")
//            }
//            manager.openCamera(cameraId, stateCallback, mBackgroundHandler)
//        } catch (e: CameraAccessException) {
//            e.printStackTrace()
//        } catch (e: InterruptedException) {
//            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
//        }
//        Log.e(TAG, "openCamera 0")
//    }
//
//    private fun closeCamera() {
//        try {
//            cameraOpenCloseLock.acquire()
//            if (null != mCaptureSession) {
//                mCaptureSession!!.close()
//                mCaptureSession = null
//            }
//            if (null != cameraDevice) {
//                cameraDevice!!.close()
//                cameraDevice = null
//            }
//            if (null != imageReader) {
//                imageReader!!.close()
//                imageReader = null
//            }
//            cameraOpenCloseLock.release()
//        } catch (e: InterruptedException) {
//            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
//        } finally {
//            cameraOpenCloseLock.release()
//        }
//    }
//
//    /*
//     * SerialListener
//     */
//    /*
//     * Serial + UI
//     */
//    private fun status(str: String) {
//        mMenu!!.getItem(3).title = "BLE $str"
//    }
//
//    private fun connectBLE() {
//        try {
//            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//            val device = bluetoothAdapter.getRemoteDevice(mBluetoothDevAddr)
//            val deviceName = if (device.name != null) device.name else device.address
//            status("connecting...")
//            connected = Connected.Pending
//            socket = SerialSocket()
//            service.connect(this, "Connected to $deviceName")
//            socket.connect(context, service, device)
//        } catch (e: Exception) {
//            onSerialConnectError(e)
//        }
//    }
//
//    private fun disconnectBLE() {
//        connected = Connected.False
//        service.disconnect()
//        socket.disconnect()
//        socket = null
//    }
//
//    private fun sendBLE(str: String) {
//        if (connected != Connected.True) {
//            Toast.makeText(activity, "not connected", Toast.LENGTH_SHORT).show()
//            return
//        }
//        try {
//            val data = (str + newline).toByteArray()
//            socket.write(data)
//        } catch (e: Exception) {
//            onSerialIoError(e)
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        if (service != null) service.attach(this) else activity!!.startService(
//            Intent(
//                activity,
//                SerialService::class.java
//            )
//        ) // prevents service destroy on unbind from recreated activity caused by orientation change
//    }
//
//    override fun onStop() {
//        if (service != null && !activity!!.isChangingConfigurations) service.detach()
//        super.onStop()
//    }
//
//    override fun onAttach(activity: Activity) {
//        super.onAttach(activity)
//        getActivity()!!.bindService(
//            Intent(getActivity(), SerialService::class.java),
//            this,
//            Context.BIND_AUTO_CREATE
//        )
//    }
//
//    override fun onDetach() {
//        try {
//            activity!!.unbindService(this)
//        } catch (ignored: Exception) {
//        }
//        super.onDetach()
//    }
//
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        inflater.inflate(R.layout.menu_camera, menu)
//        mMenu = menu
//        mMenu!!.getItem(3).isEnabled = false
//        if (mBluetoothDevAddr == "" == false && service != null) {
//            activity!!.runOnUiThread { connectBLE() }
//        }
//    }
//
//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        return if (id == R.id.ble_setup) {
//            closeCamera()
//            if (connected != Connected.False) disconnectBLE()
//            val args = Bundle()
//            args.putString("device", mBluetoothDevAddr)
//            val fragment: Fragment = DevicesFragment()
//            fragment.arguments = args
//            fragment.setTargetFragment(this@CameraFragment, REQUEST_CONNECT_CODE)
//            fragmentManager!!.beginTransaction().replace(R.id.fragment, fragment, "devices")
//                .addToBackStack(null).commit()
//            true
//        } else if (id == R.id.camera_cordinate) {
//            val builder = AlertDialog.Builder(
//                activity
//            )
//            val showCooridinate = CheckBox(activity)
//            showCooridinate.text = "Show coordinate"
//            showCooridinate.isChecked = mShowCordinate
//            showCooridinate.setOnCheckedChangeListener { buttonView, isChecked ->
//                mShowCordinate = isChecked
//            }
//            val lay = LinearLayout(activity)
//            lay.setPadding(0, 30, 0, 0)
//            lay.gravity = Gravity.CENTER_HORIZONTAL
//            lay.addView(showCooridinate)
//            builder.setView(lay)
//
//            // Set up the buttons
//            builder.setPositiveButton(
//                "OK"
//            ) { dialog, which -> }
//            builder.setCancelable(false)
//            val dialog: Dialog = builder.show()
//            true
//        } else if (id == R.id.tracker_type) {
//            val builder = AlertDialog.Builder(
//                activity
//            )
//            builder.setTitle("Tracker Selection")
//            val radioBtnNames = arrayOf(
//                "TrackerMedianFlow",
//                "TrackerCSRT",
//                "TrackerKCF",
//                "TrackerMOSSE",
//                "TrackerTLD",
//                "TrackerMIL"
//            )
//            val rb = arrayOfNulls<RadioButton>(radioBtnNames.size)
//            val rg = RadioGroup(activity) //create the RadioGroup
//            rg.orientation = RadioGroup.VERTICAL
//            for (i in radioBtnNames.indices) {
//                rb[i] = RadioButton(activity)
//                rb[i]!!.text = " " + radioBtnNames[i]
//                rb[i]!!.id = i + 100
//                rg.addView(rb[i])
//                if (radioBtnNames[i] == mSelectedTracker) {
//                    rb[i]!!.isChecked = true
//                }
//            }
//
//            // This overrides the radiogroup onCheckListener
//            rg.setOnCheckedChangeListener { group, checkedId ->
//                // This will get the radiobutton that has changed in its check state
//                val checkedRadioButton =
//                    group.findViewById<View>(checkedId) as RadioButton
//                // This puts the value (true/false) into the variable
//                val isChecked = checkedRadioButton.isChecked
//                if (isChecked) {
//                    // Changes the textview's text to "Checked: example radiobutton text"
//                    mSelectedTracker = checkedRadioButton.text.toString().replace(" ", "")
//                }
//            }
//            val lay = LinearLayout(activity)
//            lay.setPadding(0, 30, 0, 0)
//            lay.gravity = Gravity.CENTER_HORIZONTAL
//            lay.addView(rg)
//            builder.setView(lay)
//
//            // Set up the buttons
//            builder.setPositiveButton(
//                "OK"
//            ) { dialog, which -> }
//            builder.setCancelable(false)
//            val dialog: Dialog = builder.show()
//            true
//        } else {
//            super.onOptionsItemSelected(item)
//        }
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_CONNECT_CODE && resultCode == Activity.RESULT_OK) {
//            mBluetoothDevAddr = data!!.getStringExtra("bluetooth device")
//        }
//    }
//}