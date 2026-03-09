import './SeatBadge.css';

interface SeatBadgeProps {
  isFull?: boolean;
  isCancelled?: boolean;
  registered?: number;
  total?: number;
}

export default function SeatBadge({ isFull, isCancelled, registered, total }: SeatBadgeProps) {
  if (isCancelled) {
    return <span className="seat-badge seat-badge--cancelled">Cancelled</span>;
  }
  if (isFull) {
    return <span className="seat-badge seat-badge--full">Full {registered ?? 0}/{total ?? '?'}</span>;
  }
  return <span className="seat-badge seat-badge--open">Open {registered ?? 0}/{total ?? '?'}</span>;
}
