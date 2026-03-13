package handler

import (
	"encoding/base64"
	"encoding/json"
	"net/http"
	"time"

	"github.com/jbj338033/gitrank-api/internal/model"
	"github.com/jbj338033/gitrank-api/internal/repository"
	"github.com/jbj338033/gitrank-api/internal/service"
	"github.com/labstack/echo/v5"
)

type UserHandler struct {
	userRepo    *repository.UserRepository
	contribRepo *repository.ContributionRepository
	repoRepo    *repository.RepoRepository
	streakRepo  *repository.StreakRepository
	ghService   *service.GitHubService
	authService *service.AuthService
	rankService *service.RankingService
}

func NewUserHandler(
	userRepo *repository.UserRepository,
	contribRepo *repository.ContributionRepository,
	repoRepo *repository.RepoRepository,
	streakRepo *repository.StreakRepository,
	ghService *service.GitHubService,
	authService *service.AuthService,
	rankService *service.RankingService,
) *UserHandler {
	return &UserHandler{
		userRepo:    userRepo,
		contribRepo: contribRepo,
		repoRepo:    repoRepo,
		streakRepo:  streakRepo,
		ghService:   ghService,
		authService: authService,
		rankService: rankService,
	}
}

func (h *UserHandler) Me(c *echo.Context) error {
	userID, _ := echo.ContextGet[int64](c, "user_id")
	user, err := h.userRepo.GetByID(c.Request().Context(), userID)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to get user"})
	}
	if user == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse{Code: "user_not_found", Message: "user not found"})
	}

	return c.JSON(http.StatusOK, map[string]any{
		"id":           user.ID,
		"login":        user.Login,
		"name":         user.Name,
		"avatar_url":   user.AvatarURL,
		"bio":          user.Bio,
		"followers":    user.Followers,
		"following":    user.Following,
		"public_repos": user.PublicRepos,
	})
}

func (h *UserHandler) Ranking(c *echo.Context) error {
	limit, _ := echo.QueryParamOr[int](c, "limit", 20)
	if limit > 100 {
		limit = 100
	}
	if limit < 1 {
		limit = 1
	}

	sort := c.QueryParam("sort")
	validSorts := map[string]bool{"score": true, "commits": true, "prs": true, "issues": true, "reviews": true, "stars": true, "forks": true, "current_streak": true, "longest_streak": true}
	if !validSorts[sort] {
		sort = "score"
	}

	var cursor *model.Cursor
	if cursorStr := c.QueryParam("cursor"); cursorStr != "" {
		data, err := base64.URLEncoding.DecodeString(cursorStr)
		if err == nil {
			var cur model.Cursor
			if json.Unmarshal(data, &cur) == nil {
				cursor = &cur
			}
		}
	}

	rows, err := h.userRepo.ListRanking(c.Request().Context(), sort, cursor, limit+1)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to fetch rankings"})
	}
	if rows == nil {
		rows = []model.UserRankingRow{}
	}

	hasNext := len(rows) > limit
	if hasNext {
		rows = rows[:limit]
	}

	var nextCursor *string
	if hasNext && len(rows) > 0 {
		last := rows[len(rows)-1]
		sortValue := getSortValue(last, sort)
		cur := model.Cursor{S: sortValue, I: last.UserID}
		data, _ := json.Marshal(cur)
		encoded := base64.URLEncoding.EncodeToString(data)
		nextCursor = &encoded
	}

	return c.JSON(http.StatusOK, map[string]any{
		"users":       rows,
		"next_cursor": nextCursor,
		"has_next":    hasNext,
	})
}

func getSortValue(row model.UserRankingRow, sort string) float64 {
	switch sort {
	case "commits":
		return float64(row.TotalCommits)
	case "prs":
		return float64(row.TotalPRs)
	case "issues":
		return float64(row.TotalIssues)
	case "reviews":
		return float64(row.TotalReviews)
	case "stars":
		return float64(row.TotalStars)
	case "forks":
		return float64(row.TotalForks)
	case "current_streak":
		return float64(row.CurrentStreak)
	case "longest_streak":
		return float64(row.LongestStreak)
	default:
		return row.Score
	}
}

