import { useEffect, useState } from 'react';
import { searchCourses } from '../api/courses';
import { checkConflict } from '../api/schedule';
import { useSchedule } from '../context/ScheduleContext';
import type { ScheduledSection } from '../context/ScheduleContext';
import type { SearchResponse, Section, Course } from '../types/course';
import { toMeetingDto, formatInstructors } from '../types/course';
import SeatBadge from './SeatBadge';
import './SwapSectionModal.css';

interface SwapSectionModalProps {
  section: ScheduledSection;
  onClose: () => void;
}

export default function SwapSectionModal({ section, onClose }: SwapSectionModalProps) {
  const { sections, replaceSection } = useSchedule();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [matchingCourse, setMatchingCourse] = useState<Course | null>(null);
  const [swapping, setSwapping] = useState<string | null>(null);
  const [conflictMsg, setConflictMsg] = useState('');

  useEffect(() => {
    if (!section.courseNumber) {
      setError('Cannot swap: course prefix not available for this section.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    searchCourses(section.termCode, section.courseNumber)
      .then((raw) => {
        const parsed: SearchResponse = JSON.parse(raw);
        if (parsed.error) {
          setError(`Course service error: ${parsed.error}`);
          return;
        }
        // Filter to the specific course matching this section's title
        const courses = parsed.courses ?? [];
        const found = courses.find(
          (c) =>
            (c.fullCourseName ?? c.title ?? c.name) === section.title ||
            courses.length === 1
        ) ?? courses[0] ?? null;
        setMatchingCourse(found);
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Search failed'))
      .finally(() => setLoading(false));
  }, [section.courseNumber, section.termCode, section.title]);

  async function handleSwap(course: Course, candidate: Section) {
    if (candidate.sisSectionId === section.sisSectionId) return;
    setConflictMsg('');
    setSwapping(candidate.sisSectionId ?? null);
    try {
      const candidateMeetings = (candidate.schedule ?? [])
        .filter((s) => s.dayCode || s.days)
        .map(toMeetingDto);

      // Existing meetings excluding the section being replaced
      const existingMeetings = sections
        .filter((s) => !(s.termCode === section.termCode && s.sisSectionId === section.sisSectionId))
        .flatMap((s) => s.meetings);

      const res = await checkConflict({ existingMeetings, candidateMeetings });
      if (res.conflict) {
        setConflictMsg(res.message ?? 'Schedule conflict with another section.');
        return;
      }

      replaceSection(section.termCode, section.sisSectionId, {
        termCode: section.termCode,
        sisSectionId: candidate.sisSectionId ?? '',
        title: course.fullCourseName ?? course.title ?? course.name,
        courseNumber: course.prefix ?? course.courseNumber,
        instructor: formatInstructors(candidate.instructors),
        meetings: candidateMeetings,
      });
      onClose();
    } catch (err) {
      setConflictMsg(err instanceof Error ? err.message : 'Swap failed');
    } finally {
      setSwapping(null);
    }
  }

  const sectionList = matchingCourse?.sections ?? [];

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h2>Swap section</h2>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>
        <p className="modal-subtitle">
          Replacing: <strong>{section.title ?? section.sisSectionId}</strong>
        </p>

        {conflictMsg && <p className="modal-error">{conflictMsg}</p>}
        {loading && <p className="modal-loading">Searching…</p>}
        {error && <p className="modal-error">{error}</p>}

        {!loading && !error && sectionList.length === 0 && (
          <p className="modal-empty">No sections found for this course.</p>
        )}

        <div className="modal-sections">
          {matchingCourse && sectionList.map((sec) => {
            const isCurrent = sec.sisSectionId === section.sisSectionId;
            const instructorName = formatInstructors(sec.instructors);
            return (
              <div
                key={sec.sisSectionId}
                className={`modal-section-row${isCurrent ? ' modal-section-row--current' : ''}`}
              >
                <div className="modal-section-info">
                  <span className="modal-section-id">Section {sec.sisSectionId}</span>
                  {sec.rnrMode && (
                    <span className="modal-section-type">{sec.rnrMode}</span>
                  )}
                  {instructorName && (
                    <span className="modal-section-meta">{instructorName}</span>
                  )}
                  <span className="modal-section-meta">
                    {(sec.schedule ?? []).map((m, i) => (
                      <span key={i}>
                        {m.dayCode ?? '?'} {m.startTime ?? 'TBA'}{m.endTime ? `–${m.endTime}` : ''}
                      </span>
                    ))}
                  </span>
                  <SeatBadge
                    isFull={sec.isFull}
                    isCancelled={sec.isCancelled}
                    registered={sec.registeredSeats}
                    total={sec.totalSeats}
                  />
                </div>
                {isCurrent ? (
                  <span className="modal-current-label">Current</span>
                ) : (
                  <button
                    type="button"
                    className="modal-swap-btn"
                    disabled={swapping !== null}
                    onClick={() => handleSwap(matchingCourse, sec)}
                  >
                    {swapping === sec.sisSectionId ? 'Checking…' : 'Swap'}
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
