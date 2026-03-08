import { useEffect, useState } from 'react';
import { listWatchlist, removeWatchlist, type WatchlistItem } from '../api/watchlist';
import { useNotifications } from '../hooks/useNotifications';
import './Watchlist.css';

export default function Watchlist() {
  const [items, setItems] = useState<WatchlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [alert, setAlert] = useState<{ termCode: string; sisSectionId: string } | null>(null);

  useNotifications((payload) => {
    if (payload.eventType === 'seat_opened') setAlert({ termCode: payload.termCode, sisSectionId: payload.sisSectionId });
  });

  async function load() {
    setLoading(true);
    setError('');
    try {
      const list = await listWatchlist();
      setItems(list);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleRemove(item: WatchlistItem) {
    try {
      await removeWatchlist(item.termCode, item.sisSectionId);
      setItems((prev) => prev.filter((i) => i.id !== item.id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Remove failed');
    }
  }

  return (
    <div className="watchlist-page">
      <h1>Watchlist</h1>
      {alert && (
        <div className="watchlist-alert">
          A seat opened for section {alert.sisSectionId} (term {alert.termCode}).{' '}
          <button type="button" onClick={() => setAlert(null)}>Dismiss</button>
        </div>
      )}
      {error && <p className="watchlist-error">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <p className="watchlist-empty">No items. Add sections from Search.</p>
      ) : (
        <ul className="watchlist-list">
          {items.map((item) => (
            <li key={item.id} className="watchlist-item">
              <span>{item.termCode} · {item.sisSectionId}</span>
              <button type="button" onClick={() => handleRemove(item)} className="watchlist-remove">Remove</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
