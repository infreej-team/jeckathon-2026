# AGENTS.md

## 프로젝트 개요

- 해커톤(Jeckathon 2026) 출품 서비스 저장소다. 서비스 주제와 스택은 아직 정하지 않았다.
- 배포 대상은 AWS Lightsail 단일 서버이며, Docker Compose로 컨테이너를 실행하고 Nginx를 리버스 프록시로 둔다.
- 도메인은 `jeckathon.infreej.dev`이고 Cloudflare로 DNS와 HTTPS를 처리한다.
- 배포는 `main`에 병합하면 GitHub Actions가 `client/`, `server/`, `nginx/` 이미지를 빌드해 GHCR에 push하고, 서버에 SSH로 접속해 `compose.prod.yaml`을 복사한 뒤 `docker compose -f compose.prod.yaml --env-file .env.prod pull`과 `up -d`로 반영한다. 서버에서는 빌드하지 않는다.
- `main`에 병합한 코드는 바로 운영 서버에 나간다. 시연 직전에는 병합을 멈춘다.
- 자동 배포는 저장소 Secrets `DEPLOY_HOST`, `DEPLOY_USER`, `DEPLOY_SSH_KEY`, `DEPLOY_SSH_KNOWN_HOSTS`를 사용한다. 서버의 `.env.prod`와 인증서는 자동 배포가 건드리지 않으므로 바꿀 때는 서버에서 직접 수정한다.

## 임시 이미지를 실제 이미지로 교체

- 현재 client와 server는 소스가 없어 임시 이미지(`traefik/whoami`)로 실행된다. `compose.prod.yaml`의 `WHOAMI_*` 환경 변수는 이 임시 이미지 전용 설정이다.
- 실제 이미지는 다음 계약을 지킨다. client는 3000 포트에서 `/api`를 제외한 모든 경로를 처리한다. server는 8080 포트에서 `/api/*`를 처리하고 `GET /api/health`에 `200`으로 응답한다.
- 교체 순서는 다음과 같다.
  1. 소스와 `Dockerfile`을 `main`에 병합하고, Actions가 모든 서비스를 빌드해 GHCR에 `latest` 태그를 붙였는지 확인한다.
  2. 서버의 `.env.prod`에서 `CLIENT_IMAGE`와 `SERVER_IMAGE` 주석을 해제한다. 이 단계에서는 `up -d`를 실행하지 않는다.
  3. 후속 PR에서 `compose.prod.yaml`의 `WHOAMI_*` 환경 변수를 삭제하고 병합한다. 자동 배포가 이 파일을 서버에 복사하고 실제 이미지로 재시작한다.
  4. 외부에서 `/`와 `/api/health` 응답을 확인한다.
- 2번 없이 3번을 병합하면 whoami가 기본 포트(80)로 떠서 nginx가 `502`를 반환한다. 2번을 먼저 끝낸다.
- 1번에서 `WHOAMI_*` 삭제까지 같은 PR에 넣으려면 병합하기 전에 2번을 끝낸다. 실제 이미지는 배포 직전에 빌드되므로 pull에 실패하지 않는다.

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
