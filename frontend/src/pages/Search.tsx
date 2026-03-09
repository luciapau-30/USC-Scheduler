import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchCourses } from '../api/courses';
import { addWatchlist } from '../api/watchlist';
import type { SearchResponse, Section, Course } from '../types/course';
import { toMeetingDto, formatInstructors } from '../types/course';
import type { MeetingDto } from '../api/schedule';
import { useSchedule } from '../context/ScheduleContext';
import { parseDayCode } from '../utils/dayCode';
import SeatBadge from '../components/SeatBadge';
import './Search.css';

const USC_TERMS = [
  { termCode: '20263', label: 'Fall 2026' },
  { termCode: '20261', label: 'Spring 2026' },
  { termCode: '20256', label: 'Summer 2026' },
  { termCode: '20253', label: 'Fall 2025' },
  { termCode: '20251', label: 'Spring 2025' },
];

function formatTime(time: string | null | undefined): string {
  if (!time) return 'TBA';
  const [h, m] = time.split(':').map(Number);
  const ampm = h >= 12 ? 'pm' : 'am';
  const h12 = h === 0 ? 12 : h > 12 ? h - 12 : h;
  return `${h12}:${String(m).padStart(2, '0')}${ampm}`;
}

function formatSchedule(section: Section): string {
  const items = (section.schedule ?? []).filter((s) => s.dayCode || s.days);
  if (items.length === 0) return 'TBA';
  return items
    .map((s) => `${s.dayCode ?? s.days ?? '?'} ${formatTime(s.startTime)}–${formatTime(s.endTime)}`)
    .join(', ');
}

function timeToMin(t: string): number {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + (m || 0);
}

/** Client-side conflict preview — no API call, just day/time overlap check. */
function hasConflictPreview(candidateMeetings: MeetingDto[], existingMeetings: MeetingDto[]): boolean {
  for (const cand of candidateMeetings) {
    if (!cand.startTime || !cand.endTime || !cand.dayCode) continue;
    const candDays = parseDayCode(cand.dayCode);
    const candStart = timeToMin(cand.startTime);
    const candEnd = timeToMin(cand.endTime);
    for (const ex of existingMeetings) {
      if (!ex.startTime || !ex.endTime || !ex.dayCode) continue;
      const exDays = parseDayCode(ex.dayCode);
      const sharedDay = candDays.some((d) => exDays.includes(d));
      if (sharedDay && candStart < timeToMin(ex.endTime) && candEnd > timeToMin(ex.startTime)) return true;
    }
  }
  return false;
}

