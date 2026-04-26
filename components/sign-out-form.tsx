import Form from 'next/form';

import { signOut } from '@/app/(auth)/auth';

export const SignOutForm = () => {
  return (
    <Form
      action={async () => {
        'use server';
        await signOut({ redirectTo: '/login' });
      }}
    >
      <button type="submit" className="text-red-500 hover:underline">
        Sign out
      </button>
    </Form>
  );
};
