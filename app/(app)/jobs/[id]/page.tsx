import Link from 'next/link';
import { notFound, redirect } from 'next/navigation';

import { auth } from '@/app/(auth)/auth';
import { JobActions } from '@/components/job-actions';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  getJob,
  getProperty,
  listImages,
  listPlanes,
  listWaypoints,
  getReport,
  listDamages,
} from '@/lib/db/queries';
import { formatDate, statusLabel } from '@/lib/utils';

export default async function JobPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();
  if (!session?.user) redirect('/login');

  const job = await getJob(id);
  if (!job || job.userId !== session.user.id) notFound();
  const property = await getProperty(job.propertyId, job.userId);
  const [reconImages, missionImages, planes, waypoints, damages, report] =
    await Promise.all([
      listImages(job.id, 'recon'),
      listImages(job.id, 'mission'),
      listPlanes(job.id),
      listWaypoints(job.id),
      listDamages(job.id),
      getReport(job.id),
    ]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold">
            {property?.customerName ?? 'Inspection'}
          </h1>
          <p className="text-muted-foreground">
            Job {job.id.slice(0, 8)} · {formatDate(job.createdAt)}
          </p>
          <p className="mt-1 text-sm">
            Status: <span className="font-medium">{statusLabel(job.status)}</span>
          </p>
          {job.errorMessage && (
            <p className="mt-1 text-sm text-red-500">{job.errorMessage}</p>
          )}
        </div>
        {report?.pdfUrl && (
          <Link href={`/jobs/${job.id}/report`}>
            <Button variant="outline">View report</Button>
          </Link>
        )}
      </div>

      <JobActions jobId={job.id} status={job.status} />

      <section className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Stat label="Recon images" value={reconImages.length} />
        <Stat label="Mission images" value={missionImages.length} />
        <Stat label="Roof planes" value={planes.length} />
        <Stat label="Waypoints" value={waypoints.length} />
      </section>

      {damages.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-medium">Detected damage</h2>
          <Card className="divide-y">
            {damages.map((d) => (
              <div
                key={d.id}
                className="flex items-center justify-between px-4 py-3"
              >
                <div>
                  <div className="font-medium capitalize">
                    {d.type.replace(/_/g, ' ')}
                  </div>
                  {d.notes && (
                    <div className="text-xs text-muted-foreground">{d.notes}</div>
                  )}
                </div>
                <div className="text-sm text-muted-foreground">
                  Severity {d.severity} ·{' '}
                  {d.confidence ? `${Math.round(d.confidence * 100)}%` : '—'}
                </div>
              </div>
            ))}
          </Card>
        </section>
      )}

      {reconImages.length > 0 && (
        <ImageGrid title="Recon images" images={reconImages} />
      )}
      {missionImages.length > 0 && (
        <ImageGrid title="Mission images" images={missionImages} />
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <Card className="p-4">
      <div className="text-2xl font-semibold">{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </Card>
  );
}

function ImageGrid({
  title,
  images,
}: {
  title: string;
  images: Array<{ id: string; blobUrl: string; filename: string | null }>;
}) {
  return (
    <section>
      <h2 className="mb-3 text-lg font-medium">{title}</h2>
      <div className="grid grid-cols-3 gap-2 md:grid-cols-6">
        {images.map((img) => (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            key={img.id}
            src={img.blobUrl}
            alt={img.filename ?? img.id}
            className="aspect-square rounded-md object-cover"
          />
        ))}
      </div>
    </section>
  );
}