func (h *UserHandler) Detail(c *echo.Context) error {
	username := c.Param("username")
	user, err := h.userRepo.GetByLogin(c.Request().Context(), username)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to fetch user"})
	}
	if user == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse{Code: "not_found", Message: "user not found"})
	}

	ranking, _ := h.userRepo.GetRanking(c.Request().Context(), user.ID)
	contributions, _ := h.contribRepo.GetByUser(c.Request().Context(), user.ID)
	topRepos, _ := h.userRepo.GetTopRepos(c.Request().Context(), user.ID, 10)
	streak, _ := h.streakRepo.GetByUser(c.Request().Context(), user.ID)

	if contributions == nil {
		contributions = []model.ContributionYear{}
	}
	if topRepos == nil {
		topRepos = []model.RepoSummary{}
	}

	detail := model.UserDetail{
		ID:            user.ID,
		Login:         user.Login,
		Name:          user.Name,
		AvatarURL:     user.AvatarURL,
		Bio:           user.Bio,
		Followers:     user.Followers,
		Following:     user.Following,
		PublicRepos:   user.PublicRepos,
		Streak:        streak,
		Contributions: contributions,
		TopRepos:      topRepos,
		SyncedAt:      &user.UpdatedAt,
	}

	if ranking != nil {
		detail.Ranking = &model.UserRankingInfo{
			Rank:         ranking.Rank,
			Score:        ranking.Score,
			TotalCommits: ranking.TotalCommits,
			TotalPRs:     ranking.TotalPRs,
			TotalIssues:  ranking.TotalIssues,
			TotalReviews: ranking.TotalReviews,
			TotalStars:   ranking.TotalStars,
			TotalForks:   ranking.TotalForks,
		}
	}

	return c.JSON(http.StatusOK, detail)
}

func (h *UserHandler) MyRepos(c *echo.Context) error {
	userID, _ := echo.ContextGet[int64](c, "user_id")
	repos, err := h.repoRepo.ListByOwner(c.Request().Context(), userID)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to fetch repositories"})
	}
	if repos == nil {
		repos = []model.RepoSummary{}
	}
	return c.JSON(http.StatusOK, repos)
}

func (h *UserHandler) Sync(c *echo.Context) error {
	username := c.Param("username")
	login, _ := echo.ContextGet[string](c, "login")
	if login != username {
		return c.JSON(http.StatusForbidden, model.ErrorResponse{Code: "forbidden", Message: "can only sync your own profile"})
	}

	userID, _ := echo.ContextGet[int64](c, "user_id")
	user, err := h.userRepo.GetByID(c.Request().Context(), userID)
	if err != nil || user == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse{Code: "not_found", Message: "user not found"})
	}

	if time.Since(user.UpdatedAt) < 5*time.Minute {
		return c.JSON(http.StatusTooManyRequests, model.ErrorResponse{Code: "cooldown", Message: "sync available every 5 minutes"})
	}

	if user.AccessToken == nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse{Code: "no_token", Message: "no github token stored"})
	}

	ghToken, err := h.authService.DecryptToken(*user.AccessToken)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "decrypt_error", Message: "failed to decrypt token"})
	}

	ctx := c.Request().Context()

	contributions, currentStreak, longestStreak, err := h.ghService.GetContributions(ctx, ghToken, user.Login)
	if err != nil {
		return c.JSON(http.StatusBadGateway, model.ErrorResponse{Code: "github_error", Message: "failed to fetch contributions"})
	}

	repos, err := h.ghService.GetRepositories(ctx, ghToken, user.Login)
	if err != nil {
		return c.JSON(http.StatusBadGateway, model.ErrorResponse{Code: "github_error", Message: "failed to fetch repositories"})
	}

	if err := h.contribRepo.UpsertMany(ctx, userID, contributions); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to save contributions"})
	}

	if err := h.streakRepo.Upsert(ctx, userID, currentStreak, longestStreak); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to save streak"})
	}

	var repoModels []model.Repository
	for _, r := range repos {
		repoModels = append(repoModels, model.Repository{
			ID:          r.ID,
			OwnerID:     userID,
			FullName:    r.FullName,
			Description: r.Description,
			Language:    r.Language,
			Stars:       r.Stars,
			Forks:       r.Forks,
			OpenIssues:  r.OpenIssues,
			Watchers:    r.Watchers,
		})
	}

	if err := h.repoRepo.UpsertMany(ctx, repoModels); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to save repositories"})
	}

	if err := h.rankService.RecalculateUser(ctx, userID); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "rank_error", Message: "failed to recalculate user ranking"})
	}

	if err := h.rankService.RecalculateUserRepos(ctx, userID); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "rank_error", Message: "failed to recalculate repo rankings"})
	}

	if err := h.userRepo.UpdateSyncedAt(ctx, userID); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to update synced time"})
	}

	now := time.Now()
	return c.JSON(http.StatusOK, map[string]any{
		"synced_at": now,
	})
}
