ALTER TABLE users ADD COLUMN github_access_token VARCHAR(512) NULL;
ALTER TABLE users ADD COLUMN github_refresh_token VARCHAR(512) NULL;
ALTER TABLE users ADD COLUMN github_token_expires_at DATETIME NULL;
