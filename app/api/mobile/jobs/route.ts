import { NextResponse } from 'next/server';

import { verifyMobileToken } from '@/lib/auth/mobile';
import { listJobsForUser } from '@/lib/db/queries';

export async function GET(req: Request) {
  const user = await verifyMobileToken(req.headers.get('authorization'));
  if (!user) return NextResponse.json({ error: 'unauthorized' }, { status: 401 });

  const jobs = await listJobsForUser(user.id);
  return NextResponse.json({ jobs });
}
