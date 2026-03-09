package com.trojanscheduler.auth;

import java.time.Instant;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trojanscheduler.auth.dto.LoginRequest;
import com.trojanscheduler.auth.dto.RegisterRequest;
import com.trojanscheduler.auth.dto.TokenResponse;
import com.trojanscheduler.user.User;
import com.trojanscheduler.user.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			JwtService jwtService,
			RefreshTokenService refreshTokenService
	) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request, HttpServletRequest req, HttpServletResponse res) {
		if (userRepository.existsByEmail(request.email())) {
			throw new AuthException("An account with this email already exists");
		}
		String hash = passwordEncoder.encode(request.password());
		User user = new User(request.email(), hash, Instant.now());
		user = userRepository.save(user);
		// Auto-login: create refresh cookie and return access token
		refreshTokenService.createAndSetCookie(user, req, res);
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return new TokenResponse(accessToken, jwtService.getAccessTokenTtlSeconds());
	}

	public TokenResponse login(LoginRequest request, HttpServletRequest req, HttpServletResponse res) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new AuthException("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new AuthException("Invalid email or password");
		}
		refreshTokenService.createAndSetCookie(user, req, res);
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return new TokenResponse(accessToken, jwtService.getAccessTokenTtlSeconds());
	}

	@Transactional
	public TokenResponse refresh(HttpServletRequest req, HttpServletResponse res) {
		// @Transactional keeps the Hibernate session open so user.getEmail() doesn't
		// hit LazyInitializationException on the proxy returned from rotateAndSetCookie.
		User user = refreshTokenService.rotateAndSetCookie(req, res);
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return new TokenResponse(accessToken, jwtService.getAccessTokenTtlSeconds());
	}

	public void logout(HttpServletRequest req, HttpServletResponse res) {
		refreshTokenService.revokeAndClearCookie(req, res);
	}

}
