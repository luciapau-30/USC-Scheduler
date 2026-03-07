package com.trojanscheduler.cache;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_cache")
@IdClass(SearchCacheId.class)
public class SearchCache {

	@Id
	@Column(name = "term_code", nullable = false, length = 16)
	private String termCode;

	@Id
	@Column(name = "search_key", nullable = false, length = 255)
	private String searchKey;

	@Column(name = "fetched_at", nullable = false)
	private Instant fetchedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(length = 128)
	private String etag;

	@Column(name = "payload_json", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String payloadJson;

	protected SearchCache() {}

	public SearchCache(String termCode, String searchKey, Instant fetchedAt, Instant expiresAt, String etag, String payloadJson) {
		this.termCode = termCode;
		this.searchKey = searchKey;
		this.fetchedAt = fetchedAt;
		this.expiresAt = expiresAt;
		this.etag = etag;
		this.payloadJson = payloadJson;
	}

	public String getTermCode() { return termCode; }
	public String getSearchKey() { return searchKey; }
	public Instant getFetchedAt() { return fetchedAt; }
	public Instant getExpiresAt() { return expiresAt; }
	public String getEtag() { return etag; }
	public String getPayloadJson() { return payloadJson; }

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now);
	}
}
