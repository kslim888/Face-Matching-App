package com.kslimweb.testfacematching.camera

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaActionSound
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner
import com.kslimweb.testfacematching.MainActivity
import com.kslimweb.testfacematching.MainActivity.Companion.TAKE_PICTURE_FLAG
import com.kslimweb.testfacematching.MainActivity.Companion.TAKE_VIDEO_FLAG
import com.kslimweb.testfacematching.R
import com.kslimweb.testfacematching.camera.facedetection.FaceDetection
import com.kslimweb.testfacematching.utils.AutoFitPreviewBuilder
import com.kslimweb.testfacematching.utils.Timer
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class CameraXActivity : AppCompatActivity(), LifecycleOwner {

    private var lensFacing = CameraX.LensFacing.FRONT
    private var flag: Int? = null
    private val mSound = MediaActionSound()

    companion object {
        private val TAG = CameraXActivity::class.java.simpleName
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val VIDEO_EXTENSION = ".mp4"

        var cameraImageFile: File? = null
        var cameraVideoFile: File? = null

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)

        if (intent.extras != null) {
            flag = intent?.extras?.get("FLAG") as Int
            Log.i(TAG, "Camera Flag: " + flag.toString())
        }

        view_finder.post {
            startCamera()
        }
    }

    private fun setupFile(flag: Int): File? {
        val outputDirectory = getOutputDirectory(this)
        var captureFile: File? = null

        if (flag == TAKE_PICTURE_FLAG) {
            captureFile = createFile(
                outputDirectory,
                FILENAME,
                PHOTO_EXTENSION
            )
        } else if (flag == TAKE_VIDEO_FLAG) {
            captureFile = createFile(
                outputDirectory,
                FILENAME,
                VIDEO_EXTENSION
            )
        }
        return captureFile
    }

    private fun startCamera() {
        val preview = setupPreview()

        // Setup image analysis pipeline that computes average pixel luminance in real time
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            // Use a worker thread for image analysis to prevent preview glitches
            val analyzerThread = HandlerThread(
                "FaceDetection").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            // In our analysis, we care more about the latest image than analyzing *every* image
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(view_finder.display.rotation)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = FaceDetection(this@CameraXActivity)
        }

        // Bind use cases to lifecycle
        // If Android Studio complains about "this" being not a LifecycleOwner
        // try rebuilding the project or updating the appcompat dependency to
        // version 1.1.0 or higher.
        if (flag == TAKE_PICTURE_FLAG) {
            capture_button.setBackgroundResource(R.drawable.ic_shutter_image)
            timer.visibility = View.GONE
            val cameraUseCase = setupImageCapture()
            CameraX.bindToLifecycle(this, preview, cameraUseCase, analyzerUseCase)
        } else if (flag == TAKE_VIDEO_FLAG) {
            capture_button.setBackgroundResource( R.drawable.ic_shutter_video)
            val cameraUseCase = setupVideoCapture()
            CameraX.bindToLifecycle(this, preview, cameraUseCase)

            // uncomment below once videocapture API in CameraX is released
//            CameraX.bindToLifecycle(this, preview, cameraUseCase, analyzerUseCase)
        }
    }

    private fun setupPreview(): Preview {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            // We request aspect ratio but no resolution to let CameraX optimize our use cases
            setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            setTargetRotation(view_finder.display.rotation)
        }.build()

        // Use the auto-fit preview builder to automatically handle size and orientation changes
        val preview = AutoFitPreviewBuilder.build(previewConfig, view_finder)

        return preview
    }

    @SuppressLint("RestrictedApi")
    private fun setupVideoCapture(): VideoCapture {
        val rotation = view_finder.display.rotation
        val videoCaptureConfig = VideoCaptureConfig.Builder()
            .apply {
                setLensFacing(lensFacing)
                setTargetRotation(rotation)
        }.build()

        val videoCapture = VideoCapture(videoCaptureConfig)

        capture_button.setOnTouchListener { _, event ->

            if (event.action == MotionEvent.ACTION_DOWN) {
                mSound.play(MediaActionSound.START_VIDEO_RECORDING)
                val videoFile = setupFile(flag!!)

                // delay 1 second and start record
                capture_button.postDelayed({

                    videoCapture.startRecording(videoFile, videoSavedListener)
                    val timer = Timer(6000, 1000, this, videoCapture)
                    timer.start()
                }, 500)

            } else if (event.action == MotionEvent.ACTION_UP) {
                mSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                videoCapture.stopRecording()
                Log.i(TAG, "Recording stopped")
            }
            false
        }
        return videoCapture
    }

    private fun setupImageCapture(): ImageCapture {

        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                // We don't set a resolution for image capture; instead, we
                // select a capture mode which will infer the appropriate
                // resolution based on aspect ration and requested mode
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                setLensFacing(lensFacing)
                setTargetRotation(view_finder.display.rotation)
                // We request aspect ratio but no resolution to let CameraX optimize our use cases
//                setTargetAspectRatio(screenAspectRatio)
            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)

        capture_button.setOnClickListener {
            mSound.play(MediaActionSound.SHUTTER_CLICK)
            val photoFile = setupFile(flag!!)

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {
                // Mirror image when using the front camera
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }

            imageCapture.takePicture(photoFile, imageSavedListener, metadata)

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
                root_constraint_view.postDelayed({
                    root_constraint_view.foreground = ColorDrawable(Color.WHITE)
                    root_constraint_view.postDelayed(
                        { root_constraint_view.foreground = null }, 50L
                    )
                }, 100L)
            }
        }
        return imageCapture
    }

    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onImageSaved(file: File) {
            val msg = "Photo capture succeeded: ${file.absolutePath}"
            cameraImageFile = file
            Log.i(TAG, msg)
            startActivity(Intent(this@CameraXActivity, MainActivity::class.java))
        }

        override fun onError(
            imageCaptureError: ImageCapture.ImageCaptureError,
            message: String,
            cause: Throwable?
        ) {
            Log.e(TAG, message)
        }
    }

    private val videoSavedListener = object : VideoCapture.OnVideoSavedListener {
        override fun onVideoSaved(file: File) {
            val msg = "Video record succeeded: ${file.absolutePath}"
            cameraVideoFile = file
            Log.i(TAG, msg)
            startActivity(Intent(this@CameraXActivity, MainActivity::class.java))
        }

        override fun onError(
            videoCaptureError: VideoCapture.VideoCaptureError,
            message: String,
            cause: Throwable?
        ) {
            Log.e(TAG, message)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CameraX.unbind()
    }
}
