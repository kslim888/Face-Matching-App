package com.kslimweb.testfacematching.camera

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import com.kslimweb.testfacematching.graphics.RectOverlay

class FaceDetection : ImageAnalysis.Analyzer {

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

            val imageRotation = degreesToFirebaseRotation(rotationDegrees)

            val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)

            val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.35f)
//                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .enableTracking()
                .build()

            val detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

            detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { faces ->
                    // Task completed successfully

                    for (face in faces) {
                        Log.d(TAG, "face detected")
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
                            Log.d(TAG, id.toString())
                        }
                    }
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    Log.d(TAG, it.message)
                }
        }
    }


}