export type RoomStatus = "WAITING" | "LIVE" | "ENDED";

export type Match = {
  id: number;
  competition: string;
  round: string;
  groupName: string | null;
  kickoffAt: string;
  team1: string;
  team2: string;
  venue: string;
};

export type Room = {
  inviteCode: string;
  name: string;
  match: Match | null;
  status: RoomStatus;
  participantCount: number;
  startedAt: string | null;
  endedAt: string | null;
  changeRate: number | null;
  volatility: number | null;
  replayScore: number | null;
  createdAt: string;
};

type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalCount: number;
};

// 로비 목록. 서버가 진행 중, 대기, 종료 순으로 준다.
export async function fetchRooms(signal?: AbortSignal): Promise<Room[]> {
  const response = await fetch("/api/rooms?size=50", { signal });
  if (!response.ok) {
    throw new Error(`방 목록 조회 실패: ${response.status}`);
  }
  const body: PageResponse<Room> = await response.json();
  return body.items;
}
