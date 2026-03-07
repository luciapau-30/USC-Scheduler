package com.trojanscheduler.courses;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseSearchController {

	private final CourseSearchService courseSearchService;

	public CourseSearchController(CourseSearchService courseSearchService) {
		this.courseSearchService = courseSearchService;
	}

	@GetMapping(path = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> search(
			@RequestParam String termCode,
			@RequestParam(required = false, defaultValue = "") String q
	) {
		String json = courseSearchService.search(termCode, q);
		return ResponseEntity.ok(json);
	}
}
