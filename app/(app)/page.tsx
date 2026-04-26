import Link from 'next/link';

import { auth } from '@/app/(auth)/auth';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { listProperties } from '@/lib/db/queries';
import { formatDate } from '@/lib/utils';

export default async function PropertiesPage() {
  const session = await auth();
  const userId = session!.user!.id as string;
  const properties = await listProperties(userId);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Properties</h1>
        <Link href="/properties/new">
          <Button>New property</Button>
        </Link>
      </div>

      {properties.length === 0 ? (
        <Card className="p-8 text-center text-muted-foreground">
          No properties yet. Create one to start a roof inspection.
        </Card>
      ) : (
        <div className="grid gap-3">
          {properties.map((p) => (
            <Link key={p.id} href={`/properties/${p.id}`}>
              <Card className="flex items-center justify-between p-4 hover:bg-muted/40">
                <div>
                  <div className="font-medium">{p.customerName}</div>
                  <div className="text-sm text-muted-foreground">
                    {p.addressLine1}, {p.city}, {p.state} {p.postalCode}
                  </div>
                </div>
                <div className="text-xs text-muted-foreground">
                  {formatDate(p.createdAt)}
                </div>
              </Card>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
