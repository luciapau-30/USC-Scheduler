CREATE TABLE search_cache (
  term_code VARCHAR(16) NOT NULL,
  search_key VARCHAR(255) NOT NULL,
  fetched_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  etag VARCHAR(128) NULL,
  payload_json TEXT NOT NULL,
  PRIMARY KEY (term_code, search_key)
);
CREATE INDEX idx_search_cache_expires ON search_cache (expires_at);

CREATE TABLE section_snapshot (
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  last_seen_at TIMESTAMP NOT NULL,
  status VARCHAR(32) NOT NULL,
  capacity INT NULL,
  enrolled INT NULL,
  open_seats INT NULL,
  raw_json TEXT NULL,
  PRIMARY KEY (term_code, sis_section_id)
);
CREATE INDEX idx_section_snapshot_last_seen ON section_snapshot (last_seen_at);
CREATE INDEX idx_section_snapshot_open ON section_snapshot (term_code, open_seats);

CREATE TABLE notification_event (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  event_fingerprint BYTEA NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_notification_event_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uk_notification_user_fingerprint UNIQUE (user_id, event_fingerprint)
);
CREATE INDEX idx_notification_event_user_time ON notification_event (user_id, created_at);
