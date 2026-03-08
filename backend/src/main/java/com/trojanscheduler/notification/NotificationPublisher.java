package com.trojanscheduler.notification;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.trojanscheduler.notification.dto.NotificationPayload;

/**
 * Pushes notification payloads to connected WebSocket clients (per-user queue).
 */
@Service
public class NotificationPublisher {

	private static final String DESTINATION_QUEUE_NOTIFICATIONS = "/queue/notifications";

	private final SimpMessagingTemplate messagingTemplate;

	public NotificationPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	/**
	 * Send a notification to a specific user (they must be subscribed to /user/queue/notifications).
	 * userId is the principal name set at CONNECT (e.g. from JWT sub claim).
	 */
	public void sendToUser(Long userId, NotificationPayload payload) {
		messagingTemplate.convertAndSendToUser(String.valueOf(userId), DESTINATION_QUEUE_NOTIFICATIONS, payload);
	}
}
