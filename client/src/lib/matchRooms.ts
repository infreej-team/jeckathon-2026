import type { Room } from "./rooms";

// 응원 팀 고르기 화면에서 쓰는 방. team이 null이면 두 팀 팬이 함께 쓰는 방이다.
export type TeamRoom = Room & {
  team: string | null;
  capacity: number;
};

export class RoomNotFoundError extends Error {}

export async function fetchRoom(inviteCode: string, signal?: AbortSignal): Promise<TeamRoom> {
  const response = await fetch(`/api/rooms/${encodeURIComponent(inviteCode)}`, { signal });
  if (response.status === 404) {
    throw new RoomNotFoundError();
  }
  if (!response.ok) {
    throw new Error(`방 조회 실패: ${response.status}`);
  }
  return response.json();
}

// 한 경기에 열린 방 전체. 서버가 만든 순서로 준다.
export async function fetchMatchRooms(matchId: number, signal?: AbortSignal): Promise<TeamRoom[]> {
  const response = await fetch(`/api/matches/${matchId}/rooms`, { signal });
  if (!response.ok) {
    throw new Error(`경기 방 목록 조회 실패: ${response.status}`);
  }
  const body: { items: TeamRoom[] } = await response.json();
  return body.items;
}
