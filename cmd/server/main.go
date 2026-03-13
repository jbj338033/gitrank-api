package main

import (
	"context"
	"log/slog"

	"github.com/jbj338033/gitrank-api/internal/config"
	"github.com/jbj338033/gitrank-api/internal/database"
	"github.com/jbj338033/gitrank-api/internal/handler"
	authmw "github.com/jbj338033/gitrank-api/internal/middleware"
	"github.com/jbj338033/gitrank-api/internal/repository"
	"github.com/jbj338033/gitrank-api/internal/service"
	"github.com/jbj338033/gitrank-api/internal/worker"
	"github.com/labstack/echo/v5"
	"github.com/labstack/echo/v5/middleware"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		slog.Error("failed to load config", "error", err)
		return
	}

	pool, err := database.New(cfg.DatabaseURL)
	if err != nil {
		slog.Error("failed to connect to database", "error", err)
		return
	}
	defer pool.Close()

	if err := database.Migrate(cfg.DatabaseURL); err != nil {
		slog.Error("failed to run migrations", "error", err)
		return
	}

	authService, err := service.NewAuthService(cfg.JWTSecret, cfg.EncryptionKey)
	if err != nil {
		slog.Error("failed to initialize auth service", "error", err)
		return
	}

	ghService := service.NewGitHubService()
	rankService := service.NewRankingService(pool)

	userRepo := repository.NewUserRepository(pool)
	repoRepo := repository.NewRepoRepository(pool)
	contribRepo := repository.NewContributionRepository(pool)
	streakRepo := repository.NewStreakRepository(pool)

	authHandler := handler.NewAuthHandler(cfg, authService, ghService, userRepo, contribRepo, repoRepo, streakRepo, rankService)
	userHandler := handler.NewUserHandler(userRepo, contribRepo, repoRepo, streakRepo, ghService, authService, rankService)
	repoHandler := handler.NewRepoHandler(repoRepo, userRepo, rankService)

	collector := worker.NewCollector(userRepo, contribRepo, repoRepo, streakRepo, ghService, authService, rankService)
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	go collector.Start(ctx)

	e := echo.New()
	e.Use(middleware.RequestLogger())
	e.Use(middleware.Recover())
	e.Use(middleware.CORSWithConfig(middleware.CORSConfig{
		AllowOrigins:     []string{cfg.FrontendURL},
		AllowCredentials: true,
	}))

	api := e.Group("/api")

	authRL := authmw.RateLimit(10.0/60.0, 10)
	auth := api.Group("/auth", authRL)
	auth.GET("/state", authHandler.State)
	auth.POST("/login", authHandler.Login)
	auth.POST("/logout", authHandler.Logout)
	auth.POST("/refresh", authHandler.Refresh)

	users := api.Group("/users")
	users.GET("/me", userHandler.Me, authmw.JWTMiddleware(cfg.JWTSecret))
	users.GET("/me/repos", userHandler.MyRepos, authmw.JWTMiddleware(cfg.JWTSecret))
	users.GET("/ranking", userHandler.Ranking)
	users.GET("/:username", userHandler.Detail)
	users.POST("/:username/sync", userHandler.Sync, authmw.JWTMiddleware(cfg.JWTSecret))

	repos := api.Group("/repos")
	repos.GET("/ranking", repoHandler.Ranking)
	repos.GET("/:owner/:repo", repoHandler.Detail)
	repos.PATCH("/:owner/:repo/visibility", repoHandler.ToggleVisibility, authmw.JWTMiddleware(cfg.JWTSecret))

	if err := e.Start(":" + cfg.Port); err != nil {
		slog.Error("failed to start server", "error", err)
	}
}
