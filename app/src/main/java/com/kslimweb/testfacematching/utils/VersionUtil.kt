package com.kslimweb.testfacematching.utils

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.text.TextUtils
import com.kslimweb.testfacematching.BuildConfig

object VersionUtil {

    // check if app is in debug or release mode
    val isReleaseMode: Boolean
        get() = !BuildConfig.DEBUG

    /**
     * This is for user visible version string. This is for usage in UI only
     *
     * @param context
     * @return
     */
    fun getVersionForDisplay(context: Context?): String {
        val builder = StringBuilder()

        if (context == null) {
            return builder.toString()
        }
        try {
            val versionCode = VersionUtil.getVersionCode(context)
            val versionName = VersionUtil.getVersionName(context)
            if (!TextUtils.isEmpty(versionName)) {
                builder.append("version ")
                builder.append(versionName)
                builder.append("-")
                builder.append(versionCode)
            }
        } catch (e: NameNotFoundException) { }
        return builder.toString()
    }

    /**
     * This method retrieves version code of the package.
     *
     * @param context Application context
     * @return int
     * @throws NameNotFoundException Name not found exception
     */
    @Throws(NameNotFoundException::class)
    fun getVersionCode(context: Context): Int {
        val manager = context.packageManager.getPackageInfo(context.packageName, 0)
        return manager.versionCode
    }

    /**
     * This method retrieves version partnerName of the package.
     *
     * @param context Application context
     * @return String
     * @throws NameNotFoundException Name not found exception
     */
    @Throws(NameNotFoundException::class)
    fun getVersionName(context: Context): String {
        val manager = context.packageManager.getPackageInfo(context.packageName, 0)
        return manager.versionName
    }
}
