package com.trojanscheduler.section;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses USC classes.usc.edu API JSON into seat state.
 * API shape: courses[].sections[] with totalSeats, registeredSeats, isFull, isCancelled
 * (no openSeats — compute as totalSeats - registeredSeats, or 0 when isFull).
 */
@Service
public class SeatStateService {

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Parse full search response and return seat state per section by sisSectionId.
	 * Structure: { courses: [ { sections: [ { sisSectionId, totalSeats, registeredSeats, isFull, isCancelled } ] } ] }
	 */
	public Map<String, SectionSeatState> parseSectionsFromSearchResponse(String fullSearchJson) {
		Map<String, SectionSeatState> out = new LinkedHashMap<>();
		if (fullSearchJson == null || fullSearchJson.isBlank()) return out;
		try {
			JsonNode root = objectMapper.readTree(fullSearchJson);
			JsonNode courses = root.get("courses");
			if (courses == null || !courses.isArray()) return out;
			for (JsonNode course : courses) {
				JsonNode sections = course.get("sections");
				if (sections == null || !sections.isArray()) continue;
				for (JsonNode section : sections) {
					String sisSectionId = textOrNull(section, "sisSectionId");
					if (sisSectionId != null && !sisSectionId.isBlank())
						out.put(sisSectionId, parseSectionNode(section));
				}
			}
		} catch (Exception ignored) {
			// return empty map
		}
		return out;
	}

	/**
	 * Parse a single section object (USC shape: totalSeats, registeredSeats, isFull, isCancelled).
	 */
	public SectionSeatState parseSectionNode(JsonNode section) {
		if (section == null) return new SectionSeatState(SectionSeatState.STATUS_UNKNOWN, null, null, null);
		Integer totalSeats = intOrNull(section, "totalSeats");
		Integer registeredSeats = intOrNull(section, "registeredSeats");
		Boolean isFull = boolOrNull(section, "isFull");
		Boolean isCancelled = boolOrNull(section, "isCancelled");

		String status = SectionSeatState.STATUS_UNKNOWN;
		if (Boolean.TRUE.equals(isCancelled)) status = SectionSeatState.STATUS_CANCELLED;
		else if (Boolean.TRUE.equals(isFull)) status = SectionSeatState.STATUS_CLOSED;
		else status = SectionSeatState.STATUS_OPEN;

		Integer openSeats = null;
		if (totalSeats != null && registeredSeats != null) {
			openSeats = Boolean.TRUE.equals(isFull) ? 0 : Math.max(0, totalSeats - registeredSeats);
		}

		return new SectionSeatState(status, totalSeats, registeredSeats, openSeats);
	}

	/**
	 * Parse section payload: tries USC shape (totalSeats, registeredSeats, isFull, isCancelled) first,
	 * then fallback keys (open_seats, capacity, enrolled, etc.).
	 */
	public SectionSeatState parseSectionPayload(String rawJson) {
		if (rawJson == null || rawJson.isBlank()) {
			return new SectionSeatState(SectionSeatState.STATUS_UNKNOWN, null, null, null);
		}
		try {
			JsonNode root = objectMapper.readTree(rawJson);
			JsonNode node = root.has("section") ? root.get("section") : root.has("data") ? root.get("data") : root;
			if (node == null) node = root;

			// USC API shape
			if (node.has("totalSeats") || node.has("registeredSeats") || node.has("isFull")) {
				return parseSectionNode(node);
			}

			// Fallback: generic keys
			String status = textOrNull(node, "status", "registration_status", "enrollmentStatus");
			if (status != null) status = normalizeStatus(status);

			Integer capacity = intOrNull(node, "capacity", "maxEnrollment", "max_enrollment", "totalSeats");
			Integer enrolled = intOrNull(node, "enrolled", "enrollment", "currentEnrollment", "current_enrollment", "registeredSeats");
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

	/**
	 * Find one section by sisSectionId in full search JSON and return its seat state.
	 */
	public Optional<SectionSeatState> findSectionInSearchResponse(String fullSearchJson, String sisSectionId) {
		if (sisSectionId == null || sisSectionId.isBlank()) return Optional.empty();
		SectionSeatState state = parseSectionsFromSearchResponse(fullSearchJson).get(sisSectionId);
		return state != null ? Optional.of(state) : Optional.empty();
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

	private static Boolean boolOrNull(JsonNode node, String... keys) {
		for (String key : keys) {
			if (!node.has(key)) continue;
			JsonNode n = node.get(key);
			if (n != null && n.isBoolean()) return n.asBoolean();
		}
		return null;
	}

	private static String normalizeStatus(String status) {
		if (status == null) return SectionSeatState.STATUS_UNKNOWN;
		String s = status.trim();
		if (s.equalsIgnoreCase("open")) return SectionSeatState.STATUS_OPEN;
		if (s.equalsIgnoreCase("closed")) return SectionSeatState.STATUS_CLOSED;
		if (s.equalsIgnoreCase("cancelled")) return SectionSeatState.STATUS_CANCELLED;
		return s;
	}
}
