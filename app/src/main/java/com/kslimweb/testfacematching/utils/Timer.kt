package com.kslimweb.testfacematching.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.media.MediaActionSound
import android.os.CountDownTimer
import android.util.Log
import androidx.camera.core.VideoCapture
import kotlinx.android.synthetic.main.activity_camera_x.*
import com.kslimweb.testfacematching.R


open class Timer(
    private val millisInFuture: Long,
    private val countDownInterval: Long,
    private val activity: Activity,
    private val videoCapture: VideoCapture)
    : CountDownTimer(millisInFuture, countDownInterval) {

    @SuppressLint("RestrictedApi")
    override fun onFinish() {
        activity.capture_button.setBackgroundResource(R.drawable.ic_shutter_video)
        MediaActionSound().play(MediaActionSound.STOP_VIDEO_RECORDING)
        videoCapture.stopRecording()
    }

    override fun onTick(millisUntilFinished: Long) {
        val second = ((millisInFuture - millisUntilFinished) / countDownInterval).toInt()
        val formattedTimer = activity.resources.getString(R.string.timer_message, second)
        activity.timer.text = formattedTimer
        Log.d("CountUp", second.toString())
    }
}