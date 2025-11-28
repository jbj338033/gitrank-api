CREATE TABLE users (
    id BINARY(16) PRIMARY KEY,
    github_id BIGINT NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255),
    total_commits INT NOT NULL DEFAULT 0,
    total_stars INT NOT NULL DEFAULT 0,
    total_followers INT NOT NULL DEFAULT 0,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6)
);

CREATE TABLE repos (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    github_repo_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    language VARCHAR(255),
    stars INT NOT NULL DEFAULT 0,
    forks INT NOT NULL DEFAULT 0,
    is_registered BOOLEAN NOT NULL DEFAULT FALSE,
    last_synced_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE refresh_tokens (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_repos_user_id ON repos(user_id);
CREATE INDEX idx_repos_github_repo_id ON repos(github_repo_id);
CREATE INDEX idx_repos_is_registered ON repos(is_registered);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
