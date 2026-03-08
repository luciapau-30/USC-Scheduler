import { api } from './client';

export interface MeetingDto {
  dayCode: string;
  startTime: string | null;
  endTime: string | null;
}

export interface CheckConflictRequest {
  existingMeetings: MeetingDto[];
  candidateMeetings: MeetingDto[];
}

export interface CheckConflictResponse {
  conflict: boolean;
  tbaInvolved: boolean;
  message: string | null;
}

export async function checkConflict(req: CheckConflictRequest): Promise<CheckConflictResponse> {
  const res = await api('/api/schedule/check-conflict', {
    method: 'POST',
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(await res.text().then((t) => t || res.statusText));
  return res.json();
}
