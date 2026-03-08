package com.trojanscheduler.schedule;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.trojanscheduler.schedule.dto.CheckConflictResponse;
import com.trojanscheduler.schedule.dto.MeetingDto;

/**
 * Detects schedule conflicts: overlapping meetings on the same day.
 * Handles USC-style dayCode (MW, TTh/TR, F) and TBA (null times).
 */
@Service
public class ConflictDetectionService {

	private static final Pattern TIME_24 = Pattern.compile("^(?i)(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(am|pm)?$");

	/**
	 * Check if adding candidate meetings would conflict with existing meetings.
	 * Same day + overlapping time range = conflict. Any null start/end = TBA, response.tbaInvolved set.
	 */
	public CheckConflictResponse checkConflict(List<MeetingDto> existingMeetings, List<MeetingDto> candidateMeetings) {
		if (candidateMeetings == null || candidateMeetings.isEmpty()) {
			return CheckConflictResponse.noConflict();
		}
		boolean tbaInvolved = false;
		List<ParsedMeeting> existing = new ArrayList<>();
		List<MeetingDto> existingList = existingMeetings != null ? existingMeetings : List.<MeetingDto>of();
		for (MeetingDto dto : existingList) {
			ParsedMeeting p = parse(dto);
			if (p.tba) tbaInvolved = true;
			existing.add(p);
		}
		for (MeetingDto dto : candidateMeetings) {
			ParsedMeeting candidate = parse(dto);
			if (candidate.tba) tbaInvolved = true;
			for (ParsedMeeting ex : existing) {
				if (overlaps(ex, candidate)) {
					String msg = "Schedule conflict: overlapping time on same day.";
					return tbaInvolved ? CheckConflictResponse.conflictWithTba(msg) : CheckConflictResponse.conflict(msg);
				}
			}
		}
		if (tbaInvolved) return CheckConflictResponse.tbaOnly();
		return CheckConflictResponse.noConflict();
	}

	private static ParsedMeeting parse(MeetingDto dto) {
		BitSet days = parseDayCode(dto.dayCode() != null ? dto.dayCode() : "");
		Integer start = parseTime(dto.startTime());
		Integer end = parseTime(dto.endTime());
		boolean tba = (dto.startTime() == null || dto.startTime().isBlank()) || (dto.endTime() == null || dto.endTime().isBlank());
		return new ParsedMeeting(days, start, end, tba);
	}

	/** Day bits: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri. USC uses M T W Th F. */
	private static BitSet parseDayCode(String dayCode) {
		BitSet set = new BitSet(7);
		if (dayCode == null) return set;
		// Normalize "Th" (USC Thursday) to the single-char placeholder 'R' before iterating.
		String s = dayCode.toUpperCase().replace("TH", "R"); // TTh → TR, Th → R
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int day = switch (c) {
				case 'M' -> 1;
				case 'T' -> 2;
				case 'W' -> 3;
				case 'R' -> 4; // Thursday (from "Th" replacement)
				case 'F' -> 5;
				default -> -1;
			};
			if (day >= 0) set.set(day);
		}
		return set;
	}

	/** Minutes from midnight (0–1439). Returns null if TBA or unparseable. */
	private static Integer parseTime(String time) {
		if (time == null || time.isBlank()) return null;
		String t = time.trim();
		var m = TIME_24.matcher(t);
		if (m.matches()) {
			int h = Integer.parseInt(m.group(1));
			int min = Integer.parseInt(m.group(2));
			String ampm = m.group(3);
			if (ampm != null) {
				if ("pm".equalsIgnoreCase(ampm) && h != 12) h += 12;
				else if ("am".equalsIgnoreCase(ampm) && h == 12) h = 0;
			}
			return Math.min(1439, Math.max(0, h * 60 + min));
		}
		return null;
	}

	private static boolean overlaps(ParsedMeeting a, ParsedMeeting b) {
		if (!a.days.intersects(b.days)) return false;
		if (a.start == null || a.end == null || b.start == null || b.end == null) return false;
		return a.start < b.end && b.start < a.end;
	}

	private record ParsedMeeting(BitSet days, Integer start, Integer end, boolean tba) {}
}
