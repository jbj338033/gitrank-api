package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jbj338033/gitrank-api/internal/model"
)

type UserRepository struct {
	pool *pgxpool.Pool
}

func NewUserRepository(pool *pgxpool.Pool) *UserRepository {
	return &UserRepository{pool: pool}
}

func (r *UserRepository) Upsert(ctx context.Context, u *model.User) error {
	query := `
		INSERT INTO users (id, login, name, avatar_url, bio, followers, following, public_repos, access_token)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
		ON CONFLICT (id) DO UPDATE SET
			login = EXCLUDED.login,
			name = EXCLUDED.name,
			avatar_url = EXCLUDED.avatar_url,
			bio = EXCLUDED.bio,
			followers = EXCLUDED.followers,
			following = EXCLUDED.following,
			public_repos = EXCLUDED.public_repos,
			access_token = EXCLUDED.access_token,
			updated_at = NOW()`

	_, err := r.pool.Exec(ctx, query,
		u.ID, u.Login, u.Name, u.AvatarURL, u.Bio,
		u.Followers, u.Following, u.PublicRepos, u.AccessToken,
	)
	return err
}

func (r *UserRepository) GetByLogin(ctx context.Context, login string) (*model.User, error) {
	var u model.User
	err := r.pool.QueryRow(ctx,
		`SELECT id, login, name, avatar_url, bio, followers, following, public_repos, access_token, created_at, updated_at
		 FROM users WHERE login = $1`, login,
	).Scan(&u.ID, &u.Login, &u.Name, &u.AvatarURL, &u.Bio,
		&u.Followers, &u.Following, &u.PublicRepos, &u.AccessToken,
		&u.CreatedAt, &u.UpdatedAt)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &u, err
}

func (r *UserRepository) GetByID(ctx context.Context, id int64) (*model.User, error) {
	var u model.User
	err := r.pool.QueryRow(ctx,
		`SELECT id, login, name, avatar_url, bio, followers, following, public_repos, access_token, created_at, updated_at
		 FROM users WHERE id = $1`, id,
	).Scan(&u.ID, &u.Login, &u.Name, &u.AvatarURL, &u.Bio,
		&u.Followers, &u.Following, &u.PublicRepos, &u.AccessToken,
		&u.CreatedAt, &u.UpdatedAt)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &u, err
}

func (r *UserRepository) UpdateSyncedAt(ctx context.Context, id int64) error {
	_, err := r.pool.Exec(ctx, `UPDATE users SET updated_at = NOW() WHERE id = $1`, id)
	return err
}

func (r *UserRepository) GetRanking(ctx context.Context, userID int64) (*model.UserRanking, error) {
	var ur model.UserRanking
	err := r.pool.QueryRow(ctx,
		`SELECT user_id, total_commits, total_prs, total_issues, total_reviews, total_stars, total_forks, score, rank, calculated_at
		 FROM user_rankings WHERE user_id = $1`, userID,
	).Scan(&ur.UserID, &ur.TotalCommits, &ur.TotalPRs, &ur.TotalIssues,
		&ur.TotalReviews, &ur.TotalStars, &ur.TotalForks, &ur.Score,
		&ur.Rank, &ur.CalculatedAt)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &ur, err
}

var validSortColumns = map[string]string{
	"score":   "ur.score",
	"commits": "ur.total_commits",
	"prs":     "ur.total_prs",
	"issues":  "ur.total_issues",
	"reviews": "ur.total_reviews",
	"stars":   "ur.total_stars",
	"forks":   "ur.total_forks",
}

func (r *UserRepository) ListRanking(ctx context.Context, sort string, cursor *model.Cursor, limit int) ([]model.UserRankingRow, error) {
	col, ok := validSortColumns[sort]
	if !ok {
		col = "ur.score"
	}

	args := []any{limit}
	where := ""
	if cursor != nil {
		where = fmt.Sprintf("WHERE (%s, u.id) < ($2, $3)", col)
		args = append(args, cursor.S, cursor.I)
	}

	query := fmt.Sprintf(`
		SELECT ur.rank, u.login, u.name, u.avatar_url, ur.score,
			   ur.total_commits, ur.total_prs, ur.total_issues, ur.total_reviews,
			   ur.total_stars, ur.total_forks, u.id
		FROM user_rankings ur
		JOIN users u ON ur.user_id = u.id
		%s
		ORDER BY %s DESC, u.id DESC
		LIMIT $1`, where, col)

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var results []model.UserRankingRow
	for rows.Next() {
		var row model.UserRankingRow
		if err := rows.Scan(&row.Rank, &row.Login, &row.Name, &row.AvatarURL, &row.Score,
			&row.TotalCommits, &row.TotalPRs, &row.TotalIssues, &row.TotalReviews,
			&row.TotalStars, &row.TotalForks, &row.UserID); err != nil {
			return nil, err
		}
		results = append(results, row)
	}
	return results, rows.Err()
}

func (r *UserRepository) GetTopRepos(ctx context.Context, userID int64, limit int) ([]model.RepoSummary, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT full_name, description, language, stars, forks, is_public
		 FROM repositories WHERE owner_id = $1 AND is_public = true ORDER BY stars DESC LIMIT $2`, userID, limit)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var repos []model.RepoSummary
	for rows.Next() {
		var repo model.RepoSummary
		if err := rows.Scan(&repo.FullName, &repo.Description, &repo.Language, &repo.Stars, &repo.Forks, &repo.IsPublic); err != nil {
			return nil, err
		}
		repos = append(repos, repo)
	}
	return repos, rows.Err()
}

func (r *UserRepository) ListAllWithToken(ctx context.Context) ([]model.User, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT id, login, name, avatar_url, bio, followers, following, public_repos, access_token, created_at, updated_at
		 FROM users WHERE access_token IS NOT NULL ORDER BY updated_at ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var users []model.User
	for rows.Next() {
		var u model.User
		if err := rows.Scan(&u.ID, &u.Login, &u.Name, &u.AvatarURL, &u.Bio,
			&u.Followers, &u.Following, &u.PublicRepos, &u.AccessToken,
			&u.CreatedAt, &u.UpdatedAt); err != nil {
			return nil, err
		}
		users = append(users, u)
	}
	return users, rows.Err()
}
