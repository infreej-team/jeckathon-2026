"use client";

import { useCallback, useEffect, useState } from "react";
import { fetchMatchRooms, fetchRoom, RoomNotFoundError, type TeamRoom } from "@/lib/matchRooms";
import { teamOf, type TeamView } from "@/lib/teams";

export type RoomInfo = {
  room: TeamRoom;
  home: TeamView | null;
  away: TeamView | null;
  // 응원 팀 고르기 화면과 같은 방 이름. 예: KOR-2, 공용-1
  ticker: string;
};

// TeamSelect의 방 번호와 같게 매긴다. 같은 경기에서 같은 팀(또는 공용) 방 중 몇 번째인지 센다.
function tickerOf(room: TeamRoom, matchRooms: TeamRoom[]): string {
  const siblings = matchRooms.filter((r) => r.team === room.team);
  const number = siblings.findIndex((r) => r.inviteCode === room.inviteCode) + 1;
  if (number === 0) return room.name;
  return room.team === null ? `공용-${number}` : `${teamOf(room.team).code}-${number}`;
}

export function useRoom(inviteCode: string) {
  const [info, setInfo] = useState<RoomInfo | null>(null);
  const [error, setError] = useState<"notFound" | "failed" | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      try {
        const room = await fetchRoom(inviteCode, controller.signal);
        const matchRooms = room.match ? await fetchMatchRooms(room.match.id, controller.signal) : [room];
        setInfo({
          room,
          home: room.match ? teamOf(room.match.team1) : null,
          away: room.match ? teamOf(room.match.team2) : null,
          ticker: tickerOf(room, matchRooms),
        });
        setError(null);
      } catch (e) {
        if (!controller.signal.aborted) setError(e instanceof RoomNotFoundError ? "notFound" : "failed");
      }
    };
    load();
    return () => controller.abort();
  }, [inviteCode, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { info, error, reload };
}
