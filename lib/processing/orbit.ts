import type { Waypoint } from '@/lib/db/schema';

const M_PER_DEG_LAT = 111_320;

export interface OrbitParams {
  centerLat: number;
  centerLng: number;
  radiusM?: number;
  altitudeM?: number;
  numPoints?: number;
  gimbalPitchDeg?: number;
  speedMs?: number;
}

// Recon orbit: fly a circle around the property looking inward at -45°.
// The resulting set of overlapping oblique photos is what feeds the
// Structure-from-Motion stage that recovers the roof polygon.
export function generateOrbit(
  params: OrbitParams,
): Array<Omit<Waypoint, 'id' | 'jobId'>> {
  const radius = params.radiusM ?? 30;
  const altitude = params.altitudeM ?? 40;
  const n = params.numPoints ?? 18;
  const gimbalPitch = params.gimbalPitchDeg ?? -45;
  const speed = params.speedMs ?? 5;

  const mPerDegLng =
    111_320 * Math.cos((params.centerLat * Math.PI) / 180);

  const waypoints: Array<Omit<Waypoint, 'id' | 'jobId'>> = [];
  for (let i = 0; i < n; i++) {
    const angle = (i / n) * 2 * Math.PI;
    const dx = radius * Math.cos(angle);
    const dy = radius * Math.sin(angle);
    const lat = params.centerLat + dy / M_PER_DEG_LAT;
    const lng = params.centerLng + dx / mPerDegLng;

    // Compass heading that points the camera back toward the centre.
    const headingDeg =
      (((90 - (Math.atan2(-dy, -dx) * 180) / Math.PI) % 360) + 360) % 360;

    waypoints.push({
      ordering: i,
      latitude: lat,
      longitude: lng,
      altitudeM: altitude,
      headingDeg,
      gimbalPitchDeg: gimbalPitch,
      speedMs: speed,
      action: 'shoot_photo',
    });
  }
  return waypoints;
}
