import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { listWatchlist, removeWatchlist, type WatchlistItem } from '../api/watchlist';
import './Watchlist.css';

export default function Watchlist() {
  const [items, setItems] = useState<WatchlistItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

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

  function termLabel(termCode: string): string {
    const map: Record<string, string> = {
      '20263': 'Fall 2026',
      '20261': 'Spring 2026',
      '20256': 'Summer 2026',
      '20253': 'Fall 2025',
      '20251': 'Spring 2025',
    };
    return map[termCode] ?? termCode;
  }

  return (
    <div className="watchlist-page">
      <h1>Watchlist</h1>
      <p className="watchlist-hint">
        You'll receive a notification when a seat opens in a watched section.
      </p>
      {error && <p className="watchlist-error">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <p className="watchlist-empty">No items. Add sections from Search.</p>
      ) : (
        <ul className="watchlist-list">
          {items.map((item) => (
            <li key={item.id} className="watchlist-item">
              <div className="watchlist-item-info">
                <span className="watchlist-section-id">{item.sisSectionId}</span>
                <span className="watchlist-term">{termLabel(item.termCode)}</span>
                {item.coursePrefix && (
                  <button
                    type="button"
                    className="watchlist-search-link"
                    onClick={() => navigate(`/search?q=${encodeURIComponent(item.coursePrefix ?? '')}`)}
                  >
                    View in search →
                  </button>
                )}
              </div>
              <button
                type="button"
                onClick={() => handleRemove(item)}
                className="watchlist-remove"
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
