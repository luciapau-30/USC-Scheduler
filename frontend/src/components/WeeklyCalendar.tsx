import './WeeklyCalendar.css';
import type { ScheduledSection } from '../context/ScheduleContext';
import { parseDayCode } from '../utils/dayCode';

const DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'];
const HOUR_HEIGHT = 60; // px per hour
const GRID_START = 7 * 60; // 7:00am in minutes
const GRID_END = 22 * 60;  // 10:00pm in minutes
const HOURS = Array.from({ length: GRID_END / 60 - GRID_START / 60 }, (_, i) => i + 7); // [7..21]

function timeToMinutes(time: string): number {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + (m || 0);
}

function formatHour(h: number): string {
  if (h === 12) return '12pm';
  return h < 12 ? `${h}am` : `${h - 12}pm`;
}

interface CalBlock {
  section: ScheduledSection;
  day: number;
  startMin: number;
  endMin: number;
}

interface LayoutBlock extends CalBlock {
  left: number;  // 0.0–1.0 fraction
  width: number; // 0.0–1.0 fraction
}

function computeLayout(blocks: CalBlock[]): LayoutBlock[] {
  const byDay: CalBlock[][] = [[], [], [], [], []];
  for (const b of blocks) {
    if (b.day >= 0 && b.day <= 4) byDay[b.day].push(b);
  }

  const result: LayoutBlock[] = [];
  for (const dayBlocks of byDay) {
    if (dayBlocks.length === 0) continue;
    const sorted = [...dayBlocks].sort((a, b) => a.startMin - b.startMin);

    // Greedy column assignment
    const columns: CalBlock[][] = [];
    for (const b of sorted) {
      let placed = false;
      for (const col of columns) {
        if (col[col.length - 1].endMin <= b.startMin) {
          col.push(b);
          placed = true;
          break;
        }
      }
      if (!placed) columns.push([b]);
    }

    const numCols = columns.length;
    columns.forEach((col, colIdx) => {
      for (const b of col) {
        result.push({ ...b, left: colIdx / numCols, width: 1 / numCols });
      }
    });
  }
  return result;
}

interface WeeklyCalendarProps {
  sections: ScheduledSection[];
  onRemove: (termCode: string, sisSectionId: string) => void;
  onEdit: (section: ScheduledSection) => void;
}

export default function WeeklyCalendar({ sections, onRemove, onEdit }: WeeklyCalendarProps) {
  const gridHeight = HOURS.length * HOUR_HEIGHT;

  const blocks: CalBlock[] = [];
  for (const s of sections) {
    for (const m of s.meetings) {
      if (!m.startTime || !m.endTime || !m.dayCode) continue;
      const startMin = timeToMinutes(m.startTime);
      const endMin = timeToMinutes(m.endTime);
      if (startMin < GRID_START || endMin > GRID_END || endMin <= startMin) continue;
      for (const day of parseDayCode(m.dayCode)) {
        blocks.push({ section: s, day, startMin, endMin });
      }
    }
  }

  const laid = computeLayout(blocks);

  return (
    <div className="weekly-calendar">
      <div className="cal-header">
        <div className="cal-time-gutter" />
        {DAYS.map((d) => (
          <div key={d} className="cal-day-label">{d}</div>
        ))}
      </div>
      <div className="cal-grid">
        <div className="cal-time-gutter">
          {HOURS.map((h) => (
            <div key={h} className="cal-hour-label" style={{ height: HOUR_HEIGHT }}>
              {formatHour(h)}
            </div>
          ))}
        </div>
        {DAYS.map((_, dayIdx) => (
          <div key={dayIdx} className="cal-day-col" style={{ height: gridHeight }}>
            {HOURS.map((h) => (
              <div key={h} className="cal-hour-line" style={{ top: (h - 7) * HOUR_HEIGHT }} />
            ))}
            {laid
              .filter((b) => b.day === dayIdx)
              .map((b, i) => {
                const top = ((b.startMin - GRID_START) / 60) * HOUR_HEIGHT;
                const height = Math.max(((b.endMin - b.startMin) / 60) * HOUR_HEIGHT, 20);
                return (
                  <div
                    key={i}
                    className="cal-block"
                    style={{
                      top,
                      height,
                      left: `calc(${b.left * 100}% + 1px)`,
                      width: `calc(${b.width * 100}% - 2px)`,
                      backgroundColor: b.section.color,
                    }}
                  >
                    <div className="cal-block-title">
                      {b.section.title ?? b.section.sisSectionId}
                    </div>
                    <div className="cal-block-actions">
                      <button
                        type="button"
                        className="cal-block-btn"
                        onClick={() => onEdit(b.section)}
                        title="Edit section"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        className="cal-block-btn cal-block-btn--remove"
                        onClick={() => onRemove(b.section.termCode, b.section.sisSectionId)}
                        title="Remove section"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                );
              })}
          </div>
        ))}
      </div>
    </div>
  );
}
