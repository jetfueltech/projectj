import { NextResponse } from 'next/server';

import { verifyInternalSecret } from '@/lib/auth/mobile';
import {
  replaceObstacles,
  replacePlanes,
  replaceWaypoints,
  updateJobStatus,
} from '@/lib/db/queries';

// Posted by the Python service after recon processing finishes.
// Body shape:
// {
//   planes: [{ polygon, areaSqFt, pitchDegrees, azimuthDegrees, ridgeHeightM, eaveHeightM }],
//   obstacles: [{ kind, geometry, heightM }],
//   waypoints: [{ ordering, latitude, longitude, altitudeM, headingDeg, gimbalPitchDeg, speedMs, action }]
// }
export async function POST(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  if (!verifyInternalSecret(req)) {
    return NextResponse.json({ error: 'forbidden' }, { status: 403 });
  }
  const { id } = await params;
  const body = await req.json();

  await replacePlanes(id, body.planes ?? []);
  await replaceObstacles(id, body.obstacles ?? []);
  await replaceWaypoints(id, body.waypoints ?? []);
  await updateJobStatus(id, 'mission_ready');

  return NextResponse.json({ ok: true });
}
