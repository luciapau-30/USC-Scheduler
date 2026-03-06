package com.trojanscheduler.auth;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

	@Modifying
	@Query("""
		update RefreshToken rt
		set rt.revokedAt = :revokedAt
		where rt.user.id = :userId
			and rt.revokedAt is null
			and rt.expiresAt > :now
	""")
	int revokeAllActiveByUserId(
			@Param("userId") Long userId,
			@Param("revokedAt") Instant revokedAt,
			@Param("now") Instant now
	);
}
