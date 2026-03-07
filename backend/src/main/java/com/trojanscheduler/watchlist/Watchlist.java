package com.trojanscheduler.watchlist;

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
@Table(name = "watchlist")
public class Watchlist {

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

	@Column(nullable = false)
	private int priority = 0;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Watchlist() {}

	public Watchlist(User user, String termCode, String sisSectionId, int priority, Instant createdAt) {
		this.user = user;
		this.termCode = termCode;
		this.sisSectionId = sisSectionId;
		this.priority = priority;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTermCode() {
		return termCode;
	}

	public String getSisSectionId() {
		return sisSectionId;
	}

	public int getPriority() {
		return priority;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
