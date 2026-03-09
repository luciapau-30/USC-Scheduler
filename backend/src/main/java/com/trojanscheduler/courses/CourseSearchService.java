package com.trojanscheduler.courses;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.cache.SearchCache;
import com.trojanscheduler.cache.SearchCacheRepository;
import com.trojanscheduler.usc.ProgramSchoolResolver;
import com.trojanscheduler.usc.UscClient;
import com.trojanscheduler.usc.UscClientProperties;
import com.trojanscheduler.usc.UscException;

@Service
public class CourseSearchService {

	private final UscClient uscClient;
	private final UscClientProperties uscProperties;
	private final ProgramSchoolResolver programSchoolResolver;
	private final SearchCacheRepository searchCacheRepository;
	private final long searchTtlSeconds;

	public CourseSearchService(
			UscClient uscClient,
			UscClientProperties uscProperties,
			ProgramSchoolResolver programSchoolResolver,
			SearchCacheRepository searchCacheRepository,
			@Value("${app.cache.search-ttl-seconds:300}") long searchTtlSeconds
	) {
		this.uscClient = uscClient;
		this.uscProperties = uscProperties;
		this.programSchoolResolver = programSchoolResolver;
		this.searchCacheRepository = searchCacheRepository;
		this.searchTtlSeconds = searchTtlSeconds;
	}

	/**
	 * Search courses for a term via USC CoursesByTermSchoolProgram. Returns cached JSON if valid; otherwise fetches from USC and caches.
	 * Uses termCode, school (e.g. DRNS), and program (e.g. ALI, MATH) as the search key.
	 */
	private static final String DEFAULT_TERM = "20263";

	@Transactional
	public String search(String termCode, String query, String schoolParam) {
		String term = normalizeTermCode(termCode);
		String q = query != null ? query.trim() : "";
		// If user put course name (e.g. MATH) in term field and left search empty, swap
		if (!term.isEmpty() && !isNumericTerm(term) && (q.isEmpty() || isNumericTerm(q))) {
			String swap = term;
			term = q.isEmpty() ? DEFAULT_TERM : q;
			q = swap;
		}
		String invalidTerm = validateTermCode(term);
		if (invalidTerm != null) {
			return "{\"courses\":[],\"error\":\"" + escapeJson(invalidTerm) + "\"}";
		}
		// Program is required for CoursesByTermSchoolProgram; use query as program
		String program = q.isEmpty() ? "" : q.trim().toUpperCase();
		if (program.isEmpty()) {
			return "{\"courses\":[]}";
		}
		String school = (schoolParam != null && !schoolParam.trim().isEmpty())
				? schoolParam.trim().toUpperCase()
				: programSchoolResolver.getSchoolForProgram(term, program);
		String searchKey = normalizeSearchKey(school + ":" + program);
		Instant now = Instant.now();

		var cached = searchCacheRepository.findByTermCodeAndSearchKey(term, searchKey);
		if (cached.isPresent() && !cached.get().isExpired(now)) {
			return cached.get().getPayloadJson();
		}

		String path = buildSearchPath(term, school, program);
		String payload;
		try {
			payload = uscClient.get(path);
		} catch (UscException e) {
			String userMessage = toUserFriendlyTermError(e.getMessage(), term);
			String fallback = "{\"courses\":[],\"error\":\"" + escapeJson(userMessage) + "\"}";
			return fallback;
		} catch (Exception e) {
			// Timeouts/connection errors not wrapped as UscException; avoid leaking stack traces
			return "{\"courses\":[],\"error\":\"USC classes server is not reachable. Check your connection or try again later.\"}";
		}
		Instant expiresAt = now.plusSeconds(searchTtlSeconds);

		cached.ifPresent(searchCacheRepository::delete);
		searchCacheRepository.save(new SearchCache(term, searchKey, now, expiresAt, null, payload));
		return payload;
	}

	private static String escapeJson(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
	}

	private static String normalizeSearchKey(String query) {
		if (query == null) return "";
		return query.trim().toLowerCase();
	}

	private String buildSearchPath(String cleanTerm, String school, String program) {
		String template = uscProperties.getSearchPathTemplate();
		return template
				.replace("{termCode}", URLEncoder.encode(cleanTerm, StandardCharsets.UTF_8))
				.replace("{school}", URLEncoder.encode(school, StandardCharsets.UTF_8))
				.replace("{program}", URLEncoder.encode(program, StandardCharsets.UTF_8));
	}

	/** USC expects termCode to be a numeric code only (e.g. 20263). Strip spaces and take first token. */
	private static String normalizeTermCode(String termCode) {
		if (termCode == null) return "";
		String t = termCode.trim();
		int space = t.indexOf(' ');
		if (space > 0) t = t.substring(0, space);
		return t;
	}

	private static boolean isNumericTerm(String s) {
		if (s == null || s.isEmpty()) return false;
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isDigit(s.charAt(i))) return false;
		}
		return true;
	}

	/**
	 * Validates USC term code: must be 5 digits and start with "20" (e.g. 20263).
	 * Returns an error message if invalid, or null if valid.
	 */
	private static String validateTermCode(String term) {
		if (term == null || term.isEmpty()) return null;
		if (!isNumericTerm(term)) return null; // non-numeric handled elsewhere
		if (term.length() != 5) {
			return "Term code must be 5 digits (e.g. 20263 for Fall 2026). You entered: " + term + ".";
		}
		if (!term.startsWith("20")) {
			return "Term code " + term + " is not valid. Use a current term (e.g. 20263).";
		}
		return null;
	}

	/** If USC returns 'Configuration does not exist for X', return a clearer message. */
	private static String toUserFriendlyTermError(String uscMessage, String termUsed) {
		if (uscMessage == null) return "USC API error. Try again or use a different term.";
		if (uscMessage.contains("Configuration does not exist for ")) {
			return "The term code " + termUsed + " is not valid or not available at USC. Use a current term (e.g. 20263).";
		}
		return uscMessage;
	}
}
