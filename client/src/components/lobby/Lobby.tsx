"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { placeholderQuote } from "@/lib/placeholder";
import { fetchRooms, type Room } from "@/lib/rooms";
import { stageLabel, teamOf, type TeamView } from "@/lib/teams";

import { AntIcon, ArrowIcon, ReceiptIcon, TrendIcon, TrophyIcon } from "./icons";
import { Sparkline } from "./Sparkline";

const REFRESH_MS = 10_000;

type RoomView = {
  room: Room;
  home: TeamView | null;
  away: TeamView | null;
  stage: string | null;
  index: number;
  changeRate: number;
  series: number[];
  score: [number, number];
};

function toView(room: Room): RoomView {
  // 임시값: 지수, 등락률, 차트, 스코어. 종료된 방의 등락률만 서버 값이 있으면 그 값을 쓴다.
  const quote = placeholderQuote(room.inviteCode);
  return {
    room,
    home: room.match ? teamOf(room.match.team1) : null,
    away: room.match ? teamOf(room.match.team2) : null,
    stage: room.match ? stageLabel(room.match.round, room.match.groupName) : null,
    index: quote.index,
    changeRate: room.status === "ENDED" && room.changeRate !== null ? room.changeRate : quote.changeRate,
    series: quote.series,
    score: quote.score,
  };
}

const indexFormat = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const countFormat = new Intl.NumberFormat("en-US");
const kickoffFormat = new Intl.DateTimeFormat("ko-KR", {
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "Asia/Seoul",
});

function formatRate(rate: number): string {
  return `${rate >= 0 ? "+" : "-"}${Math.abs(rate).toFixed(2)}%`;
}

// 방 시작 시각을 킥오프로 보고 경과 분을 센다.
function liveMinute(room: Room, now: number): string {
  if (!room.startedAt) {
    return "LIVE";
  }
  const minutes = Math.max(Math.floor((now - Date.parse(room.startedAt)) / 60_000), 0);
  return `LIVE ${minutes}'`;
}

export function Lobby() {
  const [views, setViews] = useState<RoomView[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      try {
        const rooms = await fetchRooms(controller.signal);
        setViews(rooms.map(toView));
        setFailed(false);
      } catch {
        if (!controller.signal.aborted) {
          setFailed(true);
        }
      }
      setNow(Date.now());
    };
    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => {
      controller.abort();
      clearInterval(timer);
    };
  }, []);

  const liveViews = views?.filter((view) => view.room.status === "LIVE") ?? [];
  // 가장 뜨거운 종목: 참여자가 가장 많은 진행 중인 방. 없으면 목록의 첫 방
  const hottest =
    liveViews.reduce<RoomView | null>(
      (best, view) => (!best || view.room.participantCount > best.room.participantCount ? view : best),
      null,
    ) ??
    views?.[0] ??
    null;

  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white">
      <Header />
      <Ticker views={liveViews} />

      <main className="flex-1 px-4 pb-6">
        {views === null && !failed && <Message>경기를 불러오는 중이에요.</Message>}
        {views === null && failed && <Message>경기를 불러오지 못했어요. 잠시 후 다시 시도할게요.</Message>}
        {views?.length === 0 && <Message>아직 상장된 경기가 없어요.</Message>}

        {hottest && now !== null && <HotCard view={hottest} now={now} />}

        {views && views.length > 0 && now !== null && (
          <section className="mt-9">
            <h2 className="font-display text-[22px] leading-none">오늘의 상장 경기</h2>
            <ul className="mt-4 overflow-hidden rounded-2xl border-2 border-ink">
              {views.map((view) => (
                <li key={view.room.inviteCode} className="border-b border-ink last:border-b-0">
                  <RoomRow view={view} now={now} />
                </li>
              ))}
            </ul>
          </section>
        )}

        <p className="mt-6 text-center text-xs text-neutral-500">진짜 돈은 1원도 안 들어요. 잃는 건 멘탈뿐.</p>
      </main>

      <BottomNav />
    </div>
  );
}

