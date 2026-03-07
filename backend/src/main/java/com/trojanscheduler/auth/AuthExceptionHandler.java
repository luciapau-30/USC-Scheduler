package com.trojanscheduler.auth;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.trojanscheduler.usc.UscException;
import com.trojanscheduler.watchlist.WatchlistException;

@RestControllerAdvice
public class AuthExceptionHandler {

	private static final String DUPLICATE_EMAIL_MESSAGE = "An account with this email already exists";
	private static final String DUPLICATE_WATCHLIST_MESSAGE = "Section already in watchlist";

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<Map<String, String>> handleAuthException(AuthException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(InvalidTokenException.class)
	public ResponseEntity<Map<String, String>> handleInvalidTokenException(InvalidTokenException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
	}

	@ExceptionHandler(UscException.class)
	public ResponseEntity<Map<String, String>> handleUscException(UscException e) {
		HttpStatus status = e.isRateLimited() ? HttpStatus.SERVICE_UNAVAILABLE
				: e.isServerError() ? HttpStatus.BAD_GATEWAY
				: HttpStatus.BAD_GATEWAY;
		String message = e.getMessage() != null ? e.getMessage() : "Course data service unavailable";
		return ResponseEntity.status(status).body(Map.of("error", message));
	}

	@ExceptionHandler(WatchlistException.class)
	public ResponseEntity<Map<String, String>> handleWatchlistException(WatchlistException e) {
		String msg = e.getMessage();
		HttpStatus status = msg != null && msg.contains("already in watchlist") ? HttpStatus.CONFLICT
				: msg != null && msg.contains("not found") ? HttpStatus.NOT_FOUND
				: msg != null && msg.contains("Unauthorized") ? HttpStatus.UNAUTHORIZED
				: HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(Map.of("error", msg != null ? msg : "Watchlist error"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
		FieldError first = e.getFieldErrors().stream().findFirst().orElse(null);
		String message = first != null ? first.getDefaultMessage() : "Validation failed";
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}

	/**
	 * Handles unique constraint violations (e.g. concurrent registration with same email).
	 * Check-then-save is not atomic, so the DB constraint can fire even after existsByEmail passed.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
		String message = isDuplicateEmailConstraint(e) ? DUPLICATE_EMAIL_MESSAGE
				: isDuplicateWatchlistConstraint(e) ? DUPLICATE_WATCHLIST_MESSAGE
				: "A conflict occurred. Please try again.";
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
	}

	private static boolean isDuplicateEmailConstraint(DataIntegrityViolationException e) {
		Throwable t = e;
		while (t != null) {
			String msg = t.getMessage();
			if (msg != null && (
					msg.contains("Duplicate") ||
					msg.contains("unique") ||
					msg.contains("uk_users_email") ||
					msg.contains("UK_USERS_EMAIL"))) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private static boolean isDuplicateWatchlistConstraint(DataIntegrityViolationException e) {
		Throwable t = e;
		while (t != null) {
			String msg = t.getMessage();
			if (msg != null && (
					msg.contains("uk_watchlist_user_term_section") ||
					msg.contains("UK_WATCHLIST_USER_TERM_SECTION"))) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}
}
