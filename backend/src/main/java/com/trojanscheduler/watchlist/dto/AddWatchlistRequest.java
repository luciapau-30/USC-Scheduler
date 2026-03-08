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

		/** Course prefix (e.g. MATH, CSCI) from search; used for efficient polling. Optional for backward compatibility. */
		@Size(max = 16)
		String coursePrefix,

		@Min(0) @Max(1)
		int priority
) {}
