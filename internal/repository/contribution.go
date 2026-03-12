package repository

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jbj338033/gitrank-api/internal/model"
)

type ContributionRepository struct {
	pool *pgxpool.Pool
}

func NewContributionRepository(pool *pgxpool.Pool) *ContributionRepository {
	return &ContributionRepository{pool: pool}
}

func (r *ContributionRepository) UpsertMany(ctx context.Context, userID int64, contributions []model.ContributionYear) error {
	for _, c := range contributions {
		_, err := r.pool.Exec(ctx, `
			INSERT INTO contributions (user_id, year, commits, pull_requests, issues, code_reviews)
			VALUES ($1, $2, $3, $4, $5, $6)
			ON CONFLICT (user_id, year) DO UPDATE SET
				commits = EXCLUDED.commits,
				pull_requests = EXCLUDED.pull_requests,
				issues = EXCLUDED.issues,
				code_reviews = EXCLUDED.code_reviews,
				updated_at = NOW()`,
			userID, c.Year, c.Commits, c.PullRequests, c.Issues, c.CodeReviews,
		)
		if err != nil {
			return err
		}
	}
	return nil
}

func (r *ContributionRepository) GetByUser(ctx context.Context, userID int64) ([]model.ContributionYear, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT year, commits, pull_requests, issues, code_reviews
		 FROM contributions WHERE user_id = $1 ORDER BY year DESC`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var results []model.ContributionYear
	for rows.Next() {
		var c model.ContributionYear
		if err := rows.Scan(&c.Year, &c.Commits, &c.PullRequests, &c.Issues, &c.CodeReviews); err != nil {
			return nil, err
		}
		results = append(results, c)
	}
	return results, rows.Err()
}
