package com.trojanscheduler.auth;

/**
 * Thrown when a JWT or refresh token is invalid or expired.
 */
public class InvalidTokenException extends RuntimeException {

	public InvalidTokenException(String message) {
		super(message);
	}

	public InvalidTokenException(String message, Throwable cause) {
		super(message, cause);
	}
}
