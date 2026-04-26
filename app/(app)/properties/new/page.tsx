import { redirect } from 'next/navigation';

import { auth } from '@/app/(auth)/auth';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { createProperty } from '@/lib/db/queries';

async function createPropertyAction(formData: FormData) {
  'use server';
  const session = await auth();
  if (!session?.user) redirect('/login');

  const latitude = formData.get('latitude');
  const longitude = formData.get('longitude');

  const row = await createProperty({
    userId: session.user.id as string,
    customerName: String(formData.get('customerName') ?? ''),
    customerPhone: (formData.get('customerPhone') as string) || null,
    customerEmail: (formData.get('customerEmail') as string) || null,
    addressLine1: String(formData.get('addressLine1') ?? ''),
    addressLine2: (formData.get('addressLine2') as string) || null,
    city: String(formData.get('city') ?? ''),
    state: String(formData.get('state') ?? ''),
    postalCode: String(formData.get('postalCode') ?? ''),
    latitude: latitude ? Number(latitude) : null,
    longitude: longitude ? Number(longitude) : null,
    notes: (formData.get('notes') as string) || null,
  });

  redirect(`/properties/${row.id}`);
}

export default function NewPropertyPage() {
  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 text-2xl font-semibold">New property</h1>
      <Card className="p-6">
        <form action={createPropertyAction} className="grid gap-4">
          <Field label="Customer name" name="customerName" required />
          <div className="grid grid-cols-2 gap-4">
            <Field label="Phone" name="customerPhone" />
            <Field label="Email" name="customerEmail" type="email" />
          </div>

          <Field label="Address line 1" name="addressLine1" required />
          <Field label="Address line 2" name="addressLine2" />

          <div className="grid grid-cols-3 gap-4">
            <Field label="City" name="city" required />
            <Field label="State" name="state" required />
            <Field label="Postal code" name="postalCode" required />
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Field label="Latitude" name="latitude" type="number" step="any" />
            <Field label="Longitude" name="longitude" type="number" step="any" />
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="notes">Notes</Label>
            <Textarea id="notes" name="notes" rows={3} />
          </div>

          <div className="flex justify-end gap-2">
            <Button type="submit">Create property</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}

function Field({
  label,
  name,
  type = 'text',
  required,
  step,
}: {
  label: string;
  name: string;
  type?: string;
  required?: boolean;
  step?: string;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label htmlFor={name}>
        {label}
        {required && <span className="text-red-500"> *</span>}
      </Label>
      <Input id={name} name={name} type={type} step={step} required={required} />
    </div>
  );
}
