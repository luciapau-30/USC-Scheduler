-- Align priority with JPA integer type (was TINYINT)
ALTER TABLE watchlist MODIFY COLUMN priority INT NOT NULL DEFAULT 0;
