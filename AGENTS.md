# AGENTS.md

## 프로젝트 개요

- 해커톤(Jeckathon 2026) 출품 서비스 저장소다. 서비스 주제와 스택은 아직 정하지 않았다.
- 배포 대상은 AWS Lightsail 단일 서버이며, Docker Compose로 컨테이너를 실행하고 Nginx를 리버스 프록시로 둔다.
- 도메인은 `jeckathon.infreej.dev`이고 Cloudflare로 DNS와 HTTPS를 처리한다.
- 배포는 `main`에 병합하면 GitHub Actions가 `client/`, `server/`, `nginx/` 이미지를 빌드해 GHCR에 push하고, 서버에서 `docker compose -f compose.prod.yaml --env-file .env.prod pull`과 `up -d`로 반영한다. 서버에서는 빌드하지 않는다.

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
