import { api } from './client';

export interface WatchlistItem {
  id: number;
  termCode: string;
  sisSectionId: string;
  coursePrefix: string | null;
  priority: number;
  createdAt: string;
}

export async function listWatchlist(): Promise<WatchlistItem[]> {
  const res = await api('/api/watchlist');
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  return res.json();
}

export async function addWatchlist(termCode: string, sisSectionId: string, coursePrefix?: string | null): Promise<WatchlistItem> {
  const res = await api('/api/watchlist', {
    method: 'POST',
    body: JSON.stringify({ termCode, sisSectionId, coursePrefix: coursePrefix ?? null, priority: 0 }),
  });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  return res.json();
}

export async function removeWatchlist(termCode: string, sisSectionId: string): Promise<void> {
  const res = await api(`/api/watchlist?termCode=${encodeURIComponent(termCode)}&sisSectionId=${encodeURIComponent(sisSectionId)}`, {
    method: 'DELETE',
  });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
}
