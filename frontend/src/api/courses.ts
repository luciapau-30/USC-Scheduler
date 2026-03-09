import { api } from './client';

/** Raw JSON from backend (USC API shape). */
export async function searchCourses(termCode: string, q: string): Promise<string> {
  const res = await api(
    `/api/courses/search?termCode=${encodeURIComponent(termCode)}&q=${encodeURIComponent(q)}`,
    { skipAuth: true }
  );
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  return res.text();
}

export interface TermOption {
  termCode: string;
  label: string;
}

export async function getTerms(): Promise<TermOption[]> {
  const res = await api('/api/courses/terms', { skipAuth: true });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  return res.json();
}
