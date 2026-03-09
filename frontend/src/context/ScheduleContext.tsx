import { createContext, useCallback, useContext, useState } from 'react';
import type { MeetingDto } from '../api/schedule';

const COLOR_PALETTE = [
  '#58a6ff', '#3fb950', '#d29922', '#f0883e',
  '#bc8cff', '#ff7b72', '#39d353', '#79c0ff',
];

export interface ScheduledSection {
  termCode: string;
  sisSectionId: string;
  title?: string;
  courseNumber?: string;
  instructor?: string;
  color: string;
  meetings: MeetingDto[];
}

type ScheduleContextValue = {
  sections: ScheduledSection[];
  addSection: (section: Omit<ScheduledSection, 'color'>) => void;
  removeSection: (termCode: string, sisSectionId: string) => void;
  replaceSection: (termCode: string, oldSisSectionId: string, newSection: Omit<ScheduledSection, 'color'>) => void;
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

  const addSection = useCallback((section: Omit<ScheduledSection, 'color'>) => {
    setSections((prev) => {
      const color = COLOR_PALETTE[prev.length % COLOR_PALETTE.length];
      const next = [...prev, { ...section, color }];
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

  const replaceSection = useCallback((termCode: string, oldSisSectionId: string, newSection: Omit<ScheduledSection, 'color'>) => {
    setSections((prev) => {
      const oldIdx = prev.findIndex((s) => s.termCode === termCode && s.sisSectionId === oldSisSectionId);
      if (oldIdx === -1) return prev;
      const color = prev[oldIdx].color;
      const next = [...prev];
      next[oldIdx] = { ...newSection, color };
      saveToStorage(next);
      return next;
    });
  }, []);

  const getAllMeetings = useCallback(() => {
    return sections.flatMap((s) => s.meetings);
  }, [sections]);

  return (
    <ScheduleContext.Provider value={{ sections, addSection, removeSection, replaceSection, getAllMeetings }}>
      {children}
    </ScheduleContext.Provider>
  );
}

export function useSchedule() {
  const ctx = useContext(ScheduleContext);
  if (!ctx) throw new Error('useSchedule must be used within ScheduleProvider');
  return ctx;
}
