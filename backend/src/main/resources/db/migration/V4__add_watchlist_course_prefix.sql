-- Course prefix (e.g. MATH, CSCI) for Option B polling: one search per (termCode, prefix) returns all sections for that course.
ALTER TABLE watchlist ADD COLUMN course_prefix VARCHAR(16) NULL;
CREATE INDEX idx_watchlist_term_prefix ON watchlist (term_code, course_prefix);
