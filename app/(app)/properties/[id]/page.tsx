import Link from 'next/link';
import { notFound, redirect } from 'next/navigation';

import { auth } from '@/app/(auth)/auth';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import {
  createJob,
  getProperty,
  listJobsForProperty,
  replaceWaypoints,
} from '@/lib/db/queries';
import { generateOrbit } from '@/lib/processing/orbit';
import { formatDate, statusLabel } from '@/lib/utils';

async function startJobAction(formData: FormData) {
  'use server';
  const session = await auth();
  if (!session?.user) redirect('/login');

  const userId = session.user.id as string;
  const propertyId = String(formData.get('propertyId'));
  const property = await getProperty(propertyId, userId);

  const job = await createJob({ userId, propertyId });

  // Seed recon orbit waypoints if we have a property location. The mobile
  // app fetches these before takeoff and flies the orbit autonomously.
  // After recon processing, replaceWaypoints overwrites these with the
  // mission grid produced by the Python planner.
  if (property?.latitude != null && property?.longitude != null) {
    const orbit = generateOrbit({
      centerLat: property.latitude,
      centerLng: property.longitude,
    });
    await replaceWaypoints(job.id, orbit);
  }

  redirect(`/jobs/${job.id}`);
}

export default async function PropertyDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const session = await auth();
  const userId = session!.user!.id as string;

  const property = await getProperty(id, userId);
  if (!property) notFound();

  const jobs = await listJobsForProperty(id, userId);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-semibold">{property.customerName}</h1>
          <p className="text-muted-foreground">
            {property.addressLine1}
            {property.addressLine2 ? `, ${property.addressLine2}` : ''}, {property.city},{' '}
            {property.state} {property.postalCode}
          </p>
          {property.notes && (
            <p className="mt-2 max-w-2xl text-sm">{property.notes}</p>
          )}
        </div>
        <form action={startJobAction}>
          <input type="hidden" name="propertyId" value={property.id} />
          <Button type="submit">Start inspection</Button>
        </form>
      </div>

      <section>
        <h2 className="mb-3 text-lg font-medium">Inspections</h2>
        {jobs.length === 0 ? (
          <Card className="p-6 text-center text-muted-foreground">
            No inspections yet for this property.
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
      </section>
    </div>
  );
}
