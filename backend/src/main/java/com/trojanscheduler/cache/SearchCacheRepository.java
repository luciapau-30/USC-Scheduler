package com.trojanscheduler.cache;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SearchCacheRepository extends JpaRepository<SearchCache, SearchCacheId> {

	Optional<SearchCache> findByTermCodeAndSearchKey(String termCode, String searchKey);

	@Modifying
	@Query("DELETE FROM SearchCache s WHERE s.expiresAt < :now")
	int deleteExpiredBefore(@Param("now") Instant now);
}
