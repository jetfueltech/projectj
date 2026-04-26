import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface ApplicationError extends Error {
  info: string;
  status: number;
}

export const fetcher = async (url: string) => {
  const res = await fetch(url);
  if (!res.ok) {
    const err = new Error('Request failed') as ApplicationError;
    err.info = await res.text();
    err.status = res.status;
    throw err;
  }
  return res.json();
};

export function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

export function formatDate(d: Date | string | null | undefined) {
  if (!d) return '—';
  const date = typeof d === 'string' ? new Date(d) : d;
  return date.toLocaleString();
}

const STATUS_LABELS: Record<string, string> = {
  created: 'Created',
  recon_capturing: 'Recon flight in progress',
  recon_uploaded: 'Recon images uploaded',
  recon_processing: 'Detecting roof planes',
  mission_ready: 'Mission ready',
  mission_capturing: 'Mission flight in progress',
  mission_uploaded: 'Mission images uploaded',
  mission_processing: 'Stitching & detecting damage',
  complete: 'Complete',
  failed: 'Failed',
};

export function statusLabel(s: string) {
  return STATUS_LABELS[s] ?? s;
}
