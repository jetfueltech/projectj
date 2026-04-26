package com.roofrecon.mobile.upload

import android.util.Log
import com.roofrecon.mobile.net.BackendClient
import java.io.File

/**
 * Pulls captured images off the aircraft (via DJI MediaManager) and uploads
 * them to the backend with their EXIF/telemetry attached as form fields.
 *
 * The actual MediaManager wiring is left as a TODO; this class shows the
 * call-site contract used by [com.roofrecon.mobile.ui.JobActivity].
 */
class ImageUploader(private val token: String) {

    suspend fun upload(jobId: String, kind: Kind, file: File, telemetry: Telemetry) {
        val api = BackendClient.api
        api.uploadImage(
            bearer = BackendClient.bearer(token),
            jobId = jobId,
            file = BackendClient.imagePart(file),
            kind = BackendClient.textPart(kind.serialized)!!,
            filename = BackendClient.textPart(file.name)!!,
            latitude = BackendClient.textPart(telemetry.latitude?.toString()),
            longitude = BackendClient.textPart(telemetry.longitude?.toString()),
            altitude = BackendClient.textPart(telemetry.altitude?.toString()),
            yaw = BackendClient.textPart(telemetry.yaw?.toString()),
            pitch = BackendClient.textPart(telemetry.pitch?.toString()),
            roll = BackendClient.textPart(telemetry.roll?.toString()),
            gimbalPitch = BackendClient.textPart(telemetry.gimbalPitch?.toString()),
            capturedAt = BackendClient.textPart(telemetry.capturedAtIso),
        )
        Log.i(TAG, "uploaded ${file.name} for job=$jobId kind=$kind")
    }

    enum class Kind(val serialized: String) {
        RECON("recon"),
        MISSION("mission"),
    }

    data class Telemetry(
        val latitude: Double?,
        val longitude: Double?,
        val altitude: Double?,
        val yaw: Double?,
        val pitch: Double?,
        val roll: Double?,
        val gimbalPitch: Double?,
        val capturedAtIso: String?,
    )

    companion object {
        private const val TAG = "ImageUploader"
    }
}
