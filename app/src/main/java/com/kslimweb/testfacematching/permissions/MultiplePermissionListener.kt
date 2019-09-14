package com.kslimweb.testfacematching.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.afollestad.materialdialogs.MaterialDialog
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

class MultiplePermissionListener(
    private val activity: Activity,
    private val intent: Intent,
    private val flag: Int
) : MultiplePermissionsListener {

    private val TAG = MultiplePermissionListener::class.java.simpleName

    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

        if (report != null) {
            if (report.areAllPermissionsGranted()) {

                intent.putExtra("FLAG", flag)
                activity.startActivity(intent)
//                activity.startActivityForResult(intent, code)
                return
            }

            // check for permanent denial of any permission
            if (report.isAnyPermissionPermanentlyDenied) {

                // permission is denied permanently, navigate user to app settings
                MaterialDialog(activity).show {
                    Log.d(TAG, "isAnyPermissionPermanentlyDenied")
                    message(text = "Please allow all permissions in App settings")
                    negativeButton()
                    positiveButton(text = "Open Settings") {
                        Log.d(TAG, "Continue to Settings")
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
        token?.continuePermissionRequest()
    }
}