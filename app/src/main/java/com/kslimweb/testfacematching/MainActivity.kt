package com.kslimweb.testfacematching

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.appyvet.materialrangebar.RangeBar
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.kslimweb.testfacematching.models.ResponseData
import com.kslimweb.testfacematching.networking.FaceMatchingService
import com.kslimweb.testfacematching.networking.RetrofitClientBuilder
import com.kslimweb.testfacematching.permissions.MultiplePermissionListener.Companion.settingsFlag
import com.kslimweb.testfacematching.permissions.PermissionsUtil
import com.kslimweb.testfacematching.utils.PathUtil
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private var imageFile: File? = null
    private var videoFile: File? = null
    private var threshold: String? = null
    private var onResumeCalledAlready: Boolean = false

    companion object {
        const val TAKE_PICTURE_CODE = 200
        const val TAKE_VIDEO_CODE = 202
        const val IMAGE_FORM_KEY = "original"
        const val VIDEO_FORM_KEY = "unknown"
        const val ON_RESUME_CALLED_PREFERENCE_KEY = "onResumeCalled"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save current onResumeCalledAlready state
        outState.putBoolean(ON_RESUME_CALLED_PREFERENCE_KEY, onResumeCalledAlready)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (settingsFlag && !PermissionsUtil.areAllPermissionsGranted() && !onResumeCalledAlready) {
            onResumeCalledAlready = true
            MaterialDialog(this).show {
                message(text = "Permissions are not granted. Please allow permissions in App Settings")
                positiveButton()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionsUtil.setContext(this)
        PermissionsUtil.checkAndRequestPermissions()

        onResumeCalledAlready = // Restore value of members from saved state
            savedInstanceState?.getBoolean(ON_RESUME_CALLED_PREFERENCE_KEY) ?: false

        threshold_range_bar.setRangePinsByValue(0.6F, 0.6F)
        threshold_range_bar.setOnRangeBarChangeListener(object: RangeBar.OnRangeBarChangeListener {
            override fun onTouchEnded(rangeBar: RangeBar?) {

            }

            override fun onRangeChangeListener(
                rangeBar: RangeBar?,
                leftPinIndex: Int,
                rightPinIndex: Int,
                leftPinValue: String?,
                rightPinValue: String?
            ) {
                Log.d(TAG, "Current Threshold Value: $rightPinValue")
                threshold = rightPinValue
            }

            override fun onTouchStarted(rangeBar: RangeBar?) {

            }
        })

        take_photo.setOnClickListener{
            if(PermissionsUtil.areAllPermissionsGranted()) {
                val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(takePicture, TAKE_PICTURE_CODE)
            }
        }

        take_video.setOnClickListener{
            if(PermissionsUtil.areAllPermissionsGranted()) {
                Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                    takeVideoIntent.resolveActivity(packageManager)?.also {
                        startActivityForResult(takeVideoIntent, TAKE_VIDEO_CODE)
                    }
                }
            }
        }

        submit.setOnClickListener{

            whenAllNotNull(imageFile, videoFile) {
                layout_progress.visibility = View.VISIBLE
                val retrofit = RetrofitClientBuilder.retrofitInstance?.create(FaceMatchingService::class.java)

                val imageRequest = RequestBody.create(MediaType.parse("multipart/form-data"), imageFile!!)
                val imagePart = MultipartBody.Part.createFormData(IMAGE_FORM_KEY, imageFile!!.name, imageRequest)

                val videoRequest = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile!!)
                val videoPart = MultipartBody.Part.createFormData(VIDEO_FORM_KEY, videoFile!!.name, videoRequest)

                val thresholdFormData = RequestBody.create(MediaType.parse("multipart/form-data"), threshold!!)

                retrofit?.postData(imagePart, videoPart, thresholdFormData)?.enqueue(object:
                    Callback<ResponseData> {
                    override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                        Log.d(TAG, "Failed: " + t.message)
                        layout_progress.visibility = View.GONE
                    }

                    override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                        layout_progress.visibility = View.GONE
                        val result = response.body()
                        Log.d(TAG, "Response: " + Gson().toJson(result))
                        result_text_view.text = GsonBuilder().setPrettyPrinting().create().toJson(result)

                        showResultDialog(result!!.isMatch)
                    }
                })!!
            }
        }
    }

    private fun showResultDialog(match: Boolean) {
        MaterialDialog(this).show {
            if (match) {
                title(text = "Your face is matched")
                icon(R.drawable.ic_check_green_24dp)
            } else {
                title(text = "Your face is not match")
                icon(R.drawable.ic_cross_red_24dp)
            }
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: Exception) {
            Log.e(TAG, "getRealPathFromURI Exception : $e")
            ""
        } finally {
            cursor?.close()
        }
    }

    // https://stackoverflow.com/questions/35513636/multiple-variable-let-in-kotlin
    private fun <T: Any, R: Any> whenAllNotNull(vararg options: T?, block: (List<T>)->R) {
        if (options.all { it != null }) {
            block(options.filterNotNull()) // or do unsafe cast to non null collection
        } else {
            Toast.makeText(this,"Take photo and a short ( < 5 seconds) video first", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == TAKE_PICTURE_CODE && resultCode == RESULT_OK) {
            val imageBitmap = intent!!.extras!!.get("data") as Bitmap

            val imageUri = PathUtil.getImageUri(this, imageBitmap)
            val imagePath = getRealPathFromURI(this, imageUri)
            Log.d(TAG, "Image path: $imagePath")

            imageFile = File(imagePath)
            imageView.setImageBitmap(imageBitmap)

            // [START compressor]
            // Image file can be compress if is too big
            // Put Gradle implementation 'id.zelory:compressor:2.1.0'
//            val actualImage = FileUtil.from(this, imageUri)
//            Log.d(TAG, String.format("Size : %s", actualImage!!.length()))
//
//            val compressedImageFile = Compressor(this)
//                .setQuality(50)
//                .setDestinationDirectoryPath(Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_PICTURES).absolutePath + "/Screenshots"
//                )
//                .setCompressFormat(Bitmap.CompressFormat.WEBP)
//                .compressToFile(actualImage)
//            Log.d(TAG, String.format("Size : %s", compressedImageFile!!.length()))
        }

        if (requestCode == TAKE_VIDEO_CODE && resultCode == RESULT_OK) {
            val videoUri = intent!!.data
            Log.d(TAG, "videoUri: $videoUri")

            if (videoUri != null) {
                val videoPath = getRealPathFromURI(this, videoUri)
                videoFile = File(videoPath)
                Log.d(TAG, "Video path: $videoPath")

                // start video media
                val mediaController = MediaController(this)
                videoView.setVideoURI(videoUri)
                videoView.setMediaController(mediaController)
                videoView.start()
            }
        }
    }

    private fun faceDetection(imageUri: Uri) {
        // options
        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()

        // face detection start here
        val image = FirebaseVisionImage.fromFilePath(this, imageUri)
        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)
        detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                Log.d(TAG, "face detected")
                for (face in faces) {
                    val bounds = face.boundingBox
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                    }

                    // If contour detection was enabled:
                    val leftEyeContour = face.getContour(FirebaseVisionFaceContour.LEFT_EYE).points
                    val upperLipBottomContour = face.getContour(FirebaseVisionFaceContour.UPPER_LIP_BOTTOM).points

                    // If classification was enabled:
                    if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val smileProb = face.smilingProbability
                    }
                    if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                    }

                    // If face tracking was enabled:
                    if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                        val id = face.trackingId
                    }
                }
            }
            .addOnFailureListener {
                // Task failed with an exception
                // ...
                Log.d(TAG, it.message)
            }
    }
}
