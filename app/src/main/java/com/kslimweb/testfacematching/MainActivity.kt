package com.kslimweb.testfacematching

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.appyvet.materialrangebar.RangeBar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
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

    private val imageFormKey = "original"
    private val videoFormKey = "unknown"
    private val TAKE_PICTURE_CODE = 200
    private val TAKE_VIDEO_CODE = 202
    private val permissionCheck = PermissionCheckAndRequest(this, this)

    companion object {
        const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

        // check permission
        if (permissionCheck.checkAndRequestPermissions()) {
            // carry on the normal flow, as the case of  permissions  granted.
            Handler().postDelayed({
                // This method will be executed once the timer is over
                // Start your app main activity
            }, 2000.toLong())
        }

        take_photo.setOnClickListener{
            val takePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(takePicture, TAKE_PICTURE_CODE)
        }

        take_video.setOnClickListener{
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
                takeVideoIntent.resolveActivity(packageManager)?.also {
                    startActivityForResult(takeVideoIntent, TAKE_VIDEO_CODE)
                }
            }
        }

        submit.setOnClickListener{
            whenAllNotNull(imageFile, videoFile) {
                layout_progress.visibility = View.VISIBLE
                val retrofit = RetrofitClientBuilder.retrofitInstance?.create(FaceMatchingService::class.java)

                val imageRequest = RequestBody.create(MediaType.parse("multipart/form-data"), imageFile!!)
                val imagePart = MultipartBody.Part.createFormData(imageFormKey, imageFile!!.name, imageRequest)

                val videoRequest = RequestBody.create(MediaType.parse("multipart/form-data"), videoFile!!)
                val videoPart = MultipartBody.Part.createFormData(videoFormKey, videoFile!!.name, videoRequest)

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

                        showMaterialDialog(result!!.isMatch)
                    }
                })!!
            }
        }
    }

    private fun showMaterialDialog(match: Boolean) {
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

    // https://stackoverflow.com/questions/35513636/multiple-variable-let-in-kotlin
    private fun <T: Any, R: Any> whenAllNotNull(vararg options: T?, block: (List<T>)->R) {
        if (options.all { it != null }) {
            block(options.filterNotNull()) // or do unsafe cast to non null collection
        } else {
            Toast.makeText(this,"Take photo and a short ( < 5 seconds) video first", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        Log.d("Main", "Permission callback called")

        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {

                val perms = HashMap<String, Int>()
                // Initialize the map with both permissions
                perms[Manifest.permission.CAMERA] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.READ_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED

                // Fill with actual results from user
                if (grantResults.isNotEmpty()) {

                    for (i in permissions.indices) {
                        perms[permissions[i]] = grantResults[i]
                    }

                    // Check for both permissions
                    if (perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.READ_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                        && perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "granted permission")

                        // process the normal flow
//                        val i = Intent(this@MainActivity, WelcomeActivity::class.java)
//                        startActivity(i)
//                        finish()
//                        else any one or both the permissions are not granted
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ")

                        // permission is denied (this is the first time, when "never ask again" is not checked) so ask again explaining the usage of permission
                        // shouldShowRequestPermissionRationale will return true
                        // show the dialog or snackbar saying its necessary and try again otherwise proceed with setup.
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            permissionCheck.showDialogOK("Permissions are required for this app",
                                DialogInterface.OnClickListener { dialog, which ->
                                    when (which) {
                                        DialogInterface.BUTTON_POSITIVE -> permissionCheck.checkAndRequestPermissions()

                                        DialogInterface.BUTTON_NEGATIVE -> {
                                            // proceed with logic by disabling the related features or quit the app.
//                                            finish()
                                            permissionCheck.explain("You need to give some mandatory permissions to continue. Do you want to go to app settings?")
                                        }

                                    }
                                })
                        } else {
                            //proceed with logic by disabling the related features or quit the app.
                            //permission is denied (and never ask again is checked)
                            //shouldShowRequestPermissionRationale will return false
                            permissionCheck.explain("You need to give some mandatory permissions to continue. Do you want to go to app settings?")
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (requestCode == TAKE_PICTURE_CODE && resultCode == RESULT_OK) {
            val imageBitmap = intent!!.extras!!.get("data") as Bitmap

            val imageUri = PathUtil.getImageUri(this, imageBitmap)
            val imagePath = PathUtil.getPath(this, imageUri)
            Log.d(TAG, "Image: $imagePath")

            imageFile = File(imagePath)
            imageView.setImageBitmap(imageBitmap)
        }

        if (requestCode == TAKE_VIDEO_CODE && resultCode == RESULT_OK) {
            val videoUri = intent!!.data
            val videoPath = PathUtil.getPath(this, videoUri!!)
            Log.d(TAG, "Video: $videoPath")

            videoFile = File(videoPath)
            videoView.setVideoURI(videoUri)

            // start video media
            val mediaController = MediaController(this)
            videoView.setMediaController(mediaController)
            videoView.start()
        }
    }
}
