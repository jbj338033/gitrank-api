package config

import (
	"fmt"
	"os"

	"github.com/joho/godotenv"
)

type Config struct {
	Port           string
	DatabaseURL    string
	GitHubClientID string
	GitHubSecret   string
	JWTSecret      string
	EncryptionKey  string
	FrontendURL    string
}

func Load() (*Config, error) {
	_ = godotenv.Load()

	cfg := &Config{
		Port:           getEnv("SERVER_PORT", "8080"),
		DatabaseURL:    os.Getenv("DATABASE_URL"),
		GitHubClientID: os.Getenv("GITHUB_CLIENT_ID"),
		GitHubSecret:   os.Getenv("GITHUB_CLIENT_SECRET"),
		JWTSecret:      os.Getenv("JWT_SECRET"),
		EncryptionKey:  os.Getenv("ENCRYPTION_KEY"),
		FrontendURL:    os.Getenv("FRONTEND_URL"),
	}

	required := []struct{ name, val string }{
		{"DATABASE_URL", cfg.DatabaseURL},
		{"GITHUB_CLIENT_ID", cfg.GitHubClientID},
		{"GITHUB_CLIENT_SECRET", cfg.GitHubSecret},
		{"JWT_SECRET", cfg.JWTSecret},
		{"ENCRYPTION_KEY", cfg.EncryptionKey},
		{"FRONTEND_URL", cfg.FrontendURL},
	}
	for _, r := range required {
		if r.val == "" {
			return nil, fmt.Errorf("required environment variable %s is not set", r.name)
		}
	}

	return cfg, nil
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
