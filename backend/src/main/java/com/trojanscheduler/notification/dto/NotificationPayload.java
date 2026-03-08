package com.trojanscheduler.notification.dto;

import java.time.Instant;

/**
 * Payload pushed over WebSocket when a notification event is created (e.g. seat opened).
 */
public record NotificationPayload(
		String eventType,
		String termCode,
		String sisSectionId,
		Instant createdAt
) {}
