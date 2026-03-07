package com.trojanscheduler.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.user.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class RefreshTokenService {

	private static final int TOKEN_BYTES = 32;
	private static final SecureRandom RNG = new SecureRandom();

	private final RefreshTokenRepository refreshTokenRepository;
	private final long ttlSeconds;
	private final String cookieName;
	private final String cookiePath;
	private final boolean cookieSecure;
	private final String cookieSameSite;

	public RefreshTokenService(
			RefreshTokenRepository refreshTokenRepository,
			@Value("${app.security.refresh-token-ttl-seconds}") long ttlSeconds,
			@Value("${app.security.refresh-cookie.name}") String cookieName,
			@Value("${app.security.refresh-cookie.path}") String cookiePath,
			@Value("${app.security.refresh-cookie.secure}") boolean cookieSecure,
			@Value("${app.security.refresh-cookie.same-site}") String cookieSameSite
	) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.ttlSeconds = ttlSeconds;
		this.cookieName = cookieName;
		this.cookiePath = cookiePath;
		this.cookieSecure = cookieSecure;
		this.cookieSameSite = cookieSameSite;
	}

	/** Create a new refresh token, persist its hash, and set the cookie on the response. */
	@Transactional
	public RefreshToken createAndSetCookie(User user, HttpServletRequest request, HttpServletResponse response) {
		byte[] raw = new byte[TOKEN_BYTES];
		RNG.nextBytes(raw);
		byte[] hash = sha256(raw);
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(ttlSeconds);
		String userAgent = request.getHeader("User-Agent");
		byte[] ipHash = hashOptional(request.getRemoteAddr());

		RefreshToken token = new RefreshToken(user, hash, now, expiresAt, userAgent, ipHash);
		refreshTokenRepository.save(token);
		setCookie(response, raw, (int) ttlSeconds);
		return token;
	}

	/**
	 * Validate the refresh token from the cookie, revoke it, create a new one (rotation),
	 * set the new cookie, and return the user.
	 */
	@Transactional
	public User rotateAndSetCookie(HttpServletRequest request, HttpServletResponse response) {
		String encoded = getRefreshTokenFromCookie(request);
		if (encoded == null || encoded.isBlank()) {
			throw new InvalidTokenException("Missing refresh token cookie");
		}
		byte[] raw = decodeCookieValue(encoded);
		byte[] hash = sha256(raw);
		RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
				.orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
		Instant now = Instant.now();
		if (!current.isActive(now)) {
			throw new InvalidTokenException("Refresh token expired or revoked");
		}
		User user = current.getUser();
		// Revoke current (no replacement hash for logout-style revoke we use null for "rotated" if we want to track)
		current.revoke(now, null);
		refreshTokenRepository.save(current);
		// Issue new token and set cookie
		createAndSetCookie(user, request, response);
		return user;
	}

	/** Revoke the refresh token in the cookie (e.g. logout) and clear the cookie. */
	@Transactional
	public void revokeAndClearCookie(HttpServletRequest request, HttpServletResponse response) {
		String encoded = getRefreshTokenFromCookie(request);
		if (encoded != null && !encoded.isBlank()) {
			byte[] raw = decodeCookieValue(encoded);
			byte[] hash = sha256(raw);
			refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
				t.revoke(Instant.now(), null);
				refreshTokenRepository.save(t);
			});
		}
		clearCookie(response);
	}

	public Optional<String> getRefreshTokenFromRequest(HttpServletRequest request) {
		return Optional.ofNullable(getRefreshTokenFromCookie(request));
	}

	private String getRefreshTokenFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (Cookie c : request.getCookies()) {
			if (cookieName.equals(c.getName())) return c.getValue();
		}
		return null;
	}

	private void setCookie(HttpServletResponse response, byte[] rawToken, int maxAgeSeconds) {
		String value = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);
		Cookie cookie = new Cookie(cookieName, value);
		cookie.setPath(cookiePath);
		cookie.setHttpOnly(true);
		cookie.setSecure(cookieSecure);
		cookie.setMaxAge(maxAgeSeconds);
		cookie.setAttribute("SameSite", cookieSameSite);
		response.addCookie(cookie);
	}

	private void clearCookie(HttpServletResponse response) {
		Cookie cookie = new Cookie(cookieName, "");
		cookie.setPath(cookiePath);
		cookie.setHttpOnly(true);
		cookie.setSecure(cookieSecure);
		cookie.setMaxAge(0);
		cookie.setAttribute("SameSite", cookieSameSite);
		response.addCookie(cookie);
	}

	private static byte[] sha256(byte[] input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return md.digest(input);
		} catch (Exception e) {
			throw new RuntimeException("SHA-256 unavailable", e);
		}
	}

	private static byte[] hashOptional(String s) {
		if (s == null || s.isBlank()) return null;
		return sha256(s.getBytes(StandardCharsets.UTF_8));
	}

	private static byte[] decodeCookieValue(String base64Url) {
		try {
			return Base64.getUrlDecoder().decode(base64Url);
		} catch (IllegalArgumentException e) {
			throw new InvalidTokenException("Invalid refresh token format", e);
		}
	}
}
