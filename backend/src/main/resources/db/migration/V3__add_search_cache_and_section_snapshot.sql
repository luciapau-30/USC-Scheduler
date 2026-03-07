-- Search cache: fast lookup by term + normalized query; TTL-based expiry
CREATE TABLE search_cache (
  term_code VARCHAR(16) NOT NULL,
  search_key VARCHAR(255) NOT NULL,
  fetched_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  etag VARCHAR(128) NULL,
  payload_json MEDIUMTEXT NOT NULL,
  PRIMARY KEY (term_code, search_key),
  KEY idx_search_cache_expires (expires_at)
);

-- Section snapshot: last-seen seat state per section (for polling + transition detection)
CREATE TABLE section_snapshot (
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  last_seen_at TIMESTAMP NOT NULL,
  status VARCHAR(32) NOT NULL,
  capacity INT NULL,
  enrolled INT NULL,
  open_seats INT NULL,
  raw_json MEDIUMTEXT NULL,
  PRIMARY KEY (term_code, sis_section_id),
  KEY idx_section_snapshot_last_seen (last_seen_at),
  KEY idx_section_snapshot_open (term_code, open_seats)
);

-- Notification dedupe: avoid spamming same seat-open event after restarts
CREATE TABLE notification_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  event_fingerprint BINARY(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_notification_event_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_notification_user_fingerprint (user_id, event_fingerprint),
  KEY idx_notification_event_user_time (user_id, created_at)
);
