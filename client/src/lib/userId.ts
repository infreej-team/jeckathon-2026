// 로그인 없이 사용자를 구분하는 브라우저별 익명 ID.
// 서버는 웹소켓 접속 URL의 ?userId=와 코멘트 API의 X-User-Id 헤더(36자 이하)로 같은 값을 받는다.
const STORAGE_KEY = "fandaq:userId";
export const USER_ID_HEADER = "X-User-Id";

let cached: string | null = null;

function createId(): string {
  // randomUUID는 HTTPS나 localhost에서만 있으므로 없으면 같은 형식(36자)으로 직접 만든다.
  if (typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

// 브라우저에서만 부른다. 저장소를 쓸 수 없으면(사생활 보호 모드 등) 탭이 열려 있는 동안만 유지한다.
export function getUserId(): string {
  if (cached) return cached;
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored && stored.length <= 36) {
      cached = stored;
      return stored;
    }
    cached = createId();
    localStorage.setItem(STORAGE_KEY, cached);
  } catch {
    cached ??= createId();
  }
  return cached;
}

// 코멘트처럼 사용자 구분이 필요한 API 요청에 붙인다.
export function userIdHeaders(): Record<string, string> {
  return { [USER_ID_HEADER]: getUserId() };
}

// 웹소켓 주소. 개발 서버는 웹소켓을 넘기지 못할 수 있으므로 NEXT_PUBLIC_WS_ORIGIN으로 직접 지정할 수 있다.
export function webSocketUrl(): string {
  const origin =
    process.env.NEXT_PUBLIC_WS_ORIGIN ??
    `${location.protocol === "https:" ? "wss:" : "ws:"}//${location.host}`;
  return `${origin}/api/ws?userId=${encodeURIComponent(getUserId())}`;
}
