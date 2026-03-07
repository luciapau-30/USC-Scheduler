package com.trojanscheduler.section;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses section JSON (USC or similar) into seat state for snapshot and "seat opened" detection.
 * Tries common key names; override or extend when the real API shape is known.
 */
@Service
public class SeatStateService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Parse section payload JSON into status, capacity, enrolled, openSeats.
	 * Tries: status, capacity, enrolled, open_seats, available, seats_available; nested under "section" or root.
	 */
	public SectionSeatState parseSectionPayload(String rawJson) {
		if (rawJson == null || rawJson.isBlank()) {
			return new SectionSeatState(SectionSeatState.STATUS_UNKNOWN, null, null, null);
		}
		try {
			JsonNode root = objectMapper.readTree(rawJson);
			// Some APIs wrap the section in a "section" or "data" object
			JsonNode node = root.has("section") ? root.get("section") : root.has("data") ? root.get("data") : root;
			if (node == null) node = root;

			String status = textOrNull(node, "status", "registration_status", "enrollmentStatus");
			if (status != null) status = normalizeStatus(status);

			Integer capacity = intOrNull(node, "capacity", "maxEnrollment", "max_enrollment");
			Integer enrolled = intOrNull(node, "enrolled", "enrollment", "currentEnrollment", "current_enrollment");
			Integer openSeats = intOrNull(node, "open_seats", "openSeats", "available", "seats_available", "seatsAvailable");
			if (openSeats == null && capacity != null && enrolled != null) {
				openSeats = Math.max(0, capacity - enrolled);
			}

			return new SectionSeatState(
					status != null ? status : SectionSeatState.STATUS_UNKNOWN,
					capacity,
					enrolled,
					openSeats
			);
		} catch (Exception e) {
			return new SectionSeatState(SectionSeatState.STATUS_UNKNOWN, null, null, null);
		}
	}

	private static String textOrNull(JsonNode node, String... keys) {
		for (String key : keys) {
			if (!node.has(key)) continue;
			JsonNode n = node.get(key);
			if (n != null && n.isTextual()) return n.asText();
		}
		return null;
	}

	private static Integer intOrNull(JsonNode node, String... keys) {
		for (String key : keys) {
			if (!node.has(key)) continue;
			JsonNode n = node.get(key);
			if (n != null && n.isNumber()) return n.asInt();
		}
		return null;
	}

	private static String normalizeStatus(String status) {
		if (status == null) return SectionSeatState.STATUS_UNKNOWN;
		String s = status.trim();
		if (s.equalsIgnoreCase("open") || s.equalsIgnoreCase("OPEN")) return SectionSeatState.STATUS_OPEN;
		if (s.equalsIgnoreCase("closed") || s.equalsIgnoreCase("CLOSED")) return SectionSeatState.STATUS_CLOSED;
		return s;
	}
}
