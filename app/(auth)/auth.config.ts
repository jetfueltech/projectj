import type { NextAuthConfig } from 'next-auth';

export const authConfig = {
  pages: {
    signIn: '/login',
    newUser: '/',
  },
  providers: [
    // added in auth.ts (bcrypt requires Node.js)
  ],
  callbacks: {
    authorized({ auth, request: { nextUrl } }) {
      const isLoggedIn = !!auth?.user;
      const path = nextUrl.pathname;
      const isAuthPage = path.startsWith('/login') || path.startsWith('/register');
      // Mobile API uses bearer auth, not the web session.
      const isMobileApi = path.startsWith('/api/mobile');
      // Internal callbacks from the Python service authenticate via shared secret.
      const isInternalApi = path.startsWith('/api/internal');

      if (isMobileApi || isInternalApi) return true;

      if (isAuthPage) {
        if (isLoggedIn) {
          return Response.redirect(new URL('/', nextUrl as unknown as URL));
        }
        return true;
      }

      return isLoggedIn;
    },
  },
} satisfies NextAuthConfig;
