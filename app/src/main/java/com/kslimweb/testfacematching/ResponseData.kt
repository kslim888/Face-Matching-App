package com.kslimweb.testfacematching

import com.google.gson.annotations.SerializedName

data class ResponseData(
    @SerializedName("confidence")
    val confidence: Double,

    @SerializedName("face_found_in_image")
    val faceFoundInImage: Boolean,

    @SerializedName("face_found_in_video")
    val faceFoundInVideo: Boolean,

    @SerializedName("is_match")
    val isMatch: Boolean,

    @SerializedName("status_code")
    val statusCode: Int
)