export default function Search() {
  const [termCode, setTermCode] = useState('20263');
  const [customTerm, setCustomTerm] = useState('');
  const [useCustomTerm, setUseCustomTerm] = useState(false);
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [data, setData] = useState<SearchResponse | null>(null);
  const [hideCancelled, setHideCancelled] = useState(true);
  const navigate = useNavigate();
  const { getAllMeetings } = useSchedule();

  const effectiveTerm = useCustomTerm ? customTerm.trim() : termCode;

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    const program = q.trim();
    if (!program) {
      setError('Enter a subject or course code (e.g. CSCI, MATH, ALI).');
      return;
    }
    const term = effectiveTerm.split(/\s+/)[0] || effectiveTerm;
    if (!term) {
      setError('Select or enter a term code.');
      return;
    }
    setLoading(true);
    try {
      const raw = await searchCourses(term, program);
      const parsed: SearchResponse = JSON.parse(raw);
      setData(parsed);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Search failed');
      setData(null);
    } finally {
      setLoading(false);
    }
  }

  async function handleAddWatchlist(course: Course, section: Section) {
    const prefix = course.prefix ?? course.courseNumber ?? null;
    setError('');
    try {
      await addWatchlist(effectiveTerm, section.sisSectionId ?? '', prefix ?? undefined);
      navigate('/watchlist');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Add to watchlist failed');
    }
  }

  function handleAddToSchedule(course: Course, section: Section) {
    const meetings: MeetingDto[] = (section.schedule ?? [])
      .filter((s) => s.dayCode || s.days)
      .map(toMeetingDto);
    navigate('/schedule', {
      state: { termCode: effectiveTerm, sisSectionId: section.sisSectionId, course, section, meetings },
    });
  }

  const existingMeetings = getAllMeetings();
  const courses = data?.courses ?? [];
  const apiError = data?.error;

  return (
    <div className="search-page">
      <h1>Course search</h1>
      {apiError && <p className="search-error">Course service: {apiError}</p>}

      <form onSubmit={handleSearch} className="search-form">
        <label className="search-label">
          Term
          <select
            value={useCustomTerm ? '__custom__' : termCode}
            onChange={(e) => {
              if (e.target.value === '__custom__') {
                setUseCustomTerm(true);
              } else {
                setUseCustomTerm(false);
                setTermCode(e.target.value);
              }
            }}
          >
            {USC_TERMS.map((t) => (
              <option key={t.termCode} value={t.termCode}>
                {t.label} ({t.termCode})
              </option>
            ))}
            <option value="__custom__">Other…</option>
          </select>
        </label>

        {useCustomTerm && (
          <label className="search-label">
            Term code
            <input
              placeholder="e.g. 20263"
              value={customTerm}
              onChange={(e) => setCustomTerm(e.target.value)}
              required
            />
          </label>
        )}

        <label className="search-label">
          Course or subject
          <input
            placeholder="e.g. CSCI, MATH, ALI"
            value={q}
            onChange={(e) => setQ(e.target.value)}
            required
            minLength={1}
          />
        </label>

        <button type="submit" disabled={loading}>{loading ? 'Searching…' : 'Search'}</button>
      </form>

      {error && <p className="search-error">{error}</p>}

      {data !== null && !apiError && (
        <div className="search-toolbar">
          <span className="search-count">
            {courses.length === 0
              ? 'No courses found'
              : `${courses.length} course${courses.length === 1 ? '' : 's'}`}
          </span>
          <label className="search-toggle">
            <input
              type="checkbox"
              checked={hideCancelled}
              onChange={(e) => setHideCancelled(e.target.checked)}
            />
            Hide cancelled
          </label>
        </div>
      )}

      {!apiError && courses.length === 0 && data !== null && (
        <p className="search-empty">
          No courses found. Try a subject code (e.g. CSCI, MATH) or a full course number.
        </p>
      )}

      <div className="search-results">
        {courses.map((course, i) => {
          const sections = (course.sections ?? []).filter(
            (s) => !hideCancelled || !s.isCancelled
          );
          if (sections.length === 0 && hideCancelled && (course.sections ?? []).length > 0) return null;
          return (
            <div key={i} className="search-course">
              <h3>{course.fullCourseName ?? course.title ?? course.name ?? course.courseNumber ?? 'Course'}</h3>
              {sections.map((section, j) => {
                const candidateMeetings: MeetingDto[] = (section.schedule ?? [])
                  .filter((s) => s.dayCode || s.days)
                  .map(toMeetingDto);
                const conflicts = hasConflictPreview(candidateMeetings, existingMeetings);

                return (
                  <div
                    key={j}
                    className={`search-section${conflicts ? ' search-section--conflict-preview' : ''}`}
                  >
                    <div className="search-section-left">
                      <span className="search-section-id">§ {section.sisSectionId ?? j + 1}</span>
                      {section.rnrMode && (
                        <span className="section-type-badge">{section.rnrMode}</span>
                      )}
                      <span className="section-schedule-text">{formatSchedule(section)}</span>
                      {section.instructors && section.instructors.length > 0 && (
                        <span className="search-section-meta">{formatInstructors(section.instructors)}</span>
                      )}
                      <SeatBadge
                        isFull={section.isFull}
                        isCancelled={section.isCancelled}
                        registered={section.registeredSeats}
                        total={section.totalSeats}
                      />
                      {conflicts && (
                        <span className="search-conflict-hint">⚠ Conflicts with your schedule</span>
                      )}
                    </div>
                    <div className="search-section-actions">
                      <button
                        type="button"
                        onClick={() => handleAddWatchlist(course, section)}
                        disabled={!!section.isCancelled}
                      >
                        + Watchlist
                      </button>
                      <button
                        type="button"
                        onClick={() => handleAddToSchedule(course, section)}
                        className="btn-secondary"
                        disabled={!!section.isCancelled}
                      >
                        + Schedule
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
