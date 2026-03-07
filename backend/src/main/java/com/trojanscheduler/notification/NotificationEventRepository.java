package com.trojanscheduler.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

	boolean existsByUser_IdAndEventFingerprint(Long userId, byte[] eventFingerprint);
}
