ALTER TABLE watchlist ADD COLUMN course_prefix VARCHAR(16) NULL;
CREATE INDEX idx_watchlist_term_prefix ON watchlist (term_code, course_prefix);
