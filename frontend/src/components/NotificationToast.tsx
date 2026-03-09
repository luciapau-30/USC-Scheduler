import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useNotifications, type NotificationPayload } from '../hooks/useNotifications';
import './NotificationToast.css';

interface Toast extends NotificationPayload {
  id: number;
}

let toastId = 0;

export default function NotificationToast() {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const navigate = useNavigate();

  useNotifications((payload) => {
    if (payload.eventType === 'seat_opened') {
      const id = ++toastId;
      setToasts((prev) => [...prev, { ...payload, id }]);
      // Auto-dismiss after 10 seconds
      setTimeout(() => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
      }, 10000);
    }
  });

  function dismiss(id: number) {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }

  if (toasts.length === 0) return null;

  return (
    <div className="toast-container">
      {toasts.map((t) => (
        <div key={t.id} className="toast">
          <div className="toast-body">
            <span className="toast-dot" />
            <span>
              Seat opened for section <strong>{t.sisSectionId}</strong> (term {t.termCode})
            </span>
          </div>
          <div className="toast-actions">
            <button
              type="button"
              className="toast-btn"
              onClick={() => {
                navigate('/watchlist');
                dismiss(t.id);
              }}
            >
              View watchlist
            </button>
            <button
              type="button"
              className="toast-dismiss"
              onClick={() => dismiss(t.id)}
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
