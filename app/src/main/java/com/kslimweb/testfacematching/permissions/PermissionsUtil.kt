package com.kslimweb.testfacematching.permissions

import android.Manifest
import android.content.Intent
import android.util.Log
import com.karumi.dexter.Dexter
import com.kslimweb.testfacematching.MainActivity

class PermissionsUtil {

    companion object {
        private val TAG: String = PermissionsUtil::class.java.simpleName
        private lateinit var activity: MainActivity

        fun setContext(activity: MainActivity) {
            Companion.activity = activity
        }

        fun checkAndRequestPermissions(intent: Intent, flag: Int) {
            val multiplePermissionListener =
                MultiplePermissionListener(activity, intent, flag)
            Dexter.withActivity(activity)
                .withPermissions(Manifest.permission.CAMERA,
                    Manifest.permission.INTERNET,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO)
                .withListener(multiplePermissionListener)
                .withErrorListener { error -> Log.e(TAG, "There was an error: $error") }
                .check()
        }
    }
}
