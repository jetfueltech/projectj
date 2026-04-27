package com.roofrecon.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.roofrecon.mobile.auth.SessionStore
import com.roofrecon.mobile.dji.DroneController
import com.roofrecon.mobile.dji.MediaSync
import com.roofrecon.mobile.net.BackendClient
import com.roofrecon.mobile.net.JobDetailResponse
import com.roofrecon.mobile.net.StatusUpdate
import com.roofrecon.mobile.upload.ImageUploader
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteState
import kotlinx.coroutines.launch
import java.util.Date

/**
 * Per-job control surface. Runs both autonomous flights end-to-end:
 *
 *   Recon: orbit waypoints (seeded by the web app from the property's GPS)
 *          -> uploadMission -> runMission -> MediaSync -> status flips to
 *          recon_uploaded. The user then taps "Process recon" on the web
 *          dashboard, which dispatches the Python planner. When the planner
 *          callbacks complete, status flips to mission_ready and the
 *          waypoints in the backend are now the mission grid.
 *
 *   Mission: same flow with the new (grid) waypoints. After upload, the user
 *            triggers "Process mission" on the web side, which kicks off
 *            stitching + damage detection + report.
 */
class JobActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val jobId = requireNotNull(intent.getStringExtra(EXTRA_JOB_ID))
        setContent {
            MaterialTheme { Scaffold { p -> JobScreen(Modifier.padding(p), jobId) } }
        }
    }

    companion object {
        const val EXTRA_JOB_ID = "jobId"
    }
}

@Composable
private fun JobScreen(modifier: Modifier, jobId: String) {
    val ctx = LocalContext.current
    val token = remember { SessionStore.token(ctx)!! }
    val scope = rememberCoroutineScope()
    val drone = remember { DroneController() }
    val uploader = remember { ImageUploader(token) }
    val mediaSync = remember { MediaSync(ctx, uploader) }

    var detail by remember { mutableStateOf<JobDetailResponse?>(null) }
    var phase by remember { mutableStateOf("idle") }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        try {
            detail = BackendClient.api.getJob(BackendClient.bearer(token), jobId)
        } catch (e: Exception) {
            error = e.message
        }
    }

    suspend fun setStatus(status: String, errorMessage: String? = null) {
        BackendClient.api.updateStatus(
            BackendClient.bearer(token), jobId,
            StatusUpdate(status = status, errorMessage = errorMessage),
        )
    }

    suspend fun runFlight(
        kind: ImageUploader.Kind,
        capturingStatus: String,
        uploadedStatus: String,
    ) {
        val wps = detail?.waypoints.orEmpty()
        if (wps.isEmpty()) {
            error = "no waypoints loaded for this job"
            return
        }
        try {
            val takeoff = Date()
            phase = "uploading mission"
            setStatus(capturingStatus)
            val missionName = drone.uploadMission(jobId, wps)

            phase = "flying"
            val state = drone.runMission(missionName)
            if (state != WaypointMissionExecuteState.FINISHED) {
                throw DroneController.DjiException("mission ended in $state")
            }

            phase = "downloading photos"
            mediaSync.pullAndUpload(jobId, kind, takeoff)

            setStatus(uploadedStatus)
            phase = "idle"
            refresh()
        } catch (e: Exception) {
            error = e.message
            phase = "idle"
            try { setStatus("failed", e.message) } catch (_: Exception) {}
        }
    }

    LaunchedEffect(jobId) { refresh() }

    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Job ${jobId.take(8)}", style = MaterialTheme.typography.titleLarge)
        Text("Status: ${detail?.job?.status ?: "loading…"}")
        Text("Property: ${detail?.property?.customerName ?: "—"}")
        Text("Waypoints loaded: ${detail?.waypoints?.size ?: 0}")
        if (phase != "idle") Text("Phase: $phase")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        val status = detail?.job?.status
        Button(
            enabled = status == "created" && (detail?.waypoints?.isNotEmpty() == true),
            onClick = {
                scope.launch {
                    runFlight(
                        ImageUploader.Kind.RECON,
                        capturingStatus = "recon_capturing",
                        uploadedStatus = "recon_uploaded",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Run recon orbit") }

        Button(
            enabled = status == "mission_ready",
            onClick = {
                scope.launch {
                    runFlight(
                        ImageUploader.Kind.MISSION,
                        capturingStatus = "mission_capturing",
                        uploadedStatus = "mission_uploaded",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Run mission flight") }

        Button(
            onClick = { drone.returnHome() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Return to home") }
    }
}
