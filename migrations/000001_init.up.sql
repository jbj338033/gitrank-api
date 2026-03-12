CREATE TABLE users (
    id            BIGINT PRIMARY KEY,
    login         TEXT NOT NULL UNIQUE,
    name          TEXT,
    avatar_url    TEXT,
    bio           TEXT,
    followers     INT NOT NULL DEFAULT 0,
    following     INT NOT NULL DEFAULT 0,
    public_repos  INT NOT NULL DEFAULT 0,
    access_token  TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE repositories (
    id            BIGINT PRIMARY KEY,
    owner_id      BIGINT NOT NULL REFERENCES users(id),
    full_name     TEXT NOT NULL UNIQUE,
    description   TEXT,
    language      TEXT,
    stars         INT NOT NULL DEFAULT 0,
    forks         INT NOT NULL DEFAULT 0,
    open_issues   INT NOT NULL DEFAULT 0,
    watchers      INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE contributions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    year          INT NOT NULL,
    commits       INT NOT NULL DEFAULT 0,
    pull_requests INT NOT NULL DEFAULT 0,
    issues        INT NOT NULL DEFAULT 0,
    code_reviews  INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, year)
);

CREATE TABLE user_rankings (
    user_id       BIGINT PRIMARY KEY REFERENCES users(id),
    total_commits INT NOT NULL DEFAULT 0,
    total_prs     INT NOT NULL DEFAULT 0,
    total_issues  INT NOT NULL DEFAULT 0,
    total_reviews INT NOT NULL DEFAULT 0,
    total_stars   INT NOT NULL DEFAULT 0,
    total_forks   INT NOT NULL DEFAULT 0,
    score         DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank          INT,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE repo_rankings (
    repo_id       BIGINT PRIMARY KEY REFERENCES repositories(id),
    score         DOUBLE PRECISION NOT NULL DEFAULT 0,
    rank          INT,
    calculated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_rankings_score ON user_rankings(score DESC);
CREATE INDEX idx_user_rankings_rank ON user_rankings(rank);
CREATE INDEX idx_repo_rankings_score ON repo_rankings(score DESC);
CREATE INDEX idx_repo_rankings_rank ON repo_rankings(rank);
CREATE INDEX idx_repositories_language ON repositories(language);
