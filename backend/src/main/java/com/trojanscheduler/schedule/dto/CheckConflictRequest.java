package com.trojanscheduler.schedule.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request to check if adding candidate meetings would conflict with existing schedule.
 * Client can build these from USC section.schedule (flatten existing sections + candidate section).
 */
public record CheckConflictRequest(
		@NotNull(message = "Existing meetings are required")
		@Valid
		List<MeetingDto> existingMeetings,

		@NotNull(message = "Candidate meetings are required")
		@Valid
		List<MeetingDto> candidateMeetings
) {}
