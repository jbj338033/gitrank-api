package model

type Contribution struct {
	ID           int64
	UserID       int64
	Year         int
	Commits      int
	PullRequests int
	Issues       int
	CodeReviews  int
}

type ContributionYear struct {
	Year         int `json:"year"`
	Commits      int `json:"commits"`
	PullRequests int `json:"pull_requests"`
	Issues       int `json:"issues"`
	CodeReviews  int `json:"code_reviews"`
}
