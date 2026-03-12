package model

import "time"

type User struct {
	ID          int64
	Login       string
	Name        *string
	AvatarURL   *string
	Bio         *string
	Followers   int
	Following   int
	PublicRepos int
	AccessToken *string
	CreatedAt   time.Time
	UpdatedAt   time.Time
}

type UserRanking struct {
	UserID       int64
	TotalCommits int
	TotalPRs     int
	TotalIssues  int
	TotalReviews int
	TotalStars   int
	TotalForks   int
	Score        float64
	Rank         *int
	CalculatedAt time.Time
}

type UserRankingRow struct {
	Rank         int     `json:"rank"`
	Login        string  `json:"login"`
	Name         *string `json:"name"`
	AvatarURL    *string `json:"avatar_url"`
	Score        float64 `json:"score"`
	TotalCommits int     `json:"total_commits"`
	TotalPRs     int     `json:"total_prs"`
	TotalIssues  int     `json:"total_issues"`
	TotalReviews int     `json:"total_reviews"`
	TotalStars   int     `json:"total_stars"`
	TotalForks   int     `json:"total_forks"`
	UserID       int64   `json:"-"`
}

type UserDetail struct {
	ID            int64              `json:"id"`
	Login         string             `json:"login"`
	Name          *string            `json:"name"`
	AvatarURL     *string            `json:"avatar_url"`
	Bio           *string            `json:"bio"`
	Followers     int                `json:"followers"`
	Following     int                `json:"following"`
	PublicRepos   int                `json:"public_repos"`
	Ranking       *UserRankingInfo   `json:"ranking"`
	Contributions []ContributionYear `json:"contributions"`
	TopRepos      []RepoSummary      `json:"top_repositories"`
	SyncedAt      *time.Time         `json:"synced_at"`
}

type UserRankingInfo struct {
	Rank         *int    `json:"rank"`
	Score        float64 `json:"score"`
	TotalCommits int     `json:"total_commits"`
	TotalPRs     int     `json:"total_prs"`
	TotalIssues  int     `json:"total_issues"`
	TotalReviews int     `json:"total_reviews"`
	TotalStars   int     `json:"total_stars"`
	TotalForks   int     `json:"total_forks"`
}
