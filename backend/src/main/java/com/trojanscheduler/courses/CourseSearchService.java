package com.trojanscheduler.courses;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.cache.SearchCache;
import com.trojanscheduler.cache.SearchCacheRepository;
import com.trojanscheduler.usc.UscClient;
import com.trojanscheduler.usc.UscClientProperties;
import com.trojanscheduler.usc.UscException;

@Service
public class CourseSearchService {

	private final UscClient uscClient;
	private final UscClientProperties uscProperties;
	private final SearchCacheRepository searchCacheRepository;
	private final long searchTtlSeconds;

	public CourseSearchService(
			UscClient uscClient,
			UscClientProperties uscProperties,
			SearchCacheRepository searchCacheRepository,
			@Value("${app.cache.search-ttl-seconds:300}") long searchTtlSeconds
	) {
		this.uscClient = uscClient;
		this.uscProperties = uscProperties;
		this.searchCacheRepository = searchCacheRepository;
		this.searchTtlSeconds = searchTtlSeconds;
	}

	/**
	 * Search courses for a term. Returns cached JSON if valid; otherwise fetches from USC and caches.
	 */
	private static final String DEFAULT_TERM = "20263";

	@Transactional
	public String search(String termCode, String query) {
		String term = normalizeTermCode(termCode);
		String q = query != null ? query.trim() : "";
		// If user put course name (e.g. MATH) in term field and left search empty, swap
		if (!term.isEmpty() && !isNumericTerm(term) && (q.isEmpty() || isNumericTerm(q))) {
			String swap = term;
			term = q.isEmpty() ? DEFAULT_TERM : q;
			q = swap;
		}
		String searchKey = normalizeSearchKey(q);
		Instant now = Instant.now();

		var cached = searchCacheRepository.findByTermCodeAndSearchKey(term, searchKey);
		if (cached.isPresent() && !cached.get().isExpired(now)) {
			return cached.get().getPayloadJson();
		}

		String path = buildSearchPath(term, q);
		String payload;
		try {
			payload = uscClient.get(path);
		} catch (UscException e) {
			String fallback = "{\"courses\":[],\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
			return fallback;
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

	private String buildSearchPath(String cleanTerm, String query) {
		String template = uscProperties.getSearchPathTemplate();
		String encodedQ = query != null ? URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) : "";
		return template
				.replace("{termCode}", URLEncoder.encode(cleanTerm, StandardCharsets.UTF_8))
				.replace("{q}", encodedQ);
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
}
