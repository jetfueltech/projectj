package com.roofrecon.mobile.dji

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.roofrecon.mobile.upload.ImageUploader
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.media.MediaFile
import dji.v5.manager.datacenter.media.MediaFileListData
import dji.v5.manager.datacenter.media.MediaFileListState
import dji.v5.manager.datacenter.media.PullMediaFileListParam
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Pulls photos taken during a mission off the aircraft and uploads them
 * (with their telemetry) to the backend.
 *
 * Flow:
 *   1. Refresh the on-aircraft media file list via `MediaDataCenter`.
 *   2. Filter to JPEGs whose creation time is at or after `since`.
 *   3. For each file, request a full-resolution download to local cache.
 *   4. Read EXIF (lat/lng/altitude/yaw/pitch/gimbal pitch) off the
 *      downloaded JPEG and POST the file + telemetry to the backend.
 *
 * The MSDK MediaManager surface in v5 lives under
 * `dji.v5.manager.datacenter.MediaDataCenter`. The exact method names below
 * track the 5.8 sample app — adjust if you upgrade.
 */
class MediaSync(
    private val context: Context,
    private val uploader: ImageUploader,
) {

    suspend fun pullAndUpload(
        jobId: String,
        kind: ImageUploader.Kind,
        since: Date,
    ) {
        val files = refreshFileList().filter { mf ->
            mf.timeCreated.after(since) && mf.fileType == MediaFile.MediaFileType.JPEG
        }
        Log.i(TAG, "pulling ${files.size} files since $since")

        for (mf in files) {
            val local = downloadToCache(mf)
            val telemetry = readTelemetry(local, mf)
            uploader.upload(jobId, kind, local, telemetry)
        }
    }

    private suspend fun refreshFileList(): List<MediaFile> =
        suspendCancellableCoroutine { cont ->
            val mgr = MediaDataCenter.getInstance().mediaManager
            mgr.pullMediaFileListFromCamera(
                PullMediaFileListParam.Builder().build(),
                object : CommonCallbacks.CompletionCallbackWithParam<MediaFileListData> {
                    override fun onSuccess(data: MediaFileListData) {
                        if (cont.isActive) cont.resume(data.data ?: emptyList())
                    }
                    override fun onFailure(error: IDJIError) {
                        if (cont.isActive) cont.resumeWithException(
                            DroneController.DjiException("pull list failed: $error"),
                        )
                    }
                },
            )
        }

    private suspend fun downloadToCache(file: MediaFile): File =
        suspendCancellableCoroutine { cont ->
            val out = File(context.cacheDir, file.fileName).apply { delete() }
            file.pullOriginalMediaFileFromCamera(
                0,
                out.absolutePath,
                object : CommonCallbacks.DownloadFileCallback {
                    override fun onStart() {}
                    override fun onProgress(total: Long, current: Long) {}
                    override fun onRealtimeDataUpdate(
                        data: ByteArray?,
                        offset: Long,
                        eof: Boolean,
                    ) {}
                    override fun onFinish() {
                        if (cont.isActive) cont.resume(out)
                    }
                    override fun onFailure(error: IDJIError) {
                        if (cont.isActive) cont.resumeWithException(
                            DroneController.DjiException("download failed: $error"),
                        )
                    }
                },
            )
        }

    private fun readTelemetry(file: File, mf: MediaFile): ImageUploader.Telemetry {
        val exif = ExifInterface(file.absolutePath)
        val latLng = FloatArray(2)
        val hasLatLng = exif.getLatLong(latLng)

        // Altitude is in EXIF.GPSAltitude; DJI also writes
        // XMP-drone-dji:GimbalPitchDegree etc. but those need an XMP parser.
        val altitude = exif.getAltitude(Double.NaN)
            .takeUnless { it.isNaN() }

        return ImageUploader.Telemetry(
            latitude = if (hasLatLng) latLng[0].toDouble() else null,
            longitude = if (hasLatLng) latLng[1].toDouble() else null,
            altitude = altitude,
            yaw = null,            // TODO: parse from XMP-drone-dji:FlightYawDegree
            pitch = null,          // TODO: parse from XMP-drone-dji:FlightPitchDegree
            roll = null,           // TODO: parse from XMP-drone-dji:FlightRollDegree
            gimbalPitch = null,    // TODO: parse from XMP-drone-dji:GimbalPitchDegree
            capturedAtIso = ISO.format(mf.timeCreated),
        )
    }

    companion object {
        private const val TAG = "MediaSync"
        private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    }
}