function Header() {
  return (
    <header className="flex items-center justify-between px-4 py-4">
      <div className="flex items-center gap-3">
        <span className="flex size-10 items-center justify-center rounded-xl border-2 border-ink bg-brand">
          <AntIcon className="w-7" />
        </span>
        <div>
          <p className="font-display text-[26px] leading-none">개경장</p>
          <p className="mt-1 text-[10px] tracking-[0.2em] text-neutral-600">개미들의 경기장</p>
        </div>
      </div>
    </header>
  );
}

function Ticker({ views }: { views: RoomView[] }) {
  // 경기 정보가 없는 방은 팀 코드 대신 방 이름을 쓴다.
  const items = views.map((view) => ({ view, code: view.home ? view.home.code : `${view.room.name} ` }));
  if (items.length === 0) {
    return <div className="h-9 bg-ink" />;
  }
  // 끊김 없이 흐르도록 같은 내용을 두 번 이어 붙인다.
  const row = (hidden: boolean) => (
    <div className="flex shrink-0 gap-7 pr-7" aria-hidden={hidden}>
      {items.map(({ view, code }) => (
        <span key={view.room.inviteCode} className="whitespace-nowrap">
          <span className="font-semibold">{code}팬심</span> {indexFormat.format(view.index)}{" "}
          <span className={view.changeRate >= 0 ? "text-[#ff7b8e]" : "text-[#7fa8ff]"}>
            {view.changeRate >= 0 ? "▲" : "▼"}
            {Math.abs(view.changeRate).toFixed(2)}%
          </span>
        </span>
      ))}
    </div>
  );
  return (
    <div className="flex h-9 items-center overflow-hidden bg-ink font-mono text-[13px] text-white">
      <div className="ticker flex pl-4">
        {row(false)}
        {row(true)}
      </div>
    </div>
  );
}

function Flag({ team, className }: { team: TeamView | null; className?: string }) {
  if (!team?.flag) {
    return null;
  }
  return <span className={`fi fi-${team.flag} shrink-0 rounded-[2px] ring-1 ring-ink/70 ${className ?? ""}`} />;
}

function HotCard({ view, now }: { view: RoomView; now: number }) {
  const { room, home, away } = view;
  const live = room.status === "LIVE";

  return (
    <section className="relative mt-6 rounded-3xl border-2 border-ink bg-brand p-4 shadow-[5px_5px_0_var(--color-ink)]">
      {view.stage && (
        <span className="font-display absolute -top-3 right-4 rotate-3 rounded-lg bg-ink px-2.5 py-1 text-sm text-brand">
          {view.stage}
        </span>
      )}
      <p className="text-xs font-semibold">지금 가장 뜨거운 종목</p>

      <div className="mt-3 flex items-center justify-between gap-3">
        {home && away ? (
          <div className="font-display min-w-0 text-[28px] leading-tight">
            <p className="flex items-center gap-2">
              <Flag team={home} className="text-[22px]" />
              <span className="truncate">{home.name}</span>
            </p>
            <p className="flex items-center gap-2">
              <span className="text-[22px]">vs</span>
              <Flag team={away} className="text-[22px]" />
              <span className="truncate">{away.name}</span>
            </p>
          </div>
        ) : (
          <p className="font-display min-w-0 text-[28px] leading-tight">{room.name}</p>
        )}

        <div className="flex shrink-0 flex-col items-center gap-2">
          {live && (
            <p className="font-display text-[28px] leading-none tracking-wider">
              {view.score[0]} : {view.score[1]}
            </p>
          )}
          <StatusBadge room={room} now={now} large />
        </div>
      </div>

      <p className="mt-4 flex items-center gap-2 text-sm">
        <AntIcon className="w-5" />
        {countFormat.format(room.participantCount)}명 참전
      </p>

      <Link
        href={`/rooms/${room.inviteCode}`}
        className="mt-4 flex h-14 items-center justify-center gap-2 rounded-xl bg-ink text-base font-bold text-white active:translate-y-px"
      >
        응원석 들어가기
        <ArrowIcon className="size-5" />
      </Link>
    </section>
  );
}

