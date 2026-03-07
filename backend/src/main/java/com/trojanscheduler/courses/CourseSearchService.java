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
	@Transactional
	public String search(String termCode, String query) {
		String searchKey = normalizeSearchKey(query);
		Instant now = Instant.now();

		var cached = searchCacheRepository.findByTermCodeAndSearchKey(termCode, searchKey);
		if (cached.isPresent() && !cached.get().isExpired(now)) {
			return cached.get().getPayloadJson();
		}

		String path = buildSearchPath(termCode, query);
		String payload = uscClient.get(path);
		Instant expiresAt = now.plusSeconds(searchTtlSeconds);

		cached.ifPresent(searchCacheRepository::delete);
		searchCacheRepository.save(new SearchCache(termCode, searchKey, now, expiresAt, null, payload));
		return payload;
	}

	private static String normalizeSearchKey(String query) {
		if (query == null) return "";
		return query.trim().toLowerCase();
	}

	private String buildSearchPath(String termCode, String query) {
		String template = uscProperties.getSearchPathTemplate();
		String encodedQ = query != null ? URLEncoder.encode(query.trim(), StandardCharsets.UTF_8) : "";
		return template
				.replace("{termCode}", URLEncoder.encode(termCode, StandardCharsets.UTF_8))
				.replace("{q}", encodedQ);
	}
}
