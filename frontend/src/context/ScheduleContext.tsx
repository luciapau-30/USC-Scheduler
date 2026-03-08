import { createContext, useCallback, useContext, useState } from 'react';
import type { MeetingDto } from '../api/schedule';

export interface ScheduledSection {
  termCode: string;
  sisSectionId: string;
  title?: string;
  meetings: MeetingDto[];
}

type ScheduleContextValue = {
  sections: ScheduledSection[];
  addSection: (section: ScheduledSection) => void;
  removeSection: (termCode: string, sisSectionId: string) => void;
  getAllMeetings: () => MeetingDto[];
};

const ScheduleContext = createContext<ScheduleContextValue | null>(null);

const STORAGE_KEY = 'trojanscheduler-schedule';

function loadFromStorage(): ScheduledSection[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // ignore
  }
  return [];
}

function saveToStorage(sections: ScheduledSection[]) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sections));
  } catch {
    // ignore
  }
}

export function ScheduleProvider({ children }: { children: React.ReactNode }) {
  const [sections, setSections] = useState<ScheduledSection[]>(loadFromStorage);

  const addSection = useCallback((section: ScheduledSection) => {
    setSections((prev) => {
      const next = [...prev, section];
      saveToStorage(next);
      return next;
    });
  }, []);

  const removeSection = useCallback((termCode: string, sisSectionId: string) => {
    setSections((prev) => {
      const next = prev.filter((s) => !(s.termCode === termCode && s.sisSectionId === sisSectionId));
      saveToStorage(next);
      return next;
    });
  }, []);

  const getAllMeetings = useCallback(() => {
    return sections.flatMap((s) => s.meetings);
  }, [sections]);

  return (
    <ScheduleContext.Provider value={{ sections, addSection, removeSection, getAllMeetings }}>
      {children}
    </ScheduleContext.Provider>
  );
}

export function useSchedule() {
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useSchedule must be used within ScheduleProvider');
  return ctx;
}
