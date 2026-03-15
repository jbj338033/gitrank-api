package service

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"sort"
	"time"

	"github.com/jbj338033/gitrank-api/internal/model"
	"github.com/shurcooL/githubv4"
	"golang.org/x/oauth2"
)

type ContributionDay struct {
	Date  time.Time
	Count int
}

var httpClient = &http.Client{Timeout: 10 * time.Second}

type GitHubService struct{}

func NewGitHubService() *GitHubService {
	return &GitHubService{}
}

type GitHubUser struct {
	ID              int64
	Login           string
	Name            *string
	AvatarURL       *string
	Bio             *string
	Followers       int
	Following       int
	PublicRepos     int
	GithubCreatedAt time.Time
}

type GitHubRepo struct {
	ID          int64
	FullName    string
	Description *string
	Language    *string
	Stars       int
	Forks       int
	OpenIssues  int
	Watchers    int
}

func (s *GitHubService) ExchangeCode(ctx context.Context, clientID, clientSecret, code string) (string, error) {
	data := url.Values{
		"client_id":     {clientID},
		"client_secret": {clientSecret},
		"code":          {code},
	}

	req, err := http.NewRequestWithContext(ctx, "POST", "https://github.com/login/oauth/access_token", nil)
	if err != nil {
		return "", err
	}
	req.URL.RawQuery = data.Encode()
	req.Header.Set("Accept", "application/json")

	resp, err := httpClient.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	var result struct {
		AccessToken string `json:"access_token"`
		Error       string `json:"error"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&result); err != nil {
		return "", err
	}
	if result.Error != "" {
		return "", fmt.Errorf("github oauth error: %s", result.Error)
	}
	return result.AccessToken, nil
}

func (s *GitHubService) GetUser(ctx context.Context, token string) (*GitHubUser, error) {
	req, err := http.NewRequestWithContext(ctx, "GET", "https://api.github.com/user", nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("github api returned %d", resp.StatusCode)
	}

	var raw struct {
		ID          int64   `json:"id"`
		Login       string  `json:"login"`
		Name        *string `json:"name"`
		AvatarURL   *string `json:"avatar_url"`
		Bio         *string `json:"bio"`
		Followers   int     `json:"followers"`
		Following   int     `json:"following"`
		PublicRepos int     `json:"public_repos"`
		CreatedAt   string  `json:"created_at"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&raw); err != nil {
		return nil, err
	}

	githubCreatedAt, err := time.Parse(time.RFC3339, raw.CreatedAt)
	if err != nil || githubCreatedAt.Year() < 2008 {
		githubCreatedAt = time.Date(2008, 1, 1, 0, 0, 0, 0, time.UTC)
	}

	return &GitHubUser{
		ID:              raw.ID,
		Login:           raw.Login,
		Name:            raw.Name,
		AvatarURL:       raw.AvatarURL,
		Bio:             raw.Bio,
		Followers:       raw.Followers,
		Following:       raw.Following,
		PublicRepos:     raw.PublicRepos,
		GithubCreatedAt: githubCreatedAt,
	}, nil
}

func (s *GitHubService) GetContributionsByYear(ctx context.Context, token string, login string, year int) (*model.ContributionYear, []ContributionDay, error) {
	src := oauth2.StaticTokenSource(&oauth2.Token{AccessToken: token})
	httpClient := oauth2.NewClient(ctx, src)
	client := githubv4.NewClient(httpClient)

	now := time.Now()
	from := time.Date(year, 1, 1, 0, 0, 0, 0, time.UTC)
	to := time.Date(year, 12, 31, 23, 59, 59, 0, time.UTC)
	if to.After(now) {
		to = now
	}

	var query struct {
		User struct {
			ContributionsCollection struct {
				TotalCommitContributions            int
				RestrictedContributionsCount        int
				TotalPullRequestContributions       int
				TotalIssueContributions             int
				TotalPullRequestReviewContributions int
				ContributionCalendar                struct {
					Weeks []struct {
						ContributionDays []struct {
							Date              string
							ContributionCount int
						}
					}
				}
			} `graphql:"contributionsCollection(from: $from, to: $to)"`
		} `graphql:"user(login: $login)"`
	}

	vars := map[string]any{
		"login": githubv4.String(login),
		"from":  githubv4.DateTime{Time: from},
		"to":    githubv4.DateTime{Time: to},
	}

	if err := client.Query(ctx, &query, vars); err != nil {
		return nil, nil, err
	}

	cc := query.User.ContributionsCollection

	var days []ContributionDay
	for _, w := range cc.ContributionCalendar.Weeks {
		for _, d := range w.ContributionDays {
			t, err := time.Parse("2006-01-02", d.Date)
			if err != nil {
				continue
			}
			days = append(days, ContributionDay{Date: t, Count: d.ContributionCount})
		}
	}

	return &model.ContributionYear{
		Year:         year,
		Commits:      cc.TotalCommitContributions + cc.RestrictedContributionsCount,
		PullRequests: cc.TotalPullRequestContributions,
		Issues:       cc.TotalIssueContributions,
		CodeReviews:  cc.TotalPullRequestReviewContributions,
	}, days, nil
}

