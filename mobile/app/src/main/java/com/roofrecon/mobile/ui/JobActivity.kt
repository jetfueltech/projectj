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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import com.roofrecon.mobile.auth.SessionStore
import com.roofrecon.mobile.dji.DroneController
import com.roofrecon.mobile.net.BackendClient
import com.roofrecon.mobile.net.JobDetailResponse
import com.roofrecon.mobile.net.StatusUpdate
import kotlinx.coroutines.launch

/**
 * Per-job control surface. From here a roofer:
 *
 *   1. Triggers the recon flight (manual or automated orbit).
 *   2. Watches for the backend status to flip to `mission_ready` (we poll
 *      every few seconds — could be replaced with SSE/Push).
 *   3. Pushes the planned mission to the aircraft and starts the auto flight.
 *   4. Watches mission upload progress as photos stream off the SD card.
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
    var detail by remember { mutableStateOf<JobDetailResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        try {
            detail = BackendClient.api.getJob(BackendClient.bearer(token), jobId)
        } catch (e: Exception) {
            error = e.message
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
        Text("Waypoints planned: ${detail?.waypoints?.size ?: 0}")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = {
                scope.launch {
                    BackendClient.api.updateStatus(
                        BackendClient.bearer(token), jobId,
                        StatusUpdate(status = "recon_capturing"),
                    )
                    // TODO: hand off to a recon-flight Activity that captures
                    // ~20 photos around the property at 35-50m and uploads each.
                    BackendClient.api.updateStatus(
                        BackendClient.bearer(token), jobId,
                        StatusUpdate(status = "recon_uploaded"),
                    )
                    refresh()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Run recon flight") }

        Button(
            enabled = detail?.waypoints?.isNotEmpty() == true,
            onClick = {
                scope.launch {
                    val wps = detail!!.waypoints
                    BackendClient.api.updateStatus(
                        BackendClient.bearer(token), jobId,
                        StatusUpdate(status = "mission_capturing"),
                    )
                    drone.uploadMission(jobId, wps)
                    drone.runMission(jobId)
                    BackendClient.api.updateStatus(
                        BackendClient.bearer(token), jobId,
                        StatusUpdate(status = "mission_uploaded"),
                    )
                    refresh()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Run mission flight") }
    }
}
