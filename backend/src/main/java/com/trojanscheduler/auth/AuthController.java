package com.trojanscheduler.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trojanscheduler.auth.dto.LoginRequest;
import com.trojanscheduler.auth.dto.RegisterRequest;
import com.trojanscheduler.auth.dto.TokenResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<TokenResponse> register(
			@Valid @RequestBody RegisterRequest request,
			HttpServletRequest req,
			HttpServletResponse res
	) {
		TokenResponse token = authService.register(request, req, res);
		return ResponseEntity.status(201).body(token);
	}

	@PostMapping("/login")
	public ResponseEntity<TokenResponse> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest req,
			HttpServletResponse res
	) {
		TokenResponse token = authService.login(request, req, res);
		return ResponseEntity.ok(token);
	}

	@PostMapping("/refresh")
	public ResponseEntity<TokenResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
		TokenResponse token = authService.refresh(req, res);
		return ResponseEntity.ok(token);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest req, HttpServletResponse res) {
		authService.logout(req, res);
		return ResponseEntity.noContent().build();
	}
}
