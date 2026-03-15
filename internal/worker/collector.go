package worker

import (
	"context"
	"log/slog"
	"time"

	"github.com/jbj338033/gitrank-api/internal/model"
	"github.com/jbj338033/gitrank-api/internal/repository"
	"github.com/jbj338033/gitrank-api/internal/service"
)

type Collector struct {
	userRepo    *repository.UserRepository
	contribRepo *repository.ContributionRepository
	repoRepo    *repository.RepoRepository
	streakRepo  *repository.StreakRepository
	ghService   *service.GitHubService
	authService *service.AuthService
	rankService *service.RankingService
}

func NewCollector(
	userRepo *repository.UserRepository,
	contribRepo *repository.ContributionRepository,
	repoRepo *repository.RepoRepository,
	streakRepo *repository.StreakRepository,
	ghService *service.GitHubService,
	authService *service.AuthService,
	rankService *service.RankingService,
) *Collector {
	return &Collector{
		userRepo:    userRepo,
		contribRepo: contribRepo,
		repoRepo:    repoRepo,
		streakRepo:  streakRepo,
		ghService:   ghService,
		authService: authService,
		rankService: rankService,
	}
}

func (c *Collector) Start(ctx context.Context) {
	ticker := time.NewTicker(6 * time.Hour)
	defer ticker.Stop()

	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			c.collect(ctx)
		}
	}
}

func (c *Collector) collect(ctx context.Context) {
	users, err := c.userRepo.ListAllWithToken(ctx)
	if err != nil {
		slog.Error("failed to list users", "error", err)
		return
	}

	for _, user := range users {
		if user.AccessToken == nil {
			continue
		}

		token, err := c.authService.DecryptToken(*user.AccessToken)
		if err != nil {
			slog.Error("failed to decrypt token", "user", user.Login, "error", err)
			continue
		}

		contributions, currentStreak, longestStreak, err := c.ghService.GetContributions(ctx, token, user.Login, user.GithubCreatedAt)
		if err != nil {
			slog.Error("failed to fetch contributions", "user", user.Login, "error", err)
			continue
		}

		if err := c.contribRepo.UpsertMany(ctx, user.ID, contributions); err != nil {
			slog.Error("failed to upsert contributions", "user", user.Login, "error", err)
			continue
		}

		if err := c.streakRepo.Upsert(ctx, user.ID, currentStreak, longestStreak); err != nil {
			slog.Error("failed to upsert streak", "user", user.Login, "error", err)
		}

		repos, err := c.ghService.GetRepositories(ctx, token, user.Login)
		if err != nil {
			slog.Error("failed to fetch repositories", "user", user.Login, "error", err)
			continue
		}

		var repoModels []model.Repository
		for _, r := range repos {
			repoModels = append(repoModels, model.Repository{
				ID:          r.ID,
				OwnerID:     user.ID,
				FullName:    r.FullName,
				Description: r.Description,
				Language:    r.Language,
				Stars:       r.Stars,
				Forks:       r.Forks,
				OpenIssues:  r.OpenIssues,
				Watchers:    r.Watchers,
			})
		}

		if err := c.repoRepo.UpsertMany(ctx, repoModels); err != nil {
			slog.Error("failed to upsert repositories", "user", user.Login, "error", err)
			continue
		}

		if err := c.rankService.RecalculateUser(ctx, user.ID); err != nil {
			slog.Error("failed to recalculate user ranking", "user", user.Login, "error", err)
		}

		if err := c.rankService.RecalculateUserRepos(ctx, user.ID); err != nil {
			slog.Error("failed to recalculate repo rankings", "user", user.Login, "error", err)
		}

		if err := c.userRepo.UpdateSyncedAt(ctx, user.ID); err != nil {
			slog.Error("failed to update synced_at", "user", user.Login, "error", err)
		}
	}

	slog.Info("collection complete", "users", len(users))
}
