package com.kslimweb.testfacematching.camera.facedetection

import android.app.Activity
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_camera_x.*


class FaceDetection(private val activity: Activity) : ImageAnalysis.Analyzer {

    private val TAG = FaceDetection::class.java.simpleName

    private fun degreesToFirebaseRotation(degrees: Int): Int = when(degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270")
    }

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        val mediaImage = image?.image

        if (mediaImage != null) {

            val width = mediaImage.width
            val height = mediaImage.height

            val imageRotation = degreesToFirebaseRotation(rotationDegrees)

            val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)

            val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
//                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.70f)
//                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .enableTracking()
                .build()

            val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

            detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { faces ->
                    // Task completed successfully
                    processFacesResult(faces, width, height)
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    Log.d(TAG, it.message)
                }
        }
    }

    private fun processFacesResult(faces: List<FirebaseVisionFace>,
                                   width: Int,
                                   height: Int) {
        // create original bitmap for bounding box
        val bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        var flippedBitmap: Bitmap? = null

        if (faces.isEmpty()) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            val facePaint = Paint()
            facePaint.color = Color.GREEN
            facePaint.style = Paint.Style.STROKE
            facePaint.strokeWidth = 2F

            // Get information for face detected
            // https://firebase.google.com/docs/ml-kit/android/detect-faces#3.-get-information-about-detected-faces
            // https://developers.google.com/android/reference/com/google/firebase/ml/vision/face/package-summary
            for (face in faces) {
                canvas.drawRect(face.boundingBox, facePaint)

                // create flipped bounding box bitmap
                val matrix = Matrix()
                matrix.preScale(-1F, 1F)
                flippedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        }
        activity.face_detection_camera_image_view.setImageBitmap(flippedBitmap)
        activity.face_detection_camera_image_view.invalidate()
    }
}