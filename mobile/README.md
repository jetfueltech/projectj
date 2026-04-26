# Roof Recon — Android (DJI MSDK v5)

Android client for piloting DJI drones through a roof inspection workflow.

## Build

You'll need an Android Studio install with the Android SDK + NDK, plus a DJI
developer account so you can mint an API key.

```bash
./gradlew assembleDebug \
    -PbackendUrl=https://your-deployment.vercel.app \
    -PdjiAppKey=YOUR_DJI_APP_KEY
```

When running locally against `pnpm dev`, use `-PbackendUrl=http://10.0.2.2:3000`
(the Android emulator's loopback to your host).

## What's wired vs what's a stub

Real:
* Sign-in against `/api/mobile/auth`, bearer token persistence.
* Job list + job detail screens.
* Image upload contract (multipart with EXIF/telemetry fields).
* WPML KMZ generation from the backend's waypoint list.

Stubs you should fill before flying:
* `DroneController.capturePhoto` — `KeyManager` + `CameraKey.KeyStartShootPhoto`.
* `DroneController.uploadMission` — `WaypointMissionManager.pushKMZFileToAircraft`.
* `DroneController.runMission` — `startMission` + `WaypointMissionExecutionListener`.
* The recon-flight Activity that orbits the property and uploads each photo.
* Pulling captured photos off the SD card with `MediaManager` after the mission.

## Drone support

Defaults assume a DJI Mavic 3 Enterprise (drone enum `77` in WPML). Other v5
aircraft work too — change the `<wpml:droneEnumValue>` in
`MissionKmzBuilder.kt` and the sensor constants in the Python planner.
