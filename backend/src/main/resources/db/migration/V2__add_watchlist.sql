CREATE TABLE watchlist (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  term_code VARCHAR(16) NOT NULL,
  sis_section_id VARCHAR(32) NOT NULL,
  priority TINYINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_watchlist_user_term_section (user_id, term_code, sis_section_id),
  KEY idx_watchlist_by_section (term_code, sis_section_id, priority)
);
