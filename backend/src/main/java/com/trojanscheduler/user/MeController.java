package com.trojanscheduler.user;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MeController {

	private final UserRepository userRepository;

	public MeController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping("/me")
	public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null) {
			return ResponseEntity.status(401).build();
		}
		String sub = jwt.getSubject();
		Long userId = Long.valueOf(sub);
		return userRepository.findById(userId)
				.map(user -> ResponseEntity.ok(Map.<String, Object>of(
						"id", user.getId(),
						"email", user.getEmail())))
				.orElse(ResponseEntity.notFound().build());
	}
}