function StatusBadge({ room, now, large }: { room: Room; now: number; large?: boolean }) {
  const size = large ? "px-3 py-1 text-xs" : "px-2 py-0.5 text-[10px]";
  if (room.status === "LIVE") {
    return <span className={`rounded-full bg-ink font-bold text-white ${size}`}>{liveMinute(room, now)}</span>;
  }
  if (room.status === "ENDED") {
    return <span className={`rounded-full bg-neutral-400 font-bold text-white ${size}`}>장 마감</span>;
  }
  const kickoff = room.match ? `${kickoffFormat.format(new Date(room.match.kickoffAt))} 상장 예정` : "상장 예정";
  return <span className={`rounded-full bg-ink font-bold text-white ${size}`}>{kickoff}</span>;
}

function RoomRow({ view, now }: { view: RoomView; now: number }) {
  const { room, home, away } = view;
  const up = view.changeRate >= 0;
  const ended = room.status === "ENDED";
  const waiting = room.status === "WAITING";
  const color = ended ? "text-neutral-500" : up ? "text-up" : "text-down";

  return (
    <Link
      href={`/rooms/${room.inviteCode}`}
      className={`flex items-center gap-2 px-3 py-3 ${ended ? "bg-neutral-100" : "bg-white"}`}
    >
      <div className="min-w-0 flex-1">
        {home && away ? (
          <p className="flex items-center gap-1 text-sm font-bold tracking-tight">
            <Flag team={home} />
            <span className="min-w-0 truncate">{home.name}</span>
            <span className="text-xs font-normal text-neutral-500">vs</span>
            <Flag team={away} />
            <span className="min-w-0 truncate">{away.name}</span>
          </p>
        ) : (
          <p className="truncate text-[15px] font-bold">{room.name}</p>
        )}
        <p className="mt-1.5 flex items-center gap-2 text-xs text-neutral-500">
          <StatusBadge room={room} now={now} />
          {ended ? (
            // 결산 화면이 아직 없어 문구만 보여 준다.
            <span>리포트 보기</span>
          ) : (
            <span>
              {waiting ? "대기 개미" : "개미"} {countFormat.format(room.participantCount)}
            </span>
          )}
        </p>
      </div>

      {waiting ? (
        <span className="shrink-0 rounded-md border-2 border-ink bg-brand px-2 py-0.5 text-xs font-bold">공모 중</span>
      ) : (
        <>
          <Sparkline series={view.series} className={`w-12 shrink-0 ${color}`} />
          <div className="w-[68px] shrink-0 text-right font-mono tracking-tight">
            <p className="text-sm font-bold">{indexFormat.format(view.index)}</p>
            {ended ? (
              <p className={`mt-0.5 text-xs font-bold ${up ? "text-up" : "text-down"}`}>{formatRate(view.changeRate)}</p>
            ) : (
              <p
                className={`mt-0.5 inline-block rounded px-1.5 py-0.5 text-xs font-bold text-white ${up ? "bg-up" : "bg-down"}`}
              >
                {formatRate(view.changeRate)}
              </p>
            )}
          </div>
        </>
      )}
    </Link>
  );
}

function BottomNav() {
  return (
    <nav className="sticky bottom-0 grid grid-cols-3 border-t-2 border-ink bg-white pb-[env(safe-area-inset-bottom)] text-xs">
      <span className="flex flex-col items-center gap-1 py-2.5 font-bold" aria-current="page">
        <TrendIcon className="size-5" />
        오늘의 장
      </span>
      {/* 아래 두 탭은 API가 없어 비활성으로 둔다. */}
      <span className="flex flex-col items-center gap-1 py-2.5 text-neutral-400" aria-disabled="true">
        <ReceiptIcon className="size-5" />
        내 거래 기록
      </span>
      <span className="flex flex-col items-center gap-1 py-2.5 text-neutral-400" aria-disabled="true">
        <TrophyIcon className="size-5" />
        명예의 전당
      </span>
    </nav>
  );
}

function Message({ children }: { children: React.ReactNode }) {
  return <p className="py-16 text-center text-sm text-neutral-500">{children}</p>;
}
