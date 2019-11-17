package com.kslimweb.testfacematching.api

import com.kslimweb.testfacematching.models.FaceMatchingData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FaceMatchingAPI {

    @Multipart
    @POST("api/upload")
    suspend fun postData(
        @Part original: MultipartBody.Part,
        @Part unknown: MultipartBody.Part,
        @Part("tolerance") tolerance: RequestBody,
        @Part("threshold") threshold: RequestBody
    ): FaceMatchingData
}