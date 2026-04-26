import { NextResponse } from 'next/server';

import { auth } from '@/app/(auth)/auth';
import { getJob, updateJobStatus } from '@/lib/db/queries';
import { dispatchProcessing } from '@/lib/processing/dispatch';

export async function POST(
  _req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const session = await auth();
  if (!session?.user) {
    return NextResponse.json({ error: 'unauthorized' }, { status: 401 });
  }
  const { id } = await params;
  const job = await getJob(id);
  if (!job || job.userId !== session.user.id) {
    return NextResponse.json({ error: 'not found' }, { status: 404 });
  }

  await updateJobStatus(id, 'recon_processing');
  try {
    await dispatchProcessing('recon', id);
  } catch (err) {
    await updateJobStatus(id, 'failed', (err as Error).message);
    throw err;
  }
  return NextResponse.json({ ok: true });
}
