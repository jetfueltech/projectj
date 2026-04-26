import { NextResponse } from 'next/server';

import { verifyMobileToken } from '@/lib/auth/mobile';
import { getJob, updateJobStatus } from '@/lib/db/queries';
import { jobStatusValues, type JobStatus } from '@/lib/db/schema';

const MOBILE_ALLOWED: ReadonlySet<JobStatus> = new Set([
  'recon_capturing',
  'recon_uploaded',
  'mission_capturing',
  'mission_uploaded',
  'failed',
]);

export async function POST(
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

  const body = await req.json().catch(() => ({}));
  const status = body.status as JobStatus | undefined;
  if (
    !status ||
    !jobStatusValues.includes(status) ||
    !MOBILE_ALLOWED.has(status)
  ) {
    return NextResponse.json({ error: 'invalid status' }, { status: 400 });
  }

  await updateJobStatus(id, status, body.errorMessage);
  return NextResponse.json({ ok: true });
}
