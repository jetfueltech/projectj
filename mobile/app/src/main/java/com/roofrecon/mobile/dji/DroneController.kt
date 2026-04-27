package com.roofrecon.mobile.dji

import android.util.Log
import com.roofrecon.mobile.net.Waypoint
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteState
import dji.v5.manager.aircraft.waypoint3.WaypointMissionExecuteStateListener
import dji.v5.manager.aircraft.waypoint3.WaypointMissionManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Drives DJI MSDK v5 mission execution end-to-end.
 *
 *   uploadMission  -> WaypointMissionManager.pushKMZFileToAircraft
 *   runMission     -> WaypointMissionManager.startMission + state listener
 *
 * Both stages of the inspection (recon orbit, mission grid) share the same
 * code path — the only difference is the waypoint set the planner produced.
 *
 * Targets MSDK 5.8.x; if you upgrade, double-check the listener interface
 * name and the WaypointMissionExecuteState enum members.
 */
class DroneController {

    /**
     * Build a WPML KMZ from `waypoints`, push it to the aircraft, and resume
     * once the upload completes. The mission filename returned is what
     * [runMission] expects (without the `.kmz` extension, matching the file
     * name on the aircraft's filesystem).
     */
    suspend fun uploadMission(jobId: String, waypoints: List<Waypoint>): String {
        require(waypoints.isNotEmpty()) { "no waypoints to upload" }
        val kmz = MissionKmzBuilder.build(jobId, waypoints)
        val missionName = kmz.nameWithoutExtension

        suspendCancellableCoroutine<Unit> { cont ->
            WaypointMissionManager.getInstance().pushKMZFileToAircraft(
                kmz.absolutePath,
                object : CommonCallbacks.CompletionCallbackWithProgress<Double> {
                    override fun onSuccess() {
                        Log.i(TAG, "mission upload complete: $missionName")
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onProgressUpdate(progress: Double) {
                        Log.d(TAG, "mission upload ${(progress * 100).toInt()}%")
                    }

                    override fun onFailure(error: IDJIError) {
                        Log.e(TAG, "mission upload failed: $error")
                        if (cont.isActive) cont.resumeWithException(
                            DjiException("upload failed: $error"),
                        )
                    }
                },
            )
        }

        return missionName
    }

    /**
     * Start the previously-uploaded mission and suspend until the aircraft
     * reports a terminal state. Resumes with the final state.
     */
    suspend fun runMission(missionName: String): WaypointMissionExecuteState =
        suspendCancellableCoroutine { cont ->
            val mgr = WaypointMissionManager.getInstance()

            val listener = object : WaypointMissionExecuteStateListener {
                override fun onMissionStateUpdate(state: WaypointMissionExecuteState) {
                    Log.d(TAG, "mission state: $state")
                    when (state) {
                        WaypointMissionExecuteState.FINISHED -> finish(state)
                        WaypointMissionExecuteState.FAILED -> finish(state)
                        WaypointMissionExecuteState.NOT_SUPPORTED -> finish(state)
                        else -> Unit
                    }
                }

                private fun finish(state: WaypointMissionExecuteState) {
                    mgr.removeWaypointMissionExecuteStateListener(this)
                    if (cont.isActive) cont.resume(state)
                }
            }
            mgr.addWaypointMissionExecuteStateListener(listener)

            mgr.startMission(missionName, object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    Log.i(TAG, "mission $missionName started")
                }

                override fun onFailure(error: IDJIError) {
                    Log.e(TAG, "mission start failed: $error")
                    mgr.removeWaypointMissionExecuteStateListener(listener)
                    if (cont.isActive) cont.resumeWithException(
                        DjiException("start failed: $error"),
                    )
                }
            })

            cont.invokeOnCancellation {
                mgr.removeWaypointMissionExecuteStateListener(listener)
                mgr.stopMission(missionName, NoopCallback)
            }
        }

    /** Best-effort RTH fallback if the user aborts mid-flight. */
    fun returnHome() {
        // KeyManager.getInstance().performAction(
        //     KeyTools.createKey(FlightControllerKey.KeyStartGoHome), null
        // )
        Log.i(TAG, "RTH requested")
    }

    private object NoopCallback : CommonCallbacks.CompletionCallback {
        override fun onSuccess() {}
        override fun onFailure(error: IDJIError) {}
    }

    class DjiException(message: String) : RuntimeException(message)

    companion object {
        private const val TAG = "DroneController"
    }
}
