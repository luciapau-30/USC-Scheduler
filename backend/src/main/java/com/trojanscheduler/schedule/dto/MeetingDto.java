package com.trojanscheduler.schedule.dto;

/**
 * One meeting slot (e.g. "MW" 14:00–15:20).
 * Matches USC API: schedule[].dayCode, startTime, endTime.
 * Null startTime/endTime = TBA (time to be announced).
 */
public record MeetingDto(
		/** Day pattern: "MW", "TTh", "TR", "F", etc. */
		String dayCode,
		/** "HH:mm" or "h:mm a"; null = TBA */
		String startTime,
		/** "HH:mm" or "h:mm a"; null = TBA */
		String endTime
) {}
