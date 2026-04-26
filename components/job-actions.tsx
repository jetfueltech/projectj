'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import type { JobStatus } from '@/lib/db/schema';

export function JobActions({
  jobId,
  status,
}: {
  jobId: string;
  status: JobStatus;
}) {
  const router = useRouter();
  const [busy, setBusy] = useState<string | null>(null);

  async function trigger(action: 'process-recon' | 'process-mission') {
    setBusy(action);
    try {
      const res = await fetch(`/api/jobs/${jobId}/${action}`, { method: 'POST' });
      if (!res.ok) throw new Error(await res.text());
      toast.success('Processing started');
      router.refresh();
    } catch (err) {
      toast.error(`Failed: ${(err as Error).message}`);
    } finally {
      setBusy(null);
    }
  }

  const canProcessRecon = status === 'recon_uploaded';
  const canProcessMission = status === 'mission_uploaded';

  if (!canProcessRecon && !canProcessMission) {
    return (
      <Card className="bg-muted/40 p-4 text-sm text-muted-foreground">
        Use the Roof Recon mobile app to capture and upload images for this job.
        Processing actions appear here once uploads finish.
      </Card>
    );
  }

  return (
    <Card className="flex items-center justify-between p-4">
      <div className="text-sm text-muted-foreground">
        {canProcessRecon
          ? 'Recon images uploaded. Detect roof planes and generate the mission grid.'
          : 'Mission images uploaded. Stitch orthomosaic and detect damage.'}
      </div>
      <div className="flex gap-2">
        {canProcessRecon && (
          <Button
            disabled={busy !== null}
            onClick={() => trigger('process-recon')}
          >
            {busy === 'process-recon' ? 'Starting…' : 'Process recon'}
          </Button>
        )}
        {canProcessMission && (
          <Button
            disabled={busy !== null}
            onClick={() => trigger('process-mission')}
          >
            {busy === 'process-mission' ? 'Starting…' : 'Process mission'}
          </Button>
        )}
      </div>
    </Card>
  );
}
