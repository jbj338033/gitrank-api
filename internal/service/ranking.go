package service

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

type RankingService struct {
	pool *pgxpool.Pool
}

func NewRankingService(pool *pgxpool.Pool) *RankingService {
	return &RankingService{pool: pool}
}

func (s *RankingService) RecalculateUser(ctx context.Context, userID int64) error {
	query := `
		INSERT INTO user_rankings (user_id, total_commits, total_prs, total_issues, total_reviews, total_stars, total_forks, score, calculated_at)
		SELECT
			$1,
			COALESCE(SUM(c.commits), 0),
			COALESCE(SUM(c.pull_requests), 0),
			COALESCE(SUM(c.issues), 0),
			COALESCE(SUM(c.code_reviews), 0),
			COALESCE((SELECT SUM(r.stars) FROM repositories r WHERE r.owner_id = $1 AND r.is_public = true), 0),
			COALESCE((SELECT SUM(r.forks) FROM repositories r WHERE r.owner_id = $1 AND r.is_public = true), 0),
			COALESCE(SUM(c.commits), 0) * 1
				+ COALESCE(SUM(c.pull_requests), 0) * 3
				+ COALESCE(SUM(c.issues), 0) * 1.5
				+ COALESCE(SUM(c.code_reviews), 0) * 2
				+ COALESCE((SELECT SUM(r.stars) FROM repositories r WHERE r.owner_id = $1 AND r.is_public = true), 0) * 0.5
				+ COALESCE((SELECT SUM(r.forks) FROM repositories r WHERE r.owner_id = $1 AND r.is_public = true), 0) * 0.3,
			NOW()
		FROM contributions c
		WHERE c.user_id = $1
		ON CONFLICT (user_id) DO UPDATE SET
			total_commits = EXCLUDED.total_commits,
			total_prs = EXCLUDED.total_prs,
			total_issues = EXCLUDED.total_issues,
			total_reviews = EXCLUDED.total_reviews,
			total_stars = EXCLUDED.total_stars,
			total_forks = EXCLUDED.total_forks,
			score = EXCLUDED.score,
			calculated_at = NOW()`

	_, err := s.pool.Exec(ctx, query, userID)
	return err
}

func (s *RankingService) RecalculateUserRepos(ctx context.Context, userID int64) error {
	_, err := s.pool.Exec(ctx, `
		DELETE FROM repo_rankings
		WHERE repo_id IN (SELECT id FROM repositories WHERE owner_id = $1 AND is_public = false)`, userID)
	if err != nil {
		return err
	}

	query := `
		INSERT INTO repo_rankings (repo_id, score, calculated_at)
		SELECT
			r.id,
			r.stars * 1 + r.forks * 2 + r.watchers * 0.5,
			NOW()
		FROM repositories r
		WHERE r.owner_id = $1 AND r.is_public = true
		ON CONFLICT (repo_id) DO UPDATE SET
			score = EXCLUDED.score,
			calculated_at = NOW()`

	_, err = s.pool.Exec(ctx, query, userID)
	return err
}

func (s *RankingService) RecalculateAllRanks(ctx context.Context) error {
	_, err := s.pool.Exec(ctx, `
		UPDATE user_rankings SET rank = sub.r
		FROM (SELECT user_id, RANK() OVER (ORDER BY score DESC) AS r FROM user_rankings) sub
		WHERE user_rankings.user_id = sub.user_id`)
	if err != nil {
		return err
	}

	_, err = s.pool.Exec(ctx, `
		UPDATE repo_rankings SET rank = sub.r
		FROM (SELECT repo_id, RANK() OVER (ORDER BY score DESC) AS r FROM repo_rankings) sub
		WHERE repo_rankings.repo_id = sub.repo_id`)
	return err
}
