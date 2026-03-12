# gitrank-api

GitHub 활동 기반 개발자/레포지토리 랭킹 API 서버.

## 기술 스택

- Go 1.25, Echo v5
- PostgreSQL 17, pgx v5
- GitHub GraphQL API (githubv4)
- JWT (access/refresh) + HTTP-only cookie 인증
- golang-migrate 마이그레이션

## 실행

```bash
cp .env.example .env  # GitHub OAuth 앱 설정 필요
docker compose up postgres -d
go run ./cmd/server
```

## 환경 변수

| 변수 | 설명 |
|---|---|
| `DATABASE_URL` | PostgreSQL 연결 문자열 |
| `GITHUB_CLIENT_ID` | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth App Secret |
| `JWT_SECRET` | JWT 서명 키 |
| `ENCRYPTION_KEY` | GitHub 토큰 암호화 키 (64자 hex, AES-256) |
| `FRONTEND_URL` | 프론트엔드 URL (CORS, 쿠키 Secure 플래그) |
| `SERVER_PORT` | 서버 포트 (기본: 8080) |

## API

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| POST | `/api/auth/login` | - | GitHub OAuth 로그인 (SSE 스트리밍) |
| POST | `/api/auth/logout` | - | 로그아웃 |
| POST | `/api/auth/refresh` | cookie | 토큰 갱신 |
| GET | `/api/users/me` | JWT | 내 정보 |
| GET | `/api/users/ranking` | - | 유저 랭킹 (커서 페이지네이션) |
| GET | `/api/users/:username` | - | 유저 상세 |
| POST | `/api/users/:username/sync` | JWT | 데이터 동기화 (5분 쿨다운) |
| GET | `/api/repos/ranking` | - | 레포 랭킹 (커서 페이지네이션) |
| GET | `/api/repos/:owner/:repo` | - | 레포 상세 |

## 배포

```bash
docker compose up --build
```

nginx가 `:80`에서 `/api/`를 API로 프록시하고, 나머지는 프론트엔드 정적 파일을 서빙합니다.
