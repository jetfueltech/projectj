import { put } from '@vercel/blob';
import { NextResponse } from 'next/server';

import { verifyMobileToken } from '@/lib/auth/mobile';
import {
  getJob,
  insertImage,
  updateJobStatus,
} from '@/lib/db/queries';

const ALLOWED_KINDS = new Set(['recon', 'mission']);

function num(v: FormDataEntryValue | null) {
  if (v === null || v === '') return null;
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

function dateOrNull(v: FormDataEntryValue | null) {
  if (!v) return null;
  const d = new Date(String(v));
  return Number.isNaN(d.getTime()) ? null : d;
}

// Mobile clients POST one image per request as multipart/form-data.
// Fields: file, kind, latitude, longitude, altitude, yaw, pitch, roll,
//         gimbalPitch, capturedAt, width, height.
export async function POST(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  const user = await verifyMobileToken(req.headers.get('authorization'));
  if (!user) return NextResponse.json({ error: 'unauthorized' }, { status: 401 });

  const { id: jobId } = await params;
  const job = await getJob(jobId);
  if (!job || job.userId !== user.id) {
    return NextResponse.json({ error: 'not found' }, { status: 404 });
  }

  const form = await req.formData();
  const file = form.get('file');
  const kind = String(form.get('kind') ?? '');
  if (!(file instanceof Blob) || !ALLOWED_KINDS.has(kind)) {
    return NextResponse.json({ error: 'bad input' }, { status: 400 });
  }

  const filename = String(form.get('filename') ?? `${Date.now()}.jpg`);
  const blob = await put(`jobs/${jobId}/${kind}/${filename}`, file, {
    access: 'public',
    addRandomSuffix: true,
  });

  const row = await insertImage({
    jobId,
    kind: kind as 'recon' | 'mission',
    blobUrl: blob.url,
    filename,
    width: num(form.get('width')),
    height: num(form.get('height')),
    latitude: num(form.get('latitude')),
    longitude: num(form.get('longitude')),
    altitude: num(form.get('altitude')),
    yaw: num(form.get('yaw')),
    pitch: num(form.get('pitch')),
    roll: num(form.get('roll')),
    gimbalPitch: num(form.get('gimbalPitch')),
    capturedAt: dateOrNull(form.get('capturedAt')),
  });

  return NextResponse.json({ image: row });
}
