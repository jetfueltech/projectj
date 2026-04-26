package com.roofrecon.mobile.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val userId: String, val email: String)

@Serializable
data class Job(
    val id: String,
    val propertyId: String,
    val status: String,
    val droneSerial: String? = null,
    val droneModel: String? = null,
)

@Serializable
data class JobListResponse(val jobs: List<Job>)

@Serializable
data class Property(
    val id: String,
    val customerName: String,
    val addressLine1: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class Waypoint(
    val ordering: Int,
    val latitude: Double,
    val longitude: Double,
    @SerialName("altitudeM") val altitudeM: Double,
    @SerialName("headingDeg") val headingDeg: Double? = null,
    @SerialName("gimbalPitchDeg") val gimbalPitchDeg: Double? = null,
    @SerialName("speedMs") val speedMs: Double? = null,
    val action: String? = null,
)

@Serializable
data class JobDetailResponse(
    val job: Job,
    val property: Property?,
    val waypoints: List<Waypoint>,
)

@Serializable
data class StatusUpdate(val status: String, val errorMessage: String? = null)

interface BackendApi {

    @POST("/api/mobile/auth")
    suspend fun login(@Body req: LoginRequest): LoginResponse

    @GET("/api/mobile/jobs")
    suspend fun listJobs(@Header("Authorization") bearer: String): JobListResponse

    @GET("/api/mobile/jobs/{id}")
    suspend fun getJob(
        @Header("Authorization") bearer: String,
        @Path("id") jobId: String,
    ): JobDetailResponse

    @POST("/api/mobile/jobs/{id}/status")
    suspend fun updateStatus(
        @Header("Authorization") bearer: String,
        @Path("id") jobId: String,
        @Body body: StatusUpdate,
    )

    @Multipart
    @POST("/api/mobile/jobs/{id}/images")
    suspend fun uploadImage(
        @Header("Authorization") bearer: String,
        @Path("id") jobId: String,
        @Part file: MultipartBody.Part,
        @Part("kind") kind: RequestBody,
        @Part("filename") filename: RequestBody,
        @Part("latitude") latitude: RequestBody?,
        @Part("longitude") longitude: RequestBody?,
        @Part("altitude") altitude: RequestBody?,
        @Part("yaw") yaw: RequestBody?,
        @Part("pitch") pitch: RequestBody?,
        @Part("roll") roll: RequestBody?,
        @Part("gimbalPitch") gimbalPitch: RequestBody?,
        @Part("capturedAt") capturedAt: RequestBody?,
    )
}
