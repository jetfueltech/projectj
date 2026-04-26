import { NextResponse } from 'next/server';

import { issueMobileToken } from '@/lib/auth/mobile';

export async function POST(req: Request) {
  const { email, password } = await req.json().catch(() => ({}));
  if (!email || !password) {
    return NextResponse.json({ error: 'missing credentials' }, { status: 400 });
  }
  const result = await issueMobileToken(email, password);
  if (!result) {
    return NextResponse.json({ error: 'invalid credentials' }, { status: 401 });
  }
  return NextResponse.json(result);
}
