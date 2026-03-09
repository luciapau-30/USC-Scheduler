package com.trojanscheduler.usc;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trojanscheduler.cache.SearchCache;
import com.trojanscheduler.cache.SearchCacheRepository;

/**
 * Resolves the correct school prefix for a program (e.g. CSCI → ENGV, ALI → DRNS)
 * using the USC Programs/TermCode API (https://classes.usc.edu/api/Programs/TermCode?termCode=...).
 */
@Component
public class ProgramSchoolResolver {

	private static final String PROGRAMS_CACHE_KEY = "__programs__";

	private final UscClient uscClient;
	private final UscClientProperties uscProperties;
	private final SearchCacheRepository searchCacheRepository;
	private final long programsTtlSeconds;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ProgramSchoolResolver(
			UscClient uscClient,
			UscClientProperties uscProperties,
			SearchCacheRepository searchCacheRepository,
			@Value("${app.cache.programs-ttl-seconds:3600}") long programsTtlSeconds
	) {
		this.uscClient = uscClient;
		this.uscProperties = uscProperties;
		this.searchCacheRepository = searchCacheRepository;
		this.programsTtlSeconds = programsTtlSeconds;
	}

	/**
	 * Returns the school prefix for the given term and program (e.g. term 20263, program CSCI → ENGV).
	 * Uses cached Programs list when possible; falls back to defaultSchool if not found.
	 */
	@Transactional
	public String getSchoolForProgram(String termCode, String programPrefix) {
		if (programPrefix == null || programPrefix.isBlank()) return uscProperties.getDefaultSchool();
		String program = programPrefix.trim().toUpperCase();
		String programsJson = getProgramsJsonForTerm(termCode);
		if (programsJson == null || programsJson.isBlank()) return uscProperties.getDefaultSchool();
		try {
			JsonNode array = objectMapper.readTree(programsJson);
			if (!array.isArray()) return uscProperties.getDefaultSchool();
			for (JsonNode item : array) {
				String prefix = item.has("prefix") ? item.get("prefix").asText("") : "";
				if (program.equals(prefix.trim())) {
					JsonNode schools = item.get("schools");
					if (schools != null && schools.isArray() && schools.size() > 0) {
						JsonNode first = schools.get(0);
						if (first.has("prefix")) return first.get("prefix").asText("").trim().toUpperCase();
					}
					break;
				}
			}
		} catch (Exception ignored) {
			// fall through to default
		}
		return uscProperties.getDefaultSchool();
	}

	private String getProgramsJsonForTerm(String termCode) {
		Instant now = Instant.now();
		var cached = searchCacheRepository.findByTermCodeAndSearchKey(termCode, PROGRAMS_CACHE_KEY);
		if (cached.isPresent() && cached.get().getExpiresAt().isAfter(now)) {
			return cached.get().getPayloadJson();
		}
		String path = uscProperties.getProgramsPathTemplate()
				.replace("{termCode}", URLEncoder.encode(termCode, StandardCharsets.UTF_8));
		try {
			String payload = uscClient.get(path);
			Instant expiresAt = now.plusSeconds(programsTtlSeconds);
			cached.ifPresent(searchCacheRepository::delete);
			searchCacheRepository.save(new SearchCache(termCode, PROGRAMS_CACHE_KEY, now, expiresAt, null, payload));
			return payload;
		} catch (UscException e) {
			return null;
		}
	}
}
