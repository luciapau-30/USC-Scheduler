/** USC API search response shape */
export interface ScheduleItem {
  dayCode?: string;
  days?: string | string[];
  startTime?: string | null;
  endTime?: string | null;
}

export interface SectionInstructor {
  firstName?: string;
  lastName?: string;
}

export interface Section {
  sisSectionId?: string;
  /** USC API field for section type (e.g. "Lecture", "Lecture/Lab", "Lab") */
  rnrMode?: string;
  /** USC API instructors array */
  instructors?: SectionInstructor[];
  location?: string;
  totalSeats?: number;
  registeredSeats?: number;
  isFull?: boolean;
  isCancelled?: boolean;
  schedule?: ScheduleItem[];
}

export interface Course {
  title?: string;
  /** USC API: "CSCI 380" — display name */
  fullCourseName?: string;
  name?: string;
  /** USC API: course number/title part only (e.g. "Intro to CS") */
  courseNumber?: string;
  /** USC API: subject prefix (e.g. "CSCI") — used for watchlist/swap search */
  prefix?: string;
  sections?: Section[];
}

export interface SearchResponse {
  courses?: Course[];
  /** Set when the backend could not reach USC API (timeout, etc.) */
  error?: string;
}

export function toMeetingDto(s: ScheduleItem): { dayCode: string; startTime: string | null; endTime: string | null } {
  return {
    dayCode: s.dayCode ?? (Array.isArray(s.days) ? '' : s.days ?? ''),
    startTime: s.startTime ?? null,
    endTime: s.endTime ?? null,
  };
}

/** Format an instructors array into a display string. */
export function formatInstructors(instructors?: SectionInstructor[]): string {
  if (!instructors?.length) return '';
  return instructors
    .map((i) => [i.firstName, i.lastName].filter(Boolean).join(' '))
    .filter(Boolean)
    .join(', ');
}
