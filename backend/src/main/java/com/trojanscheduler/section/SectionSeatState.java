package com.trojanscheduler.section;

/**
 * Parsed seat state from a section payload (USC or similar).
 * Used to update section_snapshot and detect "seat opened" transitions.
 */
public record SectionSeatState(
		String status,
		Integer capacity,
		Integer enrolled,
		Integer openSeats
) {
	public static final String STATUS_OPEN = "Open";
	public static final String STATUS_CLOSED = "Closed";
	public static final String STATUS_UNKNOWN = "UNKNOWN";

	/** True if there is at least one open seat. */
	public boolean hasOpenSeats() {
		return openSeats != null && openSeats > 0;
	}
}
