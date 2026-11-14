"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import type { RoomStatus } from "@/lib/rooms";
import { useNickname } from "@/lib/nickname";
import { useComments } from "../_lib/use-comments";
import { useLiveMarket } from "../_lib/use-live-market";
import { useRoom, type RoomInfo } from "../_lib/use-room";
import { BasicTrading } from "./basic-trading";
import { NicknameDialog } from "./nickname-dialog";
import { TapTrading } from "./tap-trading";

// 화면 두 모드가 함께 쓰는 방 상태. 상태와 참여자 수는 웹소켓 값이 더 최신이라 우선한다.
export type LiveView = {
  inviteCode: string;
  info: RoomInfo;
  status: RoomStatus;
  participantCount: number;
  // 방 시작 뒤 몇 분째인지. 경기 중이 아니면 null
  minute: number | null;
};

function useNow(intervalMs: number) {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const id = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(id);
  }, [intervalMs]);
  return now;
}

export function LiveRoom() {
  const { inviteCode } = useParams<{ inviteCode: string }>();
  const { info, error, reload } = useRoom(inviteCode);
  const market = useLiveMarket(inviteCode);
  const comments = useComments(inviteCode);
  const [mode, setMode] = useState<"basic" | "tap">("basic");
  const now = useNow(10_000);
  const nickname = useNickname();

  // 시작·종료 알림을 받으면 startedAt과 결산 값을 다시 받는다.
  useEffect(() => {
    if (market.status) reload();
  }, [market.status, reload]);

  if (!info) {
    return (
      <main className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col items-center justify-center gap-4 bg-white px-4 text-center text-sm">
        <p className="text-muted">
          {error === "notFound" && "방을 찾을 수 없어요."}
          {error === "failed" && "방 정보를 불러오지 못했어요."}
          {error === null && "방 정보를 불러오는 중이에요."}
        </p>
        {error && (
          <Link href="/" className="rounded-xl border-2 border-ink px-4 py-2 font-bold">
            로비로 가기
          </Link>
        )}
      </main>
    );
  }

  const status = market.status ?? info.room.status;
  const startedAt = info.room.startedAt ? Date.parse(info.room.startedAt) : null;
  const view: LiveView = {
    inviteCode,
    info,
    status,
    participantCount: market.participantCount ?? info.room.participantCount,
    minute: status === "LIVE" && startedAt !== null ? Math.max(1, Math.floor((now - startedAt) / 60_000) + 1) : null,
  };

  return (
    <>
      {mode === "basic" ? (
        <BasicTrading view={view} market={market} comments={comments} onTapMode={() => setMode("tap")} />
      ) : (
        <TapTrading view={view} market={market} onBasicMode={() => setMode("basic")} />
      )}
      {nickname === null && <NicknameDialog />}
    </>
  );
}
