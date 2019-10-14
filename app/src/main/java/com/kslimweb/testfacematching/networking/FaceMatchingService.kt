package com.kslimweb.testfacematching.networking

import com.kslimweb.testfacematching.models.ResponseData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FaceMatchingService {

    @Multipart
    @POST("api/upload")
    fun postData(
        @Part original: MultipartBody.Part,
        @Part unknown: MultipartBody.Part,
        @Part("tolerance") tolerance: RequestBody,
        @Part("threshold") threshold: RequestBody
    ): Call<ResponseData>
}