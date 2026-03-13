package repository

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jbj338033/gitrank-api/internal/model"
)

type StreakRepository struct {
	pool *pgxpool.Pool
}

func NewStreakRepository(pool *pgxpool.Pool) *StreakRepository {
	return &StreakRepository{pool: pool}
}

func (r *StreakRepository) Upsert(ctx context.Context, userID int64, currentStreak, longestStreak int) error {
	_, err := r.pool.Exec(ctx, `
		INSERT INTO streak_rankings (user_id, current_streak, longest_streak, calculated_at)
		VALUES ($1, $2, $3, NOW())
		ON CONFLICT (user_id) DO UPDATE SET
			current_streak = EXCLUDED.current_streak,
			longest_streak = EXCLUDED.longest_streak,
			calculated_at = NOW()`,
		userID, currentStreak, longestStreak)
	return err
}

func (r *StreakRepository) GetByUser(ctx context.Context, userID int64) (*model.StreakInfo, error) {
	var info model.StreakInfo
	err := r.pool.QueryRow(ctx, `
		SELECT current_streak, longest_streak,
			(SELECT COUNT(*)+1 FROM streak_rankings WHERE current_streak > sr.current_streak) AS rank
		FROM streak_rankings sr WHERE sr.user_id = $1`, userID,
	).Scan(&info.CurrentStreak, &info.LongestStreak, &info.Rank)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &info, err
}
