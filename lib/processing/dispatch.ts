import 'server-only';

const PYTHON_URL = process.env.PYTHON_SERVICE_URL;
const INTERNAL_SECRET = process.env.INTERNAL_SECRET;

type Stage = 'recon' | 'mission';

export async function dispatchProcessing(stage: Stage, jobId: string) {
  if (!PYTHON_URL) throw new Error('PYTHON_SERVICE_URL is not set');
  if (!INTERNAL_SECRET) throw new Error('INTERNAL_SECRET is not set');

  const res = await fetch(`${PYTHON_URL}/process/${stage}`, {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      'x-internal-secret': INTERNAL_SECRET,
    },
    body: JSON.stringify({ jobId }),
  });

  if (!res.ok) {
    throw new Error(`python service error ${res.status}: ${await res.text()}`);
  }
  return res.json();
}
