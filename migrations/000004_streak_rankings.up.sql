CREATE TABLE streak_rankings (
    user_id         BIGINT PRIMARY KEY REFERENCES users(id),
    current_streak  INT NOT NULL DEFAULT 0,
    longest_streak  INT NOT NULL DEFAULT 0,
    calculated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_streak_rankings_current ON streak_rankings(current_streak DESC);
CREATE INDEX idx_streak_rankings_longest ON streak_rankings(longest_streak DESC);
