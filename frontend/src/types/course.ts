/** USC API search response shape */
export interface ScheduleItem {
  dayCode?: string;
  days?: string;
  startTime?: string | null;
  endTime?: string | null;
}

export interface Section {
  sisSectionId?: string;
  totalSeats?: number;
  registeredSeats?: number;
  isFull?: boolean;
  isCancelled?: boolean;
  schedule?: ScheduleItem[];
  [key: string]: unknown;
}

export interface Course {
  title?: string;
  courseNumber?: string;
  sections?: Section[];
  [key: string]: unknown;
}

export interface SearchResponse {
  courses?: Course[];
  /** Set when the backend could not reach USC API (timeout, etc.) */
  error?: string;
}

export function toMeetingDto(s: ScheduleItem): { dayCode: string; startTime: string | null; endTime: string | null } {
  return {
    dayCode: s.dayCode ?? s.days ?? '',
    startTime: s.startTime ?? null,
    endTime: s.endTime ?? null,
  };
}
