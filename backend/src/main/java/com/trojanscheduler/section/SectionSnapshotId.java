package com.trojanscheduler.section;

import java.io.Serializable;
import java.util.Objects;

public class SectionSnapshotId implements Serializable {

	private String termCode;
	private String sisSectionId;

	public SectionSnapshotId() {}

	public SectionSnapshotId(String termCode, String sisSectionId) {
		this.termCode = termCode;
		this.sisSectionId = sisSectionId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SectionSnapshotId that = (SectionSnapshotId) o;
		return Objects.equals(termCode, that.termCode) && Objects.equals(sisSectionId, that.sisSectionId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(termCode, sisSectionId);
	}
}
