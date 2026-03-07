package com.trojanscheduler.usc;

/**
 * Thrown when the USC API request fails (non-2xx, timeout, or connection error).
 * For 429 Rate Limited, {@link #getRetryAfterSeconds()} may be set from Retry-After header.
 */
public class UscException extends RuntimeException {

	private final int statusCode;
	private final Integer retryAfterSeconds;

	public UscException(String message) {
		super(message);
		this.statusCode = 0;
		this.retryAfterSeconds = null;
	}

	public UscException(String message, Throwable cause) {
		super(message, cause);
		this.statusCode = 0;
		this.retryAfterSeconds = null;
	}

	public UscException(String message, int statusCode, Integer retryAfterSeconds) {
		super(message);
		this.statusCode = statusCode;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public int getStatusCode() {
		return statusCode;
	}

	/** Present when status was 429 and Retry-After header was provided. */
	public Integer getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

	public boolean isRateLimited() {
		return statusCode == 429;
	}

	public boolean isServerError() {
		return statusCode >= 500 && statusCode < 600;
	}
}
