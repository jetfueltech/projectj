import { NextResponse } from 'next/server';

import { verifyMobileToken } from '@/lib/auth/mobile';
import {
  getJob,
  getProperty,
  listWaypoints,
} from '@/lib/db/queries';

export async function GET(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const user = await verifyMobileToken(req.headers.get('authorization'));
  if (!user) return NextResponse.json({ error: 'unauthorized' }, { status: 401 });

  const { id } = await params;
  const job = await getJob(id);
  if (!job || job.userId !== user.id) {
    return NextResponse.json({ error: 'not found' }, { status: 404 });
  }
  const property = await getProperty(job.propertyId, user.id);
  const waypoints = await listWaypoints(job.id);

  return NextResponse.json({ job, property, waypoints });
}
