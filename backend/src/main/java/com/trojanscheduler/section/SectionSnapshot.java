package com.trojanscheduler.section;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "section_snapshot")
@IdClass(SectionSnapshotId.class)
public class SectionSnapshot {

	@Id
	@Column(name = "term_code", nullable = false, length = 16)
	private String termCode;

	@Id
	@Column(name = "sis_section_id", nullable = false, length = 32)
	private String sisSectionId;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(nullable = false, length = 32)
	private String status;

	@Column private Integer capacity;
	@Column private Integer enrolled;
	@Column(name = "open_seats") private Integer openSeats;
	@Column(name = "raw_json", columnDefinition = "MEDIUMTEXT") private String rawJson;

	protected SectionSnapshot() {}

	public SectionSnapshot(String termCode, String sisSectionId, Instant lastSeenAt, String status,
			Integer capacity, Integer enrolled, Integer openSeats, String rawJson) {
		this.termCode = termCode;
		this.sisSectionId = sisSectionId;
		this.lastSeenAt = lastSeenAt;
		this.status = status != null ? status : "UNKNOWN";
		this.capacity = capacity;
		this.enrolled = enrolled;
		this.openSeats = openSeats;
		this.rawJson = rawJson;
	}

	public String getTermCode() { return termCode; }
	public String getSisSectionId() { return sisSectionId; }
	public Instant getLastSeenAt() { return lastSeenAt; }
	public String getStatus() { return status; }
	public Integer getCapacity() { return capacity; }
	public Integer getEnrolled() { return enrolled; }
	public Integer getOpenSeats() { return openSeats; }
	public String getRawJson() { return rawJson; }

	public void update(Instant lastSeenAt, String status, Integer capacity, Integer enrolled, Integer openSeats, String rawJson) {
		this.lastSeenAt = lastSeenAt;
		this.status = status != null ? status : "UNKNOWN";
		this.capacity = capacity;
		this.enrolled = enrolled;
		this.openSeats = openSeats;
		this.rawJson = rawJson;
	}
}
