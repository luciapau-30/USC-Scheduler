CREATE TABLE watchlist (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  priority SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT uk_watchlist_user_term_section UNIQUE (user_id, term_code, sis_section_id)
);
CREATE INDEX idx_watchlist_by_section ON watchlist (term_code, sis_section_id, priority);
