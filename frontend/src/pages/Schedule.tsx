import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { checkConflict } from '../api/schedule';
import { useSchedule } from '../context/ScheduleContext';
import type { MeetingDto } from '../api/schedule';
import './Schedule.css';

type LocationState = {
  termCode: string;
  sisSectionId: string;
  course?: { title?: string };
  meetings: MeetingDto[];
};

export default function Schedule() {
  const { sections, addSection, removeSection, getAllMeetings } = useSchedule();
  const location = useLocation();
  const navigate = useNavigate();
  const [conflictError, setConflictError] = useState('');
  const [tbaWarning, setTbaWarning] = useState('');

  useEffect(() => {
    const state = location.state as LocationState | null;
    if (!state?.meetings?.length) return;

    navigate('/schedule', { replace: true, state: null });
    const candidate = state;
    const existingMeetings = getAllMeetings();
    checkConflict({
      existingMeetings,
      candidateMeetings: candidate.meetings,
    }).then((res) => {
      if (res.conflict) {
        setConflictError(res.message ?? 'Schedule conflict.');
        setTbaWarning(res.tbaInvolved ? 'Some times are TBA.' : '');
      } else {
        if (res.tbaInvolved) {
          setTbaWarning('Some times are TBA; conflict check may be incomplete.');
        } else {
          setTbaWarning('');
        }
        addSection({
          termCode: candidate.termCode,
          sisSectionId: candidate.sisSectionId,
          title: candidate.course?.title,
          meetings: candidate.meetings,
        });
        setConflictError('');
      }
    }).catch(() => {
      setConflictError('Could not check conflict.');
    });
  }, [location.state, addSection, getAllMeetings, navigate]);

  return (
    <div className="schedule-page">
      <h1>Schedule</h1>
      <p className="schedule-hint">Add sections from Search. Conflicts are checked when you add.</p>
      {conflictError && <p className="schedule-error">{conflictError}</p>}
      {tbaWarning && <p className="schedule-warn">{tbaWarning}</p>}
      {sections.length === 0 ? (
        <p className="schedule-empty">No sections. Use Search and “Add to schedule”.</p>
      ) : (
        <ul className="schedule-list">
          {sections.map((s) => (
            <li key={`${s.termCode}-${s.sisSectionId}`} className="schedule-item">
              <div>
                <strong>{s.title ?? s.sisSectionId}</strong> · {s.termCode}
                {s.meetings.length > 0 && (
                  <span className="schedule-times">
                    {' '}({s.meetings.map((m) => `${m.dayCode ?? '?'} ${m.startTime ?? 'TBA'}-${m.endTime ?? 'TBA'}`).join(', ')})
                  </span>
                )}
              </div>
              <button type="button" onClick={() => removeSection(s.termCode, s.sisSectionId)} className="schedule-remove">Remove</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
