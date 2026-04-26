package com.roofrecon.mobile.net

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.roofrecon.mobile.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit

object BackendClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    val api: BackendApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_URL)
        .client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(BackendApi::class.java)

    fun bearer(token: String) = "Bearer $token"

    fun textPart(value: String?): RequestBody? =
        value?.toRequestBody("text/plain".toMediaType())

    fun imagePart(file: File): MultipartBody.Part {
        val body = file.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData("file", file.name, body)
    }
}
