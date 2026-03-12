package handler

import (
	"encoding/base64"
	"encoding/json"
	"net/http"

	"github.com/jbj338033/gitrank-api/internal/model"
	"github.com/jbj338033/gitrank-api/internal/repository"
	"github.com/jbj338033/gitrank-api/internal/service"
	"github.com/labstack/echo/v5"
)

type RepoHandler struct {
	repoRepo    *repository.RepoRepository
	userRepo    *repository.UserRepository
	rankService *service.RankingService
}

func NewRepoHandler(repoRepo *repository.RepoRepository, userRepo *repository.UserRepository, rankService *service.RankingService) *RepoHandler {
	return &RepoHandler{repoRepo: repoRepo, userRepo: userRepo, rankService: rankService}
}

func (h *RepoHandler) Ranking(c *echo.Context) error {
	limit, _ := echo.QueryParamOr[int](c, "limit", 20)
	if limit > 100 {
		limit = 100
	}
	if limit < 1 {
		limit = 1
	}

	sort := c.QueryParam("sort")
	validSorts := map[string]bool{"score": true, "stars": true, "forks": true}
	if !validSorts[sort] {
		sort = "score"
	}

	var language *string
	if lang := c.QueryParam("language"); lang != "" {
		language = &lang
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

	rows, err := h.repoRepo.ListRanking(c.Request().Context(), sort, language, cursor, limit+1)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to fetch rankings"})
	}

	hasNext := len(rows) > limit
	if hasNext {
		rows = rows[:limit]
	}

	var nextCursor *string
	if hasNext && len(rows) > 0 {
		last := rows[len(rows)-1]
		cur := model.Cursor{S: last.Score, I: last.RepoID}
		if sort == "stars" {
			cur.S = float64(last.Stars)
		} else if sort == "forks" {
			cur.S = float64(last.Forks)
		}
		data, _ := json.Marshal(cur)
		encoded := base64.URLEncoding.EncodeToString(data)
		nextCursor = &encoded
	}

	if rows == nil {
		rows = []model.RepoRankingRow{}
	}

	return c.JSON(http.StatusOK, map[string]any{
		"repositories": rows,
		"next_cursor":  nextCursor,
		"has_next":     hasNext,
	})
}

func (h *RepoHandler) Detail(c *echo.Context) error {
	owner := c.Param("owner")
	repo := c.Param("repo")
	fullName := owner + "/" + repo

	repository, err := h.repoRepo.GetByFullName(c.Request().Context(), fullName)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to fetch repository"})
	}
	if repository == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse{Code: "not_found", Message: "repository not found"})
	}

	ranking, _ := h.repoRepo.GetRanking(c.Request().Context(), repository.ID)
	ownerDetail, _ := h.repoRepo.GetOwner(c.Request().Context(), repository.OwnerID)

	detail := model.RepoDetail{
		ID:          repository.ID,
		FullName:    repository.FullName,
		Description: repository.Description,
		Language:    repository.Language,
		Stars:       repository.Stars,
		Forks:       repository.Forks,
		OpenIssues:  repository.OpenIssues,
		Watchers:    repository.Watchers,
	}

	if ranking != nil {
		detail.Ranking = &model.RepoRankingInfo{
			Rank:  ranking.Rank,
			Score: ranking.Score,
		}
	}

	if ownerDetail != nil {
		detail.Owner = *ownerDetail
	}

	return c.JSON(http.StatusOK, detail)
}

func (h *RepoHandler) ToggleVisibility(c *echo.Context) error {
	owner := c.Param("owner")
	repo := c.Param("repo")
	fullName := owner + "/" + repo

	var body struct {
		IsPublic bool `json:"is_public"`
	}
	if err := c.Bind(&body); err != nil {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse{Code: "bad_request", Message: "invalid request body"})
	}

	ctx := c.Request().Context()

	repository, err := h.repoRepo.GetByFullName(ctx, fullName)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to fetch repository"})
	}
	if repository == nil {
		return c.JSON(http.StatusNotFound, model.ErrorResponse{Code: "not_found", Message: "repository not found"})
	}

	userID, _ := echo.ContextGet[int64](c, "user_id")
	if repository.OwnerID != userID {
		return c.JSON(http.StatusForbidden, model.ErrorResponse{Code: "forbidden", Message: "can only modify your own repositories"})
	}

	if err := h.repoRepo.TogglePublic(ctx, repository.ID, body.IsPublic); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "db_error", Message: "failed to update visibility"})
	}

	if err := h.rankService.RecalculateUser(ctx, userID); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "rank_error", Message: "failed to recalculate user ranking"})
	}
	if err := h.rankService.RecalculateUserRepos(ctx, userID); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "rank_error", Message: "failed to recalculate repo rankings"})
	}

	return c.JSON(http.StatusOK, map[string]any{"is_public": body.IsPublic})
}
