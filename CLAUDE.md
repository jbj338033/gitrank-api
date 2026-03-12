# CLAUDE.md

## 프로젝트 구조

```
cmd/server/main.go       # 엔트리포인트, 의존성 조립, 라우팅
internal/
  config/                # 환경변수 로드
  database/              # pgxpool 초기화, golang-migrate 실행
  handler/               # Echo 핸들러 (auth, user, repo)
  middleware/             # JWT 미들웨어
  model/                 # DB 모델, API 응답 구조체
  repository/            # PostgreSQL 쿼리 (user, repo, contribution)
  service/               # 비즈니스 로직 (auth, github, ranking)
  worker/                # 백그라운드 데이터 수집기
migrations/              # SQL 마이그레이션 파일
```

## 컨벤션

- Echo v5 사용 — `c *echo.Context` (포인터), `c.Response()`는 `http.ResponseWriter` 반환
- Echo v5 제네릭 헬퍼: `echo.ContextGet[T](c, key)`, `echo.QueryParamOr[T](c, key, default)`
- 에러 응답: `model.ErrorResponse{Code, Message}` — 메시지는 소문자, 마침표 없음
- 커서 페이지네이션: `model.Cursor{S, I}` → base64 인코딩
- GitHub 토큰은 AES-256-GCM으로 암호화 저장
- 랭킹 점수: 유저 `commits*1 + prs*3 + issues*1.5 + reviews*2 + stars*0.5 + forks*0.3`, 레포 `stars*1 + forks*2 + watchers*0.5`
- repository 메서드들은 `*pgxpool.Pool`을 직접 사용 (ORM 없음)

## 빌드 & 테스트

```bash
go build ./...
go test ./...
```

## 주의사항

- `handler/auth.go`의 Login은 SSE 스트리밍 — 쿠키 설정 후 `text/event-stream` 전환
- Login 플로우에서 `oauth_state` 쿠키로 CSRF 방어 (`crypto/subtle.ConstantTimeCompare`)
- `service/github.go`의 GraphQL 쿼리에서 `githubv4.DateTime`, `githubv4.String` 타입 사용 필수
- `middleware/ratelimit.go`는 토큰 버킷 방식 — auth 그룹에 10req/min 적용
- `worker/collector.go`는 6시간 주기로 전체 유저 데이터 수집 후 랭크 재계산
