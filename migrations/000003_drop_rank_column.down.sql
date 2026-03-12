ALTER TABLE user_rankings ADD COLUMN rank INT;
ALTER TABLE repo_rankings ADD COLUMN rank INT;
CREATE INDEX idx_user_rankings_rank ON user_rankings(rank);
CREATE INDEX idx_repo_rankings_rank ON repo_rankings(rank);
