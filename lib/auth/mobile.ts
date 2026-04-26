import { compare } from 'bcrypt-ts';
import { SignJWT, jwtVerify } from 'jose';

import { getUser, getUserById } from '@/lib/db/queries';

const ISSUER = 'roof-recon';
const AUDIENCE = 'roof-recon-mobile';

function key() {
  const secret = process.env.AUTH_SECRET;
  if (!secret) throw new Error('AUTH_SECRET is not set');
  return new TextEncoder().encode(secret);
}

export async function issueMobileToken(email: string, password: string) {
  const [user] = await getUser(email);
  if (!user?.password) return null;
  const ok = await compare(password, user.password);
  if (!ok) return null;

  const token = await new SignJWT({ sub: user.id })
    .setProtectedHeader({ alg: 'HS256' })
    .setIssuer(ISSUER)
    .setAudience(AUDIENCE)
    .setIssuedAt()
    .setExpirationTime('30d')
    .sign(key());

  return { token, userId: user.id, email: user.email };
}

export async function verifyMobileToken(authorization?: string | null) {
  if (!authorization?.startsWith('Bearer ')) return null;
  const token = authorization.slice(7);
  try {
    const { payload } = await jwtVerify(token, key(), {
      issuer: ISSUER,
      audience: AUDIENCE,
    });
    const userId = payload.sub as string;
    const user = await getUserById(userId);
    return user ?? null;
  } catch {
    return null;
  }
}

export function verifyInternalSecret(req: Request) {
  const sent = req.headers.get('x-internal-secret');
  const expected = process.env.INTERNAL_SECRET;
  if (!expected || !sent) return false;
  return sent === expected;
}
