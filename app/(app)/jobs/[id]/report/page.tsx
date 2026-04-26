import { notFound, redirect } from 'next/navigation';

import { auth } from '@/app/(auth)/auth';
import { Card } from '@/components/ui/card';
import {
  getJob,
  getProperty,
  getReport,
  listDamages,
  listPlanes,
} from '@/lib/db/queries';
import { formatDate } from '@/lib/utils';

export default async function ReportPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();
  if (!session?.user) redirect('/login');

  const job = await getJob(id);
  if (!job || job.userId !== session.user.id) notFound();

  const [property, report, planes, damages] = await Promise.all([
    getProperty(job.propertyId, job.userId),
    getReport(job.id),
    listPlanes(job.id),
    listDamages(job.id),
  ]);

  if (!report) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        Report not yet available.
      </Card>
    );
  }

  const totalArea = planes.reduce((sum, p) => sum + (p.areaSqFt ?? 0), 0);
  const avgSeverity =
    damages.length > 0
      ? damages.reduce((s, d) => s + d.severity, 0) / damages.length
      : 0;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Inspection report</h1>
        <p className="text-muted-foreground">
          {property?.customerName} · {formatDate(report.createdAt)}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        <Stat label="Roof planes" value={planes.length.toString()} />
        <Stat label="Roof area (sq ft)" value={Math.round(totalArea).toString()} />
        <Stat label="Damages found" value={damages.length.toString()} />
        <Stat label="Avg severity" value={avgSeverity.toFixed(1)} />
      </div>

      {report.summary && (
        <Card className="whitespace-pre-wrap p-6 leading-relaxed">
          {report.summary}
        </Card>
      )}

      {report.pdfUrl && (
        <a
          href={report.pdfUrl}
          target="_blank"
          rel="noreferrer"
          className="text-sm underline"
        >
          Download PDF
        </a>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <Card className="p-4">
      <div className="text-2xl font-semibold">{value}</div>
      <div className="text-xs text-muted-foreground">{label}</div>
    </Card>
  );
}
