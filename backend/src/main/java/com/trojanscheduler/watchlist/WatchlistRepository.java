package com.trojanscheduler.watchlist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

	List<Watchlist> findByUserIdOrderByPriorityDescCreatedAtAsc(Long userId);

	Optional<Watchlist> findByUserIdAndTermCodeAndSisSectionId(Long userId, String termCode, String sisSectionId);

	boolean existsByUserIdAndTermCodeAndSisSectionId(Long userId, String termCode, String sisSectionId);

	/** Distinct (termCode, sisSectionId) pairs that appear in any watchlist (for polling). */
	@Query("SELECT DISTINCT w.termCode, w.sisSectionId FROM Watchlist w")
	List<Object[]> findDistinctTermCodeAndSisSectionId();

	/** All watchlist entries for a given section (to notify each user). */
	List<Watchlist> findByTermCodeAndSisSectionId(String termCode, String sisSectionId);
}
