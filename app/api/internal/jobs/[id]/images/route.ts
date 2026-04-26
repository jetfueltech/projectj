import { NextResponse } from 'next/server';

import { verifyInternalSecret } from '@/lib/auth/mobile';
import { listImages } from '@/lib/db/queries';

// The Python service fetches the list of images (with EXIF/telemetry + blob URLs)
// for a job so it can pull them down for processing.
export async function GET(
  req: Request,
  { params }: { params: Promise<{ id: string }> },
) {
  if (!verifyInternalSecret(req)) {
    return NextResponse.json({ error: 'forbidden' }, { status: 403 });
  }
  const { id } = await params;
  const url = new URL(req.url);
  const kind = url.searchParams.get('kind') as
    | 'recon'
    | 'mission'
    | 'orthomosaic'
    | null;
  const images = await listImages(id, kind ?? undefined);
  return NextResponse.json({ images });
}
