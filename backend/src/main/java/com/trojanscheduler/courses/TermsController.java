package com.trojanscheduler.courses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class TermsController {

	@GetMapping("/terms")
	public List<Map<String, String>> getTerms() {
		return List.of(
				Map.of("termCode", "20263", "label", "Fall 2026"),
				Map.of("termCode", "20261", "label", "Spring 2026"),
				Map.of("termCode", "20256", "label", "Summer 2026"),
				Map.of("termCode", "20253", "label", "Fall 2025"),
				Map.of("termCode", "20251", "label", "Spring 2025")
		);
	}
}
