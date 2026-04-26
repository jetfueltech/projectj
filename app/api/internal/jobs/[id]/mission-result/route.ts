import { NextResponse } from 'next/server';

import { verifyInternalSecret } from '@/lib/auth/mobile';
import {
  insertDamages,
  insertImage,
  updateJobStatus,
  upsertReport,
} from '@/lib/db/queries';

// Posted by the Python service after mission processing finishes.
// Body shape:
// {
//   orthomosaicUrl?: string,
//   damages: [{ imageId, planeId, type, severity, confidence, bbox, geo, notes }],
//   report: { pdfUrl, summary, totals }
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

  if (body.orthomosaicUrl) {
    await insertImage({
      jobId: id,
      kind: 'orthomosaic',
      blobUrl: body.orthomosaicUrl,
      filename: 'orthomosaic.tif',
      width: null,
      height: null,
      latitude: null,
      longitude: null,
      altitude: null,
      yaw: null,
      pitch: null,
      roll: null,
      gimbalPitch: null,
      capturedAt: null,
    });
  }

  if (Array.isArray(body.damages) && body.damages.length > 0) {
    await insertDamages(
      body.damages.map((d: any) => ({
        jobId: id,
        imageId: d.imageId ?? null,
        planeId: d.planeId ?? null,
        type: d.type,
        severity: d.severity,
        confidence: d.confidence ?? null,
        bbox: d.bbox ?? null,
        geo: d.geo ?? null,
        notes: d.notes ?? null,
      })),
    );
  }

  if (body.report) {
    await upsertReport({
      jobId: id,
      pdfUrl: body.report.pdfUrl ?? null,
      summary: body.report.summary ?? null,
      totals: body.report.totals ?? null,
    });
  }

  await updateJobStatus(id, 'complete');
  return NextResponse.json({ ok: true });
}
