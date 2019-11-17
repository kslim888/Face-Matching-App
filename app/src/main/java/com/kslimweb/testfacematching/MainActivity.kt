package com.kslimweb.testfacematching

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.appyvet.materialrangebar.RangeBar
import com.google.gson.GsonBuilder
import com.kslimweb.testfacematching.camera.CameraXActivity
import com.kslimweb.testfacematching.camera.CameraXActivity.Companion.cameraImageFile
import com.kslimweb.testfacematching.camera.CameraXActivity.Companion.cameraVideoFile
import com.kslimweb.testfacematching.api.FaceMatchingRetrofitBuilder
import com.kslimweb.testfacematching.models.FaceMatchingData
import com.kslimweb.testfacematching.models.RequestData
import com.kslimweb.testfacematching.permissions.PermissionsUtil
import com.kslimweb.testfacematching.utils.ImageUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName
    private var imageFile: File? = null
    private var videoFile: File? = null
    private lateinit var threshold: String
    private lateinit var tolerance: String

    companion object {
        const val TAKE_PICTURE_FLAG = 1
        const val TAKE_VIDEO_FLAG = 2
        private const val IMAGE_FORM_KEY = "known"
        private const val VIDEO_FORM_KEY = "unknown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PermissionsUtil.setContext(this)

        if (cameraImageFile != null) {
            val orientedBitmap = ImageUtils.decodeBitmap(cameraImageFile!!)
            imageView.setImageBitmap(orientedBitmap)
            imageFile = cameraImageFile
        }

        if (cameraVideoFile != null) {
            // start video media
            val mediaController = MediaController(this)
            videoView.setVideoURI(Uri.fromFile(cameraVideoFile))
            videoView.setMediaController(mediaController)
            videoView.start()

            videoFile = cameraVideoFile
        }

        threshold_range_bar.setRangePinsByValue(0.8F, 0.8F)
        threshold_range_bar.setOnRangeBarChangeListener(object: RangeBar.OnRangeBarChangeListener {
            override fun onTouchEnded(rangeBar: RangeBar?) { }
            override fun onRangeChangeListener(
                rangeBar: RangeBar?,
                leftPinIndex: Int,
                rightPinIndex: Int,
                leftPinValue: String?,
                rightPinValue: String?
            ) {
                Log.d(TAG, "Current Threshold Value: $rightPinValue")
                threshold = rightPinValue!!
            }
            override fun onTouchStarted(rangeBar: RangeBar?) { }
        })

        tolerance_range_bar.setRangePinsByValue(0.5F, 0.5F)
        tolerance_range_bar.setOnRangeBarChangeListener(object: RangeBar.OnRangeBarChangeListener {
            override fun onTouchEnded(rangeBar: RangeBar?) { }
            override fun onRangeChangeListener(
                rangeBar: RangeBar?,
                leftPinIndex: Int,
                rightPinIndex: Int,
                leftPinValue: String?,
                rightPinValue: String?
            ) {
                Log.d(TAG, "Current Tolerance Value: $rightPinValue")
                tolerance = rightPinValue!!
            }
            override fun onTouchStarted(rangeBar: RangeBar?) { }
        })

        take_photo.setOnClickListener{
            val takePicture = Intent(this, CameraXActivity::class.java)
            PermissionsUtil.checkAndRequestPermissions(takePicture, TAKE_PICTURE_FLAG)
        }

        take_video.setOnClickListener{
            val takeVideo = Intent(this, CameraXActivity::class.java)
            PermissionsUtil.checkAndRequestPermissions(takeVideo, TAKE_VIDEO_FLAG)
        }

        submit.setOnClickListener{
            whenAllNotNull(imageFile, videoFile) {

                layout_progress.visibility = View.VISIBLE

                // setting up forms data
                val (imagePart,
                    videoPart,
                    thresholdFormData,
                    toleranceFormData) = setUpFormData()

                CoroutineScope(IO).launch {
                    performFaceMatchingRequest(imagePart, videoPart, thresholdFormData, toleranceFormData)
                }
            }
        }
    }

    private fun setUpFormData(): RequestData {
        val imageRequest = RequestBody.create(MediaType.parse("multipart/form-data"), imageFile!!)
        val imagePart = MultipartBody.Part.createFormData(IMAGE_FORM_KEY, imageFile!!.name, imageRequest)
        val videoRequest = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile!!)
        val videoPart = MultipartBody.Part.createFormData(VIDEO_FORM_KEY, videoFile!!.name, videoRequest)
        val thresholdFormData = RequestBody.create(MediaType.parse("multipart/form-data"), threshold)
        val toleranceFormData = RequestBody.create(MediaType.parse("multipart/form-data"), tolerance)

        return RequestData(imagePart, videoPart, thresholdFormData, toleranceFormData)
    }

    private suspend fun performFaceMatchingRequest(
        imagePart: MultipartBody.Part,
        videoPart: MultipartBody.Part,
        thresholdFormData: RequestBody,
        toleranceFormData: RequestBody
    ) {
        val faceMatchingData = FaceMatchingRetrofitBuilder
            .apiService
            .postData(imagePart, videoPart, thresholdFormData, toleranceFormData)

        setUI(faceMatchingData)
    }

    private suspend fun setUI(faceMatchingData: FaceMatchingData) {
        withContext(Main) {
            result_text_view.text = GsonBuilder().setPrettyPrinting().create().toJson(faceMatchingData)
            showResultDialog(faceMatchingData.isMatch)
            layout_progress.visibility = View.GONE
            onEnterAnimationComplete()
        }
    }

    private fun showResultDialog(isMatch: Boolean) {
        MaterialDialog(this).show {
            if (isMatch) {
                title(text = "Your face is matched")
                icon(R.drawable.ic_check_green_24dp)
            } else {
                title(text = "Your face is not match")
                icon(R.drawable.ic_cross_red_24dp)
            }
        }
    }

    // https://stackoverflow.com/questions/35513636/multiple-variable-let-in-kotlin
    private fun <T: Any, R: Any> whenAllNotNull(vararg options: T?, block: (List<T>)->R) {
        if (options.all { it != null }) {
            block(options.filterNotNull()) // or do unsafe cast to non null collection
        } else {
            Toast.makeText(this,"Please take photo and a short ( < 5 seconds) video first", Toast.LENGTH_LONG).show()
        }
    }
}
