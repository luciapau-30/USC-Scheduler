package com.trojanscheduler.section;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SectionSnapshotRepository extends JpaRepository<SectionSnapshot, SectionSnapshotId> {

	Optional<SectionSnapshot> findByTermCodeAndSisSectionId(String termCode, String sisSectionId);
}
