package com.trojanscheduler.auth.dto;

public record TokenResponse(
		String accessToken,
		long expiresInSeconds
) {}
