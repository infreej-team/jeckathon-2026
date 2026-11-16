# AGENTS.md

## 프로젝트 개요

- 해커톤(Jeckathon 2026) 출품 서비스 저장소다. 서비스 주제는 아직 정하지 않았다.
- 스택: `server/`는 Java 21, Spring Boot 4, Spring Data JPA, Gradle이고 `client/`는 Next.js 16(App Router, TypeScript, Tailwind CSS), npm이다. DB는 PostgreSQL 18이다.
- 배포 대상은 홈서버(Linux 맥미니, x86_64) 한 대이며, Docker Compose로 컨테이너를 실행하고 Nginx를 리버스 프록시로 둔다. 프로젝트 폴더는 `~/apps/jeckathon-2026`이다.
- 도메인은 `jeckathon.infreej.dev`이고 Cloudflare Tunnel로 외부 요청을 받는다. 서버 호스트에 서비스로 설치한 `cloudflared`가 `https://localhost:443`(nginx)으로 전달하며, 서버는 외부에 포트를 공개하지 않는다. 터널의 공개 호스트명과 전달 대상은 Cloudflare 대시보드에서 관리한다.
- 배포는 `main`에 병합하면 GitHub Actions가 `client/`, `server/`, `nginx/` 이미지를 빌드해 GHCR에 push하고, 서버에 SSH로 접속해 `compose.prod.yaml`을 복사한 뒤 `docker compose -f compose.prod.yaml --env-file .env.prod pull`과 `up -d`로 반영한다. 서버에서는 빌드하지 않는다.
- 배포 SSH도 같은 터널을 지난다. Cloudflare Access가 서비스 토큰을 확인한 요청만 서버 SSH로 넘긴다.
- `main`에 병합한 코드는 바로 운영 서버에 나간다.
- 자동 배포는 `production` 환경 Secrets `DEPLOY_HOST`(터널의 SSH 호스트명), `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_SSH_KNOWN_HOSTS`, `CF_ACCESS_CLIENT_ID`, `CF_ACCESS_CLIENT_SECRET`을 사용한다. 이 환경은 `main`에서만 쓸 수 있게 제한되어 있으므로 저장소 Secrets에 같은 값을 넣지 않는다. 서버의 `.env.prod`와 인증서는 자동 배포가 건드리지 않으므로 바꿀 때는 서버에서 직접 수정한다.
- `compose.prod.yaml`에 새 환경 변수가 필요한 PR은 병합하기 전에 서버 `.env.prod`에 값을 먼저 추가한다. 값이 없으면 배포가 compose 검증 단계에서 멈춘다.
- 롤백으로 `.env.prod`의 `IMAGE_TAG`를 sha 태그로 고정한 동안에는 병합해도 이미지가 바뀌지 않는다. `compose.prod.yaml`은 계속 최신본으로 덮어쓴다.

## 디렉터리 구조

- `server/src/main/java/dev/infreej/jeckathon/`
  - `domain/<도메인>/`: 기능 코드를 도메인별로 둔다. 도메인 안에서 `controller`, `service`, `repository`, `entity`, `dto`로 나눈다.
  - `global/`: 여러 도메인이 쓰는 코드다. `error/`(오류 코드·예외·전역 예외 처리), `response/`(목록 응답 `PageResponse`), `health/`(`GET /api/health`).
- 의도한 실패는 `ErrorCode`에 코드를 추가하고 `BusinessException`을 던진다. 실패 응답 형식은 `GlobalExceptionHandler`가 맞춘다.
- 운영 이미지 계약: client는 3000 포트에서 `/api`를 제외한 모든 경로를 처리한다. server는 8080 포트에서 `/api/*`를 처리하고 `GET /api/health`에 `200`으로 응답한다. 배포 후 상태 확인이 이 경로를 쓴다.
- server의 모든 API 경로는 `/api`로 시작한다. client에서는 `/api` 경로에 Route Handler를 만들지 않는다. 운영에서 nginx가 `/api`를 server로 보내므로 client에 닿지 않는다.
- JPA `ddl-auto`는 기본 `update`다. 컬럼 삭제나 이름 변경은 반영되지 않으므로 필요하면 DB에서 직접 바꾼다.

## 로컬 개발

1. 루트에서 `docker compose up -d`로 PostgreSQL을 띄운다. 접속 정보는 `localhost:5432`, DB·사용자 `jeckathon`, 비밀번호 `1234`다.
2. `server/`에서 `./gradlew bootRun`으로 8080 포트에 server를 띄운다.
3. `client/`에서 `npm install` 후 `npm run dev`로 3000 포트에 client를 띄운다. 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 넘긴다.

## Git 브랜치 전략

- `dev` 브랜치를 사용하지 않는다.
- `main`에서 작업 브랜치를 만들고, PR로 `main`에 바로 스쿼시 병합한다.

## REST API 응답 구조

- 성공 여부는 HTTP 상태 코드로 나타낸다. 실패를 `200`으로 응답하지 않는다.
- 성공 응답은 공통 래퍼 없이 데이터를 바로 반환한다. 삭제처럼 반환할 데이터가 없으면 `204`로 응답한다.
- 목록은 배열을 바로 반환하지 않고 `{ "items": [...], "page": 0, "size": 20, "totalCount": 135 }` 형식으로 반환한다.
- 실패 응답은 다음 형식으로 통일한다. `errors`는 입력값 검증 실패에만 포함한다.

  ```json
  {
    "code": "VALIDATION_FAILED",
    "message": "입력값이 올바르지 않습니다.",
    "errors": [{ "field": "title", "reason": "필수 값입니다." }]
  }
  ```

- 날짜는 시간대를 포함한 ISO-8601 문자열로 보낸다. 값이 없으면 필드를 빼지 않고 `null`로 보낸다.

## 테스트

- 테스트 코드를 작성하지 않는다.
