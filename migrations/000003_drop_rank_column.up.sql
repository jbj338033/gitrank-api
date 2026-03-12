ALTER TABLE user_rankings DROP COLUMN rank;
ALTER TABLE repo_rankings DROP COLUMN rank;
DROP INDEX IF EXISTS idx_user_rankings_rank;
DROP INDEX IF EXISTS idx_repo_rankings_rank;
