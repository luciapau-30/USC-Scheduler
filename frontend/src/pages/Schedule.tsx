import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { checkConflict } from '../api/schedule';
import { useSchedule } from '../context/ScheduleContext';
import type { ScheduledSection } from '../context/ScheduleContext';
import type { MeetingDto } from '../api/schedule';
import { formatInstructors } from '../types/course';
import type { SectionInstructor } from '../types/course';
import WeeklyCalendar from '../components/WeeklyCalendar';
import ScheduleSidebar from '../components/ScheduleSidebar';
import SwapSectionModal from '../components/SwapSectionModal';
import './Schedule.css';

type LocationState = {
  termCode: string;
  sisSectionId: string;
  course?: { title?: string; fullCourseName?: string; prefix?: string; courseNumber?: string };
  section?: { instructors?: SectionInstructor[]; rnrMode?: string };
  meetings: MeetingDto[];
};

export default function Schedule() {
  const { sections, addSection, removeSection, getAllMeetings } = useSchedule();
  const location = useLocation();
  const navigate = useNavigate();
  const [conflictError, setConflictError] = useState('');
  const [tbaWarning, setTbaWarning] = useState('');
  const [editingSection, setEditingSection] = useState<ScheduledSection | null>(null);

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
          title: candidate.course?.fullCourseName ?? candidate.course?.title,
          courseNumber: candidate.course?.prefix ?? candidate.course?.courseNumber,
          instructor: formatInstructors(candidate.section?.instructors),
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
      <div className="schedule-header">
        <h1>Schedule</h1>
        <p className="schedule-hint">Add sections from Search. Conflicts are checked automatically.</p>
      </div>

      {conflictError && <p className="schedule-error">{conflictError}</p>}
      {tbaWarning && <p className="schedule-warn">{tbaWarning}</p>}

      <div className="schedule-layout">
        <div className="schedule-calendar">
          <WeeklyCalendar
            sections={sections}
            onRemove={removeSection}
            onEdit={setEditingSection}
          />
        </div>
        <div className="schedule-sidebar-col">
          <ScheduleSidebar
            sections={sections}
            onRemove={removeSection}
            onEdit={setEditingSection}
          />
        </div>
      </div>

      {editingSection && (
        <SwapSectionModal
          section={editingSection}
          onClose={() => setEditingSection(null)}
        />
      )}
    </div>
  );
}
