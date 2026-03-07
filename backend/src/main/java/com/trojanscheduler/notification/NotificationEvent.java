package com.trojanscheduler.notification;

import java.time.Instant;

import com.trojanscheduler.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_event")
public class NotificationEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "term_code", nullable = false, length = 16)
	private String termCode;

	@Column(name = "sis_section_id", nullable = false, length = 32)
	private String sisSectionId;

	@Column(name = "event_type", nullable = false, length = 32)
	private String eventType;

	@Column(name = "event_fingerprint", nullable = false, length = 32, columnDefinition = "BINARY(32)")
	private byte[] eventFingerprint;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected NotificationEvent() {}

	public NotificationEvent(User user, String termCode, String sisSectionId, String eventType, byte[] eventFingerprint, Instant createdAt) {
		this.user = user;
		this.termCode = termCode;
		this.sisSectionId = sisSectionId;
		this.eventType = eventType;
		this.eventFingerprint = eventFingerprint;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public String getTermCode() { return termCode; }
	public String getSisSectionId() { return sisSectionId; }
	public String getEventType() { return eventType; }
	public Instant getCreatedAt() { return createdAt; }
}
