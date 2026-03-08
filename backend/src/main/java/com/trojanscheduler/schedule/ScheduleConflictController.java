package com.trojanscheduler.schedule;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.trojanscheduler.schedule.dto.CheckConflictRequest;
import com.trojanscheduler.schedule.dto.CheckConflictResponse;

import jakarta.validation.Valid;

/**
 * Schedule conflict check for the builder: does adding the candidate meetings
 * conflict with the existing schedule? Requires JWT.
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleConflictController {

	private final ConflictDetectionService conflictDetectionService;

	public ScheduleConflictController(ConflictDetectionService conflictDetectionService) {
		this.conflictDetectionService = conflictDetectionService;
	}

	@PostMapping("/check-conflict")
	public ResponseEntity<CheckConflictResponse> checkConflict(@Valid @RequestBody CheckConflictRequest request) {
		CheckConflictResponse response = conflictDetectionService.checkConflict(
				request.existingMeetings(),
				request.candidateMeetings());
		return ResponseEntity.ok(response);
	}
}
