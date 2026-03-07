package com.trojanscheduler.watchlist.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddWatchlistRequest(
		@NotBlank(message = "Term code is required")
		@Size(max = 16)
		String termCode,

		@NotBlank(message = "Section ID is required")
		@Size(max = 32)
		String sisSectionId,

		@Min(0) @Max(1)
		int priority
) {}
