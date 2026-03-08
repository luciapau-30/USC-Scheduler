package com.trojanscheduler.schedule.dto;

/**
 * Result of conflict check. When tbaInvolved is true, conflict may be unknown (TBA meetings can't be compared).
 */
public record CheckConflictResponse(
		/** True if at least one candidate meeting overlaps an existing meeting (same day, overlapping time). */
		boolean conflict,
		/** True if any meeting has null/empty startTime or endTime (TBA). UI should warn that conflict is uncertain. */
		boolean tbaInvolved,
		/** Human-readable summary; null if no conflict and no TBA. */
		String message
) {
	public static CheckConflictResponse noConflict() {
		return new CheckConflictResponse(false, false, null);
	}

	public static CheckConflictResponse conflict(String message) {
		return new CheckConflictResponse(true, false, message);
	}

	public static CheckConflictResponse conflictWithTba(String message) {
		return new CheckConflictResponse(true, true, message);
	}

	public static CheckConflictResponse tbaOnly() {
		return new CheckConflictResponse(false, true, "Schedule includes TBA times; conflict cannot be fully determined.");
	}
}
