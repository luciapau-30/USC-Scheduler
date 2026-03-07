package com.trojanscheduler.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final String issuer;
	private final long accessTokenTtlSeconds;

	public JwtService(
			@Value("${app.security.jwt.secret}") String secret,
			@Value("${app.security.jwt.issuer}") String issuer,
			@Value("${app.security.jwt.access-token-ttl-seconds}") long accessTokenTtlSeconds
	) {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalArgumentException("app.security.jwt.secret must be at least 32 bytes for HS256");
		}
		this.key = Keys.hmacShaKeyFor(keyBytes);
		this.issuer = issuer;
		this.accessTokenTtlSeconds = accessTokenTtlSeconds;
	}

	/** Build HMAC-SHA key for the same secret (e.g. for JwtDecoder bean). */
	public SecretKey getSigningKey() {
		return key;
	}

	public long getAccessTokenTtlSeconds() {
		return accessTokenTtlSeconds;
	}

	/** Create a signed access token for the user. */
	public String createAccessToken(Long userId, String email) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessTokenTtlSeconds);
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("email", email)
				.issuer(issuer)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(key)
				.compact();
	}

	/**
	 * Parse and validate the token; returns claims or throws.
	 * Use this only when you need to read claims outside the filter chain (e.g. refresh flow).
	 */
	public Claims parseAccessToken(String token) {
		try {
			return Jwts.parser()
					.verifyWith(key)
					.requireIssuer(issuer)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException e) {
			throw new InvalidTokenException("Invalid or expired access token", e);
		}
	}
}
