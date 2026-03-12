package model

type Repository struct {
	ID          int64
	OwnerID     int64
	FullName    string
	Description *string
	Language    *string
	Stars       int
	Forks       int
	OpenIssues  int
	Watchers    int
	IsPublic    bool
}

type RepoRanking struct {
	RepoID       int64
	Score        float64
	Rank         *int
	CalculatedAt string
}

type RepoRankingRow struct {
	Rank        int          `json:"rank"`
	FullName    string       `json:"full_name"`
	Description *string      `json:"description"`
	Language    *string      `json:"language"`
	Stars       int          `json:"stars"`
	Forks       int          `json:"forks"`
	Watchers    int          `json:"watchers"`
	Score       float64      `json:"score"`
	Owner       OwnerSummary `json:"owner"`
	RepoID      int64        `json:"-"`
}

type OwnerSummary struct {
	Login     string  `json:"login"`
	AvatarURL *string `json:"avatar_url"`
}

type RepoSummary struct {
	FullName    string  `json:"full_name"`
	Description *string `json:"description"`
	Language    *string `json:"language"`
	Stars       int     `json:"stars"`
	Forks       int     `json:"forks"`
	IsPublic    bool    `json:"is_public"`
}

type RepoDetail struct {
	ID          int64            `json:"id"`
	FullName    string           `json:"full_name"`
	Description *string          `json:"description"`
	Language    *string          `json:"language"`
	Stars       int              `json:"stars"`
	Forks       int              `json:"forks"`
	OpenIssues  int              `json:"open_issues"`
	Watchers    int              `json:"watchers"`
	Ranking     *RepoRankingInfo `json:"ranking"`
	Owner       OwnerDetail      `json:"owner"`
}

type RepoRankingInfo struct {
	Rank  *int    `json:"rank"`
	Score float64 `json:"score"`
}

type OwnerDetail struct {
	Login     string  `json:"login"`
	Name      *string `json:"name"`
	AvatarURL *string `json:"avatar_url"`
}
