package com.kslimweb.testfacematching.api

import com.kslimweb.testfacematching.utils.VersionUtil
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object FaceMatchingRetrofitBuilder {
    // To debug http://192.168.0.161:8080
    // and add android:usesCleartextTraffic="true" in manifest application tag
    private val BASE_URL = if (!VersionUtil.isReleaseMode) {
        "https://matching-face.herokuapp.com"
    } else {
        "https://cardzone-face-matching.herokuapp.com"
    }

    // Create a Custom Interceptor to apply Headers application wide
    private val headerInterceptor = Interceptor { chain ->
        var request = chain.request()
        request = request.newBuilder()
            .addHeader("connection", "keep-alive")
            .build()
        val response = chain.proceed(request)
        response
    }

    // add timeout 120 seconds for API call
    // header keep-connection alive can't solve timeout issues
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(120, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofitBuilder: Retrofit.Builder by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
    }

    val apiService: FaceMatchingAPI by lazy {
        retrofitBuilder.build().create(FaceMatchingAPI::class.java)
    }
}