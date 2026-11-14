"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { TrophyIcon } from "@/components/lobby/icons";
import { fetchReplays, replayGrade, type Replay } from "@/lib/results";

import { ScoreBar } from "./ScoreBar";

const PAGE_SIZE = 20;

const endedFormat = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "Asia/Seoul",
});

type State = { items: Replay[]; totalCount: number; page: number };

export function Replays() {
  const [state, setState] = useState<State | null>(null);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [page, setPage] = useState(0);
  // 같은 쪽을 다시 요청할 때 effect를 다시 돌리기 위한 값
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetchReplays(page, PAGE_SIZE, controller.signal)
      .then((body) => {
        setState((current) => ({
          items: page === 0 || !current ? body.items : [...current.items, ...body.items],
          totalCount: body.totalCount,
          page,
        }));
        setFailed(false);
        setLoading(false);
      })
      .catch(() => {
        if (controller.signal.aborted) return;
        setFailed(true);
        setLoading(false);
      });
    return () => controller.abort();
  }, [page, attempt]);

  const hasMore = state !== null && state.items.length < state.totalCount;

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white">
      <header className="flex items-center gap-3 px-4 py-4">
        <Link
          href="/"
          aria-label="로비로"
          className="flex size-10 shrink-0 items-center justify-center rounded-xl border-2 border-ink"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        <span className="flex size-10 items-center justify-center rounded-xl border-2 border-ink bg-brand">
          <TrophyIcon className="size-6" />
        </span>
        <div>
          <h1 className="font-display text-[26px] leading-none">명경기 순위</h1>
          <p className="mt-1 text-[11px] text-neutral-600">팬심이 가장 크게 출렁인 명경기 순위</p>
        </div>
      </header>

      <main className="flex-1 px-4 pb-6">
        {state === null && !failed && <Message>명경기를 불러오는 중이에요.</Message>}
        {state === null && failed && <Message>명경기를 불러오지 못했어요.</Message>}
        {state?.items.length === 0 && <Message>아직 장이 마감된 경기가 없어요.</Message>}

        {state && state.items.length > 0 && (
          <ol className="mt-2 flex flex-col gap-3">
            {state.items.map((replay, i) => (
              <li key={replay.inviteCode}>
                <ReplayRow replay={replay} rank={i + 1} />
              </li>
            ))}
          </ol>
        )}

        {state && failed && <Message>더 불러오지 못했어요.</Message>}
        {hasMore && (
          <button
            type="button"
            disabled={loading}
            onClick={() => {
              setLoading(true);
              setPage(state.page + 1);
              setAttempt((n) => n + 1);
            }}
            className="mt-4 h-12 w-full rounded-xl border-2 border-ink text-sm font-bold disabled:text-neutral-400"
          >
            {loading ? "불러오는 중" : "더 보기"}
          </button>
        )}
      </main>

    </div>
  );
}

function ReplayRow({ replay, rank }: { replay: Replay; rank: number }) {
  const podium = rank <= 3;
  return (
    <Link
      href={`/rooms/${replay.inviteCode}/result`}
      className={`flex items-center gap-3 rounded-2xl border-2 border-ink p-3 active:translate-y-px ${
        podium ? "bg-brand shadow-[3px_3px_0_var(--color-ink)]" : "bg-white"
      }`}
    >
      <span
        className={`font-display flex size-10 shrink-0 items-center justify-center rounded-xl text-xl ${
          podium ? "bg-ink text-brand" : "border-2 border-ink"
        }`}
      >
        {rank}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-[15px] font-bold">{replay.name}</p>
        <p className="mt-0.5 text-xs text-neutral-600">
          {replayGrade(replay.replayScore)} · {endedFormat.format(new Date(replay.endedAt))} 마감
        </p>
        <ScoreBar score={replay.replayScore} className="mt-2 h-2.5 bg-white" />
      </div>
      <p className="w-12 shrink-0 text-right font-num text-2xl font-extrabold tabular-nums">
        {Math.round(replay.replayScore)}
      </p>
    </Link>
  );
}

function Message({ children }: { children: React.ReactNode }) {
  return <p className="py-16 text-center text-sm text-neutral-500">{children}</p>;
}
