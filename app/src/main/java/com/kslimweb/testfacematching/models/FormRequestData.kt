package com.kslimweb.testfacematching.models

import okhttp3.MultipartBody
import okhttp3.RequestBody

data class FormRequestData(val imagePart:  MultipartBody.Part,
                           val videoPart:  MultipartBody.Part,
                           val thresholdFormData: RequestBody,
                           val toleranceFormData: RequestBody)
