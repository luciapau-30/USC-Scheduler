import './ScheduleSidebar.css';
import type { ScheduledSection } from '../context/ScheduleContext';

function formatTime(time: string | null): string {
  if (!time) return 'TBA';
  const [h, m] = time.split(':').map(Number);
  const ampm = h >= 12 ? 'pm' : 'am';
  const h12 = h === 0 ? 12 : h > 12 ? h - 12 : h;
  return `${h12}:${String(m).padStart(2, '0')}${ampm}`;
}

function formatMeeting(dayCode: string, startTime: string | null, endTime: string | null): string {
  if (!startTime || !endTime) return `${dayCode || '?'} TBA`;
  return `${dayCode} ${formatTime(startTime)}–${formatTime(endTime)}`;
}

interface ScheduleSidebarProps {
  sections: ScheduledSection[];
  onRemove: (termCode: string, sisSectionId: string) => void;
  onEdit: (section: ScheduledSection) => void;
}

export default function ScheduleSidebar({ sections, onRemove, onEdit }: ScheduleSidebarProps) {
  if (sections.length === 0) {
    return (
      <div className="sidebar">
        <p className="sidebar-empty">No sections added yet. Use Search to find and add sections.</p>
      </div>
    );
  }

  return (
    <div className="sidebar">
      <ul className="sidebar-list">
        {sections.map((s) => {
          const isTba = s.meetings.every((m) => !m.startTime || !m.endTime);
          return (
            <li key={`${s.termCode}-${s.sisSectionId}`} className="sidebar-item">
              <div
                className="sidebar-color-dot"
                style={{ backgroundColor: s.color }}
              />
              <div className="sidebar-info">
                <div className="sidebar-title">{s.title ?? s.sisSectionId}</div>
                {s.instructor && (
                  <div className="sidebar-meta">{s.instructor}</div>
                )}
                <div className="sidebar-meta">
                  {isTba
                    ? 'TBA'
                    : s.meetings
                        .filter((m) => m.dayCode)
                        .map((m, i) => (
                          <span key={i}>{formatMeeting(m.dayCode, m.startTime, m.endTime)}</span>
                        ))}
                </div>
                {isTba && <span className="sidebar-tba-badge">TBA</span>}
              </div>
              <div className="sidebar-actions">
                <button
                  type="button"
                  className="sidebar-btn"
                  onClick={() => onEdit(s)}
                >
                  Edit
                </button>
                <button
                  type="button"
                  className="sidebar-btn sidebar-btn--remove"
                  onClick={() => onRemove(s.termCode, s.sisSectionId)}
                >
                  Remove
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
