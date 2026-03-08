import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { searchCourses } from '../api/courses';
import { addWatchlist } from '../api/watchlist';
import type { SearchResponse, Section, Course } from '../types/course';
import { toMeetingDto } from '../types/course';
import type { MeetingDto } from '../api/schedule';
import './Search.css';

export default function Search() {
  const [termCode, setTermCode] = useState('20263');
  const [q, setQ] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [data, setData] = useState<SearchResponse | null>(null);
  const navigate = useNavigate();

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    const cleanTerm = termCode.trim().split(/\s+/)[0] || termCode.trim();
    try {
      const raw = await searchCourses(cleanTerm, q.trim());
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
    const prefix = course.courseNumber ?? (course as { prefix?: string }).prefix ?? null;
    const sisSectionId = section.sisSectionId ?? '';
    setError('');
    try {
      await addWatchlist(termCode, sisSectionId, prefix ?? undefined);
      navigate('/watchlist');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Add failed');
    }
  }

  function handleAddToSchedule(course: Course, section: Section) {
    const meetings: MeetingDto[] = (section.schedule ?? [])
      .filter((s) => s.dayCode || (s as { days?: string }).days)
      .map((s) => toMeetingDto(s));
    navigate('/schedule', { state: { termCode, sisSectionId: section.sisSectionId, course, section, meetings } });
  }

  const courses = data?.courses ?? [];
  const apiError = data?.error;

  return (
    <div className="search-page">
      <h1>Course search</h1>
      {apiError && <p className="search-error">Course service: {apiError}</p>}
      <form onSubmit={handleSearch} className="search-form">
        <label className="search-label">
          Term code <span style={{ color: 'var(--muted)', fontWeight: 'normal' }}>(digits, e.g. 20263)</span>
          <input
            placeholder="20263"
            value={termCode}
            onChange={(e) => setTermCode(e.target.value)}
            required
          />
        </label>
        <label className="search-label">
          Course or subject <span style={{ color: 'var(--muted)', fontWeight: 'normal' }}>(e.g. MATH, CSCI)</span>
          <input
            placeholder="MATH"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </label>
        <button type="submit" disabled={loading}>{loading ? 'Searching…' : 'Search'}</button>
      </form>
      {error && <p className="search-error">{error}</p>}
      {!apiError && courses.length === 0 && data !== null && <p className="search-empty">No courses found. Try a different term or search.</p>}
      <div className="search-results">
        {courses.map((course, i) => (
          <div key={i} className="search-course">
            <h3>{course.title ?? course.courseNumber ?? 'Course'}</h3>
            {(course.sections ?? []).map((section, j) => (
              <div key={j} className="search-section">
                <span>Section {section.sisSectionId ?? j + 1}</span>
                <span className="search-section-meta">
                  {section.isFull ? 'Full' : 'Open'} · {section.registeredSeats ?? 0}/{section.totalSeats ?? '?'} seats
                </span>
                <div className="search-section-actions">
                  <button type="button" onClick={() => handleAddWatchlist(course, section)}>Add to watchlist</button>
                  <button type="button" onClick={() => handleAddToSchedule(course, section)} className="btn-secondary">Add to schedule</button>
                </div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
