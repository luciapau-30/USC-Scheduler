package com.trojanscheduler;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the backend root so browsers hitting http://localhost:8080/ get a clear response
 * instead of the Whitelabel 404 (No static resource .).
 */
@RestController
public class RootController {

	@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> root() {
		return ResponseEntity.ok("Trojan Scheduler API — use the frontend at http://localhost:5173 or call /api/* endpoints.");
	}
}
