package repository

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/jbj338033/gitrank-api/internal/model"
)

type RepoRepository struct {
	pool *pgxpool.Pool
}

func NewRepoRepository(pool *pgxpool.Pool) *RepoRepository {
	return &RepoRepository{pool: pool}
}

func (r *RepoRepository) UpsertMany(ctx context.Context, repos []model.Repository) error {
	for _, repo := range repos {
		_, err := r.pool.Exec(ctx, `
			INSERT INTO repositories (id, owner_id, full_name, description, language, stars, forks, open_issues, watchers)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
			ON CONFLICT (id) DO UPDATE SET
				full_name = EXCLUDED.full_name,
				description = EXCLUDED.description,
				language = EXCLUDED.language,
				stars = EXCLUDED.stars,
				forks = EXCLUDED.forks,
				open_issues = EXCLUDED.open_issues,
				watchers = EXCLUDED.watchers,
				updated_at = NOW()`,
			repo.ID, repo.OwnerID, repo.FullName, repo.Description,
			repo.Language, repo.Stars, repo.Forks, repo.OpenIssues, repo.Watchers,
		)
		if err != nil {
			return err
		}
	}
	return nil
}

func (r *RepoRepository) GetByFullName(ctx context.Context, fullName string) (*model.Repository, error) {
	var repo model.Repository
	err := r.pool.QueryRow(ctx,
		`SELECT id, owner_id, full_name, description, language, stars, forks, open_issues, watchers, is_public
		 FROM repositories WHERE full_name = $1`, fullName,
	).Scan(&repo.ID, &repo.OwnerID, &repo.FullName, &repo.Description,
		&repo.Language, &repo.Stars, &repo.Forks, &repo.OpenIssues, &repo.Watchers, &repo.IsPublic)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &repo, err
}

func (r *RepoRepository) GetRanking(ctx context.Context, repoID int64) (*model.RepoRanking, error) {
	var rr model.RepoRanking
	err := r.pool.QueryRow(ctx,
		`SELECT repo_id, score, (SELECT COUNT(*)+1 FROM repo_rankings WHERE score > rr.score) AS rank, calculated_at
		 FROM repo_rankings rr WHERE rr.repo_id = $1`, repoID,
	).Scan(&rr.RepoID, &rr.Score, &rr.Rank, &rr.CalculatedAt)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &rr, err
}

func (r *RepoRepository) ListRanking(ctx context.Context, sort string, language *string, cursor *model.Cursor, limit int) ([]model.RepoRankingRow, error) {
	sortCol := "rr.score"
	if sort == "stars" {
		sortCol = "r.stars"
	} else if sort == "forks" {
		sortCol = "r.forks"
	}

	args := []any{limit}
	conditions := []string{"r.is_public = true"}

	if language != nil {
		args = append(args, *language)
		conditions = append(conditions, fmt.Sprintf("r.language = $%d", len(args)))
	}

	if cursor != nil {
		args = append(args, cursor.S, cursor.I)
		conditions = append(conditions, fmt.Sprintf("(%s, r.id) < ($%d, $%d)", sortCol, len(args)-1, len(args)))
	}

	where := ""
	if len(conditions) > 0 {
		where = "WHERE "
		for i, c := range conditions {
			if i > 0 {
				where += " AND "
			}
			where += c
		}
	}

	query := fmt.Sprintf(`
		SELECT (SELECT COUNT(*)+1 FROM repo_rankings WHERE score > rr.score) AS rank,
			   r.full_name, r.description, r.language, r.stars, r.forks, r.watchers,
			   rr.score, u.login, u.avatar_url, r.id
		FROM repo_rankings rr
		JOIN repositories r ON rr.repo_id = r.id
		JOIN users u ON r.owner_id = u.id
		%s
		ORDER BY %s DESC, r.id DESC
		LIMIT $1`, where, sortCol)

	rows, err := r.pool.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var results []model.RepoRankingRow
	for rows.Next() {
		var row model.RepoRankingRow
		if err := rows.Scan(&row.Rank, &row.FullName, &row.Description, &row.Language,
			&row.Stars, &row.Forks, &row.Watchers, &row.Score,
			&row.Owner.Login, &row.Owner.AvatarURL, &row.RepoID); err != nil {
			return nil, err
		}
		results = append(results, row)
	}
	return results, rows.Err()
}

func (r *RepoRepository) TogglePublic(ctx context.Context, repoID int64, isPublic bool) error {
	_, err := r.pool.Exec(ctx, `UPDATE repositories SET is_public = $2 WHERE id = $1`, repoID, isPublic)
	return err
}

func (r *RepoRepository) ListByOwner(ctx context.Context, ownerID int64) ([]model.RepoSummary, error) {
	rows, err := r.pool.Query(ctx,
		`SELECT full_name, description, language, stars, forks, is_public
		 FROM repositories WHERE owner_id = $1 ORDER BY stars DESC`, ownerID)
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

func (r *RepoRepository) GetOwner(ctx context.Context, ownerID int64) (*model.OwnerDetail, error) {
	var o model.OwnerDetail
	err := r.pool.QueryRow(ctx,
		`SELECT login, name, avatar_url FROM users WHERE id = $1`, ownerID,
	).Scan(&o.Login, &o.Name, &o.AvatarURL)
	if err == pgx.ErrNoRows {
		return nil, nil
	}
	return &o, err
}
