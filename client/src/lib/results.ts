// 서버 MarketController의 size 상한
const TICK_PAGE_SIZE = 600;
// 한 방의 틱을 최대 몇 쪽까지 받을지. 600 × 30 = 5시간 분량
const MAX_TICK_PAGES = 30;

export type MarketTick = {
  tickId: number;
  recordedAt: string;
  indexValue: number;
  buyCount: number;
  sellCount: number;
};

export type Replay = {
  inviteCode: string;
  name: string;
  endedAt: string;
  replayScore: number;
};

type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalCount: number;
};

async function fetchPage<T>(url: string, signal?: AbortSignal): Promise<PageResponse<T>> {
  const response = await fetch(url, { signal });
  if (!response.ok) {
    throw new Error(`조회 실패: ${response.status} ${url}`);
  }
  return response.json();
}

// 경기 전체 틱을 오래된 순으로 준다. 서버는 최신 순으로 쪽을 나눠 준다.
export async function fetchAllTicks(inviteCode: string, signal?: AbortSignal): Promise<MarketTick[]> {
  const url = (page: number) =>
    `/api/rooms/${encodeURIComponent(inviteCode)}/market/ticks?page=${page}&size=${TICK_PAGE_SIZE}`;
  const first = await fetchPage<MarketTick>(url(0), signal);
  const pageCount = Math.min(Math.ceil(first.totalCount / TICK_PAGE_SIZE), MAX_TICK_PAGES);
  const rest = await Promise.all(
    Array.from({ length: Math.max(pageCount - 1, 0) }, (_, i) => fetchPage<MarketTick>(url(i + 1), signal)),
  );
  return [first, ...rest].flatMap((page) => page.items).reverse();
}

// 명경기 지수가 높은 순. 거래가 없어 지수가 없는 방은 서버가 뺀다.
export function fetchReplays(page: number, size: number, signal?: AbortSignal): Promise<PageResponse<Replay>> {
  return fetchPage<Replay>(`/api/replays?page=${page}&size=${size}`, signal);
}

// 서버 ReplayScore는 0~100점이다.
export function replayGrade(score: number): string {
  if (score >= 80) return "역대급 명경기";
  if (score >= 50) return "손에 땀 쥔 경기";
  if (score >= 20) return "볼만한 경기";
  return "잔잔한 경기";
}
