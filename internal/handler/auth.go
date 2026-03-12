package handler

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/jbj338033/gitrank-api/internal/config"
	"github.com/jbj338033/gitrank-api/internal/model"
	"github.com/jbj338033/gitrank-api/internal/repository"
	"github.com/jbj338033/gitrank-api/internal/service"
	"github.com/labstack/echo/v5"
)

type AuthHandler struct {
	cfg         *config.Config
	authService *service.AuthService
	ghService   *service.GitHubService
	userRepo    *repository.UserRepository
	contribRepo *repository.ContributionRepository
	repoRepo    *repository.RepoRepository
	rankService *service.RankingService
}

func NewAuthHandler(
	cfg *config.Config,
	authService *service.AuthService,
	ghService *service.GitHubService,
	userRepo *repository.UserRepository,
	contribRepo *repository.ContributionRepository,
	repoRepo *repository.RepoRepository,
	rankService *service.RankingService,
) *AuthHandler {
	return &AuthHandler{
		cfg:         cfg,
		authService: authService,
		ghService:   ghService,
		userRepo:    userRepo,
		contribRepo: contribRepo,
		repoRepo:    repoRepo,
		rankService: rankService,
	}
}

func sseEvent(w http.ResponseWriter, f http.Flusher, event string, data any) {
	b, _ := json.Marshal(data)
	fmt.Fprintf(w, "event: %s\ndata: %s\n\n", event, b)
	f.Flush()
}

func (h *AuthHandler) State(c *echo.Context) error {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to generate state"})
	}
	state := base64.URLEncoding.EncodeToString(b)

	c.SetCookie(&http.Cookie{
		Name:     "oauth_state",
		Value:    state,
		Path:     "/api/auth",
		MaxAge:   600,
		HttpOnly: true,
		SameSite: http.SameSiteNoneMode,
		Secure:   true,
	})

	return c.JSON(http.StatusOK, map[string]string{"state": state})
}

