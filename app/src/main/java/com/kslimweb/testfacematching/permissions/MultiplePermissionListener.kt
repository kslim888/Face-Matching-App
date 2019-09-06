package com.kslimweb.testfacematching.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MultiplePermissionListener(private val context: Context) : MultiplePermissionsListener {

    private val TAG = MultiplePermissionListener::class.java.simpleName
    companion object {
        var areAllPermissionsGranted = false
        var settingsFlag = false
    }

    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

        if (report != null) {
            if (report.areAllPermissionsGranted()) {
                areAllPermissionsGranted = true
                return
            }

            // check for permanent denial of any permission
            if (report.isAnyPermissionPermanentlyDenied && !settingsFlag) {

                // permission is denied permenantly, navigate user to app settings
                MaterialDialog(context).show {
                    Log.d(TAG, "isAnyPermissionPermanentlyDenied")
                    message(text = "Permissions are required in order to work properly")
                    negativeButton()
                    positiveButton(text = "Open Settings") {
                        Log.d(TAG, "Continue to Settings")
                        settingsFlag = true
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:com.kslimweb.testfacematching")
                            )
                        )
                    }
                    cancelOnTouchOutside(false)
                }
            }
        }
    }

    override fun onPermissionRationaleShouldBeShown(
        permissions: MutableList<PermissionRequest>?,
        token: PermissionToken?
    ) {
        if (token != null) {
            MaterialDialog(context).show {
                message(text = "Permissions are required in order to work properly")
                negativeButton { token.cancelPermissionRequest() }
                positiveButton(text = "Continue Allow") {
                    Log.d(TAG, "Continue Allow")
                    token.continuePermissionRequest()
//                    checkAndRequestPermissions()
                }
                cancelOnTouchOutside(false)
            }
        }
    }
}