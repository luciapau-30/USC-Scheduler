package com.trojanscheduler.auth;

/**
 * Thrown when auth fails (bad credentials, duplicate email, etc.).
 */
public class AuthException extends RuntimeException {

	public AuthException(String message) {
		super(message);
	}
}