func (h *AuthHandler) Login(c *echo.Context) error {
	var body struct {
		Code  string `json:"code"`
		State string `json:"state"`
	}
	if err := c.Bind(&body); err != nil || body.Code == "" {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse{Code: "bad_request", Message: "missing authorization code"})
	}

	stateCookie, err := c.Cookie("oauth_state")
	if err != nil || stateCookie.Value == "" || subtle.ConstantTimeCompare([]byte(stateCookie.Value), []byte(body.State)) != 1 {
		return c.JSON(http.StatusBadRequest, model.ErrorResponse{Code: "invalid_state", Message: "invalid or missing oauth state"})
	}

	c.SetCookie(&http.Cookie{Name: "oauth_state", Value: "", Path: "/api/auth", MaxAge: -1})

	token, err := h.ghService.ExchangeCode(c.Request().Context(), h.cfg.GitHubClientID, h.cfg.GitHubSecret, body.Code)
	if err != nil {
		return c.JSON(http.StatusBadGateway, model.ErrorResponse{Code: "oauth_failed", Message: "failed to exchange code"})
	}

	ghUser, err := h.ghService.GetUser(c.Request().Context(), token)
	if err != nil {
		return c.JSON(http.StatusBadGateway, model.ErrorResponse{Code: "github_error", Message: "failed to get user info"})
	}

	encrypted, err := h.authService.EncryptToken(token)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to encrypt token"})
	}

	u := &model.User{
		ID:          ghUser.ID,
		Login:       ghUser.Login,
		Name:        ghUser.Name,
		AvatarURL:   ghUser.AvatarURL,
		Bio:         ghUser.Bio,
		Followers:   ghUser.Followers,
		Following:   ghUser.Following,
		PublicRepos: ghUser.PublicRepos,
		AccessToken: &encrypted,
	}
	if err := h.userRepo.Upsert(c.Request().Context(), u); err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to save user"})
	}

	accessToken, err := h.authService.GenerateAccessToken(ghUser.ID, ghUser.Login)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to generate token"})
	}

	refreshToken, err := h.authService.GenerateRefreshToken(ghUser.ID, ghUser.Login)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to generate token"})
	}

	c.SetCookie(&http.Cookie{
		Name:     "access_token",
		Value:    accessToken,
		Path:     "/",
		MaxAge:   3600,
		HttpOnly: true,
		SameSite: http.SameSiteNoneMode,
		Secure:   true,
	})
	c.SetCookie(&http.Cookie{
		Name:     "refresh_token",
		Value:    refreshToken,
		Path:     "/api/auth",
		MaxAge:   604800,
		HttpOnly: true,
		SameSite: http.SameSiteNoneMode,
		Secure:   true,
	})

	w := c.Response()
	f, ok := w.(http.Flusher)
	if !ok {
		return c.JSON(http.StatusOK, map[string]any{
			"id": ghUser.ID, "login": ghUser.Login, "name": ghUser.Name, "avatar_url": ghUser.AvatarURL,
		})
	}

	w.Header().Set("Content-Type", "text/event-stream")
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("Connection", "keep-alive")
	w.WriteHeader(http.StatusOK)

	type statusMsg struct {
		Step    string `json:"step"`
		Message string `json:"message"`
	}

	sseEvent(w, f, "status", statusMsg{Step: "auth", Message: "GitHub 인증 완료"})

	ctx := c.Request().Context()
	now := time.Now()
	currentYear := now.Year()

	for y := currentYear; y >= currentYear-4; y-- {
		sseEvent(w, f, "status", statusMsg{Step: "contributions", Message: fmt.Sprintf("%d년 기여 내역 수집 중...", y)})

		cy, err := h.ghService.GetContributionsByYear(ctx, token, ghUser.Login, y)
		if err != nil {
			sseEvent(w, f, "error", map[string]string{"step": "contributions", "message": "failed to fetch contributions"})
			return h.finishSSE(w, f, ghUser)
		}

		if err := h.contribRepo.UpsertMany(ctx, ghUser.ID, []model.ContributionYear{*cy}); err != nil {
			sseEvent(w, f, "error", map[string]string{"step": "contributions", "message": "failed to save contributions"})
			return h.finishSSE(w, f, ghUser)
		}
	}

	sseEvent(w, f, "status", statusMsg{Step: "repositories", Message: "레포지토리 수집 중..."})

	repos, err := h.ghService.GetRepositories(ctx, token, ghUser.Login)
	if err != nil {
		sseEvent(w, f, "error", map[string]string{"step": "repositories", "message": "failed to fetch repositories"})
		return h.finishSSE(w, f, ghUser)
	}

	var repoModels []model.Repository
	for _, r := range repos {
		repoModels = append(repoModels, model.Repository{
			ID:          r.ID,
			OwnerID:     ghUser.ID,
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
		sseEvent(w, f, "error", map[string]string{"step": "repositories", "message": "failed to save repositories"})
		return h.finishSSE(w, f, ghUser)
	}

	sseEvent(w, f, "status", statusMsg{Step: "ranking", Message: "랭킹 계산 중..."})

	_ = h.rankService.RecalculateUser(ctx, ghUser.ID)
	_ = h.rankService.RecalculateUserRepos(ctx, ghUser.ID)
	_ = h.userRepo.UpdateSyncedAt(ctx, ghUser.ID)

	return h.finishSSE(w, f, ghUser)
}

func (h *AuthHandler) finishSSE(w http.ResponseWriter, f http.Flusher, ghUser *service.GitHubUser) error {
	sseEvent(w, f, "done", map[string]any{
		"id": ghUser.ID, "login": ghUser.Login, "name": ghUser.Name, "avatar_url": ghUser.AvatarURL,
	})
	return nil
}

func (h *AuthHandler) Logout(c *echo.Context) error {
	c.SetCookie(&http.Cookie{Name: "access_token", Value: "", Path: "/", MaxAge: -1})
	c.SetCookie(&http.Cookie{Name: "refresh_token", Value: "", Path: "/api/auth", MaxAge: -1})
	return c.NoContent(http.StatusNoContent)
}

func (h *AuthHandler) Refresh(c *echo.Context) error {
	cookie, err := c.Cookie("refresh_token")
	if err != nil {
		return c.JSON(http.StatusUnauthorized, model.ErrorResponse{Code: "unauthorized", Message: "missing refresh token"})
	}

	userID, login, err := h.authService.ParseToken(cookie.Value, "refresh")
	if err != nil {
		return c.JSON(http.StatusUnauthorized, model.ErrorResponse{Code: "invalid_refresh_token", Message: "refresh token expired or invalid"})
	}

	accessToken, err := h.authService.GenerateAccessToken(userID, login)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to generate token"})
	}

	refreshToken, err := h.authService.GenerateRefreshToken(userID, login)
	if err != nil {
		return c.JSON(http.StatusInternalServerError, model.ErrorResponse{Code: "internal_error", Message: "failed to generate token"})
	}

	c.SetCookie(&http.Cookie{
		Name:     "access_token",
		Value:    accessToken,
		Path:     "/",
		MaxAge:   3600,
		HttpOnly: true,
		SameSite: http.SameSiteNoneMode,
		Secure:   true,
	})
	c.SetCookie(&http.Cookie{
		Name:     "refresh_token",
		Value:    refreshToken,
		Path:     "/api/auth",
		MaxAge:   604800,
		HttpOnly: true,
		SameSite: http.SameSiteNoneMode,
		Secure:   true,
	})

	return c.NoContent(http.StatusNoContent)
}
