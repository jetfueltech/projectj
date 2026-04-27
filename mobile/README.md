# Roof Recon — Android (DJI MSDK v5)

Android client that flies the inspection autonomously.

## Build

```bash
./gradlew assembleDebug \
    -PbackendUrl=https://your-deployment.vercel.app \
    -PdjiAppKey=YOUR_DJI_APP_KEY
```

When running locally against `pnpm dev`, use `-PbackendUrl=http://10.0.2.2:3000`.

## Flight model

Both stages of the inspection are autonomous waypoint missions:

1. **Recon orbit** — when a job is created, the web backend seeds a circular
   orbit (~30 m radius, 40 m alt, gimbal -45°) around the property's GPS
   point. The phone fetches those waypoints, builds a WPML KMZ, pushes it
   to the aircraft, and runs `startMission`.
2. **Mission grid** — once recon photos are processed, the Python planner
   replaces the orbit waypoints with a boustrophedon grid clipped to the
   detected roof polygon (gimbal -90°, sensor-driven overlap). The phone
   runs the same upload + execute path with the new waypoints.

Both flights end with `MediaSync` pulling the captured JPEGs off the
aircraft's SD card and uploading each with its EXIF/GPS telemetry.

## Wired (against MSDK 5.8)

* `DroneController.uploadMission` — `WaypointMissionManager.pushKMZFileToAircraft`.
* `DroneController.runMission` — `startMission` plus
  `WaypointMissionExecuteStateListener` that resumes on
  `FINISHED` / `FAILED` / `NOT_SUPPORTED`.
* `MissionKmzBuilder` — emits valid WPML KMZ with `takePhoto` action groups
  and per-waypoint gimbal pitch.
* `MediaSync` — refreshes the on-aircraft file list, downloads JPEGs taken
  after takeoff, parses EXIF lat/lng/altitude.

## Still TODO before flying

* Verify `WaypointMissionExecuteStateListener` interface name on your
  installed MSDK version (5.8 uses `onMissionStateUpdate`).
* Parse DJI XMP tags (`drone-dji:GimbalPitchDegree`, `FlightYawDegree`,
  `FlightPitchDegree`, `FlightRollDegree`) in `MediaSync.readTelemetry` —
  AndroidX `ExifInterface` skips XMP, so use a dedicated XMP reader or the
  DJI camera's `ImageMetadata` callbacks during capture.
* Confirm the `<wpml:droneEnumValue>` in `MissionKmzBuilder` matches your
  aircraft (77 = Mavic 3E; see DJI WPML spec for others).
* Get a DJI app key registered against your bundle ID
  (`com.roofrecon.mobile`) and pass it as `-PdjiAppKey=...`.
* Test with the DJI Mobile SDK Bridge / aircraft simulator before live flight.
