package com.trojanscheduler.watchlist.dto;

import java.time.Instant;

public record WatchlistItemResponse(
		Long id,
		String termCode,
		String sisSectionId,
		String coursePrefix,
		int priority,
		Instant createdAt
) {}