func (s *GitHubService) GetContributions(ctx context.Context, token string, login string) ([]model.ContributionYear, int, int, error) {
	now := time.Now()
	currentYear := now.Year()
	var results []model.ContributionYear
	var allDays []ContributionDay

	for y := currentYear; y >= currentYear-4; y-- {
		cy, days, err := s.GetContributionsByYear(ctx, token, login, y)
		if err != nil {
			return nil, 0, 0, err
		}
		results = append(results, *cy)
		allDays = append(allDays, days...)
	}

	current, longest := CalcStreaks(allDays, now)
	return results, current, longest, nil
}

func CalcStreaks(days []ContributionDay, today time.Time) (current, longest int) {
	if len(days) == 0 {
		return 0, 0
	}

	sort.Slice(days, func(i, j int) bool {
		return days[i].Date.Before(days[j].Date)
	})

	streak := 0
	for _, d := range days {
		if d.Count > 0 {
			streak++
			if streak > longest {
				longest = streak
			}
		} else {
			streak = 0
		}
	}

	todayDate := time.Date(today.Year(), today.Month(), today.Day(), 0, 0, 0, 0, time.UTC)
	yesterday := todayDate.AddDate(0, 0, -1)

	start := todayDate
	if len(days) > 0 {
		last := days[len(days)-1]
		if last.Date.Equal(todayDate) && last.Count == 0 {
			start = yesterday
		}
	}

	for i := len(days) - 1; i >= 0; i-- {
		if days[i].Date.After(start) {
			continue
		}
		if days[i].Date.Equal(start) {
			if days[i].Count > 0 {
				current++
				start = start.AddDate(0, 0, -1)
			} else {
				break
			}
		} else {
			break
		}
	}

	return current, longest
}

func (s *GitHubService) GetRepositories(ctx context.Context, token string, login string) ([]GitHubRepo, error) {
	src := oauth2.StaticTokenSource(&oauth2.Token{AccessToken: token})
	httpClient := oauth2.NewClient(ctx, src)
	client := githubv4.NewClient(httpClient)

	var query struct {
		User struct {
			Repositories struct {
				Nodes []struct {
					DatabaseId  int64
					NameWithOwner string
					Description *string
					PrimaryLanguage *struct {
						Name string
					}
					StargazerCount int
					ForkCount      int
					OpenIssues     struct {
						TotalCount int
					} `graphql:"openIssues: issues(states: OPEN)"`
					Watchers struct {
						TotalCount int
					}
				}
			} `graphql:"repositories(first: 100, ownerAffiliations: OWNER, orderBy: {field: STARGAZERS, direction: DESC})"`
		} `graphql:"user(login: $login)"`
	}

	vars := map[string]any{
		"login": githubv4.String(login),
	}

	if err := client.Query(ctx, &query, vars); err != nil {
		return nil, err
	}

	var repos []GitHubRepo
	for _, n := range query.User.Repositories.Nodes {
		var lang *string
		if n.PrimaryLanguage != nil {
			lang = &n.PrimaryLanguage.Name
		}
		repos = append(repos, GitHubRepo{
			ID:          n.DatabaseId,
			FullName:    n.NameWithOwner,
			Description: n.Description,
			Language:    lang,
			Stars:       n.StargazerCount,
			Forks:       n.ForkCount,
			OpenIssues:  n.OpenIssues.TotalCount,
			Watchers:    n.Watchers.TotalCount,
		})
	}

	return repos, nil
}
