package com.trojanscheduler.auth;

import java.time.Instant;

import com.trojanscheduler.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, length = 32, columnDefinition = "BINARY(32)")
	private byte[] tokenHash;

	@Column(name = "issued_at", nullable = false)
	private Instant issuedAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_hash", length = 32, columnDefinition = "BINARY(32)")
	private byte[] replacedByTokenHash;

	@Column(name = "user_agent")
	private String userAgent;

	@Column(name = "ip_hash", length = 32, columnDefinition = "BINARY(32)")
	private byte[] ipHash;

	protected RefreshToken() {}

	public RefreshToken(
			User user,
			byte[] tokenHash,
			Instant issuedAt,
			Instant expiresAt,
			String userAgent,
			byte[] ipHash
	) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.userAgent = userAgent;
		this.ipHash = ipHash;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public byte[] getTokenHash() {
		return tokenHash;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public byte[] getReplacedByTokenHash() {
		return replacedByTokenHash;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public byte[] getIpHash() {
		return ipHash;
	}

	public boolean isActive(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public void revoke(Instant revokedAt, byte[] replacedByTokenHash) {
		this.revokedAt = revokedAt;
		this.replacedByTokenHash = replacedByTokenHash;
	}
}
