import Link from 'next/link';

import { auth } from '@/app/(auth)/auth';
import { SignOutForm } from '@/components/sign-out-form';

export async function SiteHeader() {
  const session = await auth();

  return (
    <header className="border-b">
      <div className="mx-auto flex h-14 max-w-6xl items-center justify-between px-4">
        <div className="flex items-center gap-6">
          <Link href="/" className="font-semibold">
            Roof Recon
          </Link>
          <nav className="flex items-center gap-4 text-sm text-muted-foreground">
            <Link href="/" className="hover:text-foreground">Properties</Link>
            <Link href="/jobs" className="hover:text-foreground">Jobs</Link>
          </nav>
        </div>
        <div className="flex items-center gap-3 text-sm">
          {session?.user?.email && (
            <span className="text-muted-foreground">{session.user.email}</span>
          )}
          <SignOutForm />
        </div>
      </div>
    </header>
  );
}
