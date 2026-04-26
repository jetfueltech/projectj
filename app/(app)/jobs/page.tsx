import Link from 'next/link';

import { auth } from '@/app/(auth)/auth';
import { Card } from '@/components/ui/card';
import { listJobsForUser } from '@/lib/db/queries';
import { formatDate, statusLabel } from '@/lib/utils';

export default async function JobsPage() {
  const session = await auth();
  const userId = session!.user!.id as string;
  const jobs = await listJobsForUser(userId);

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold">Inspections</h1>
      {jobs.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          No inspections yet.
        </Card>
      ) : (
        <div className="grid gap-2">
          {jobs.map((j) => (
            <Link key={j.id} href={`/jobs/${j.id}`}>
              <Card className="flex items-center justify-between p-4 hover:bg-muted/40">
                <div>
                  <div className="font-medium">{statusLabel(j.status)}</div>
                  <div className="text-xs text-muted-foreground">
                    Job {j.id.slice(0, 8)} · {formatDate(j.createdAt)}
                  </div>
                </div>
                {j.droneModel && (
                  <span className="text-xs text-muted-foreground">
                    {j.droneModel}
                  </span>
                )}
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
