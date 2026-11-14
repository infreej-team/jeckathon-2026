"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { ArrowIcon } from "@/components/lobby/icons";
import { fetchMatchRooms, fetchRoom, RoomNotFoundError, type TeamRoom } from "@/lib/matchRooms";
import { placeholderQuote } from "@/lib/placeholder";
import type { Match } from "@/lib/rooms";
import { stageLabel, teamOf, type TeamView } from "@/lib/teams";

const REFRESH_MS = 10_000;

type Loaded = {
  entry: TeamRoom;
  // 같은 경기의 방. 경기가 없는 방이면 들어온 방 하나만 담는다.
  rooms: TeamRoom[];
};

type Side = {
  team: string;
  view: TeamView;
  fanCount: number;
  index: number;
};

type RoomOption = {
  room: TeamRoom;
  label: string;
  full: boolean;
};

const countFormat = new Intl.NumberFormat("en-US");
const indexFormat = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

function toSide(match: Match, team: string, rooms: TeamRoom[]): Side {
  return {
    team,
    view: teamOf(team),
    // 그 팀 전용 방에 지금 들어와 있는 사람 수의 합
    fanCount: rooms.filter((room) => room.team === team).reduce((sum, room) => sum + room.participantCount, 0),
    // 임시값: 팀별 팬심 지수. 서버에 팀 단위 지수가 없다.
    index: placeholderQuote(`${match.id}:${team}`).index,
  };
}

// 고른 팀의 전용 방과 두 팀이 함께 쓰는 방을 보여 준다. 끝난 방은 뺀다.
// 번호는 끝난 방을 포함해 만든 순서로 매겨서 방이 끝나도 바뀌지 않게 한다.
function toOptions(rooms: TeamRoom[], team: string | null, code: string | null, stage: string | null): RoomOption[] {
  const prefix = stage ? `${stage} ` : "";
  let teamNumber = 0;
  let sharedNumber = 0;
  const options: RoomOption[] = [];
  for (const room of rooms) {
    let label: string;
    if (room.team === null) {
      label = team === null ? room.name : `${prefix}공용-${++sharedNumber}`;
    } else if (room.team === team) {
      label = `${prefix}${code}-${++teamNumber}`;
    } else {
      continue;
    }
    if (room.status !== "ENDED") {
      options.push({ room, label, full: room.participantCount >= room.capacity });
    }
  }
  return options;
}

export function TeamSelect() {
  const { inviteCode } = useParams<{ inviteCode: string }>();
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [error, setError] = useState<"notFound" | "failed" | null>(null);
  const [pickedTeam, setPickedTeam] = useState<string | null>(null);
  const [pickedRoom, setPickedRoom] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    const load = async () => {
      try {
        const entry = await fetchRoom(inviteCode, controller.signal);
        const rooms = entry.match ? await fetchMatchRooms(entry.match.id, controller.signal) : [entry];
        setLoaded({ entry, rooms });
        setError(null);
      } catch (e) {
        if (!controller.signal.aborted) {
          setError(e instanceof RoomNotFoundError ? "notFound" : "failed");
        }
      }
    };
    load();
    const timer = setInterval(load, REFRESH_MS);
    return () => {
      controller.abort();
      clearInterval(timer);
    };
  }, [inviteCode]);

  if (!loaded) {
    return (
      <Shell title={null} subtitle={null}>
        <p className="py-16 text-center text-sm text-neutral-500">
          {error === "notFound" && "방을 찾을 수 없어요."}
          {error === "failed" && "방 정보를 불러오지 못했어요. 잠시 후 다시 시도할게요."}
          {error === null && "방 정보를 불러오는 중이에요."}
        </p>
      </Shell>
    );
  }

  const { entry, rooms } = loaded;
  const match = entry.match;
  const sides = match ? [toSide(match, match.team1, rooms), toSide(match, match.team2, rooms)] : [];
  // 팀을 고르기 전에는 들어온 방의 팀, 없으면 첫 번째 팀
  const side = sides.find((s) => s.team === pickedTeam) ?? sides.find((s) => s.team === entry.team) ?? sides[0] ?? null;
  const stage = match ? stageLabel(match.round, match.groupName) : null;
  const options = toOptions(rooms, side?.team ?? null, side?.view.code ?? null, stage?.replace("월드컵 ", "") ?? null);
  // 방을 고르기 전에는 들어온 방, 그 방에 못 들어가면 자리가 있는 첫 방
  const selected =
    options.find((o) => o.room.inviteCode === pickedRoom && !o.full) ??
    options.find((o) => o.room.inviteCode === entry.inviteCode && !o.full) ??
    options.find((o) => !o.full) ??
    null;

  const title = sides.length === 2 ? `${sides[0].view.name} vs ${sides[1].view.name}` : entry.name;
  // 고른 팀은 쿼리로 넘긴다.
  const enterHref = selected
    ? `/rooms/${selected.room.inviteCode}/live${side ? `?team=${encodeURIComponent(side.team)}` : ""}`
    : null;

  return (
    <Shell title={title} subtitle={stage}>
      <main className="flex-1 px-5 pb-6">
        {side ? (
          <>
            <h1 className="font-display mt-2 text-[28px] leading-[1.25]">
              어느 쪽 <span className="bg-brand px-1">주주</span>가
              <br />
              되실래요?
            </h1>
            <p className="mt-2 text-sm text-neutral-700">같은 팀 팬끼리만 한 방에 모여요.</p>
          </>
        ) : (
          // 경기가 연결되지 않은 방은 팀을 고르지 않는다.
          <h1 className="font-display mt-2 text-[28px] leading-[1.25]">
            <span className="bg-brand px-1">주주</span>가 되실래요?
          </h1>
        )}

        {side && (
          <div className="mt-4 grid grid-cols-2 gap-3" role="radiogroup" aria-label="응원 팀">
            {sides.map((s) => (
              <TeamCard
                key={s.team}
                side={s}
                selected={s.team === side.team}
                onSelect={() => {
                  setPickedTeam(s.team);
                  setPickedRoom(null);
                }}
              />
            ))}
          </div>
        )}

        <section className="mt-7">
          <h2 className="text-base font-bold">{side ? `${side.view.code}팬심 방 고르기` : "방 고르기"}</h2>
          {options.length === 0 ? (
            <p className="mt-3 rounded-2xl border-2 border-dashed border-neutral-400 px-4 py-6 text-center text-sm text-neutral-500">
              아직 열린 방이 없어요.
            </p>
          ) : (
            <ul className="mt-3 space-y-3" role="radiogroup" aria-label="방">
              {options.map((option) => (
                <li key={option.room.inviteCode}>
                  <RoomButton
                    option={option}
                    selected={option.room.inviteCode === selected?.room.inviteCode}
                    onSelect={() => setPickedRoom(option.room.inviteCode)}
                  />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="mt-5 rounded-2xl border-2 border-dashed border-ink px-4 py-4">
          <h2 className="font-display text-[15px]">입장 전에 알아두세요</h2>
          <ul className="mt-3 space-y-2 text-xs text-neutral-800">
            <li>· 진짜 돈은 오가지 않아요. 잃는 건 멘탈뿐.</li>
            <li>· 경기가 끝나면 등락률과 변동성 리포트가 나와요.</li>
          </ul>
        </section>
      </main>

      <div className="sticky bottom-0 bg-white px-4 pb-[calc(env(safe-area-inset-bottom)+1.25rem)] pt-3">
        {selected && enterHref ? (
          <Link
            href={enterHref}
            className="font-display flex h-16 items-center justify-center gap-3 rounded-2xl border-2 border-ink bg-brand text-xl shadow-[0_5px_0_var(--color-ink)] active:translate-y-1 active:shadow-[0_1px_0_var(--color-ink)]"
          >
            {selected.label} 방 입장
            <ArrowIcon className="size-5" />
          </Link>
        ) : (
          <span
            className="font-display flex h-16 items-center justify-center rounded-2xl border-2 border-neutral-400 bg-neutral-200 text-xl text-neutral-500"
            aria-disabled="true"
          >
            들어갈 수 있는 방이 없어요
          </span>
        )}
      </div>
    </Shell>
  );
}

function Shell({
  title,
  subtitle,
  children,
}: {
  title: string | null;
  subtitle: string | null;
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white">
      <header className="flex items-center gap-3 px-4 py-4">
        <Link
          href="/"
          aria-label="로비로 돌아가기"
          className="flex size-11 shrink-0 items-center justify-center rounded-xl border-2 border-ink"
        >
          <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2.4" aria-hidden="true">
            <path d="M14 6l-6 6 6 6" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        <div className="min-w-0">
          {subtitle && <p className="text-xs text-neutral-600">{subtitle}</p>}
          {title && <p className="truncate text-base font-bold">{title}</p>}
        </div>
      </header>
      {children}
    </div>
  );
}

function TeamCard({ side, selected, onSelect }: { side: Side; selected: boolean; onSelect: () => void }) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      onClick={onSelect}
      className={`relative min-w-0 rounded-2xl border-2 border-ink px-3.5 py-3.5 text-left ${
        selected ? "bg-ink text-white" : "bg-white"
      }`}
    >
      {selected && (
        <span className="absolute right-3 top-3 flex size-6 items-center justify-center rounded-full bg-brand text-ink">
          <svg viewBox="0 0 24 24" className="size-4" fill="none" stroke="currentColor" strokeWidth="3" aria-hidden="true">
            <path d="M5 12.5l4.5 4.5L19 7.5" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </span>
      )}
      <p className="flex items-center gap-2 text-xs font-semibold">
        {side.view.flag && (
          <span className={`fi fi-${side.view.flag} shrink-0 rounded-[2px] text-[20px] ring-1 ring-ink/70`} />
        )}
        {side.view.code}팬심
      </p>
      <p className="font-display mt-2 truncate text-[24px] leading-tight">{side.view.name}</p>
      <p className={`mt-2 text-xs ${selected ? "text-white" : "text-neutral-600"}`}>
        주주 {countFormat.format(side.fanCount)}명 · <span className="font-mono">{indexFormat.format(side.index)}</span>
      </p>
    </button>
  );
}

function RoomButton({ option, selected, onSelect }: { option: RoomOption; selected: boolean; onSelect: () => void }) {
  const { room, label, full } = option;
  const ratio = Math.min(room.participantCount / room.capacity, 1);
  const tone = full
    ? "border-neutral-400 bg-white text-neutral-500"
    : selected
      ? "border-ink bg-brand shadow-[0_3px_0_var(--color-ink)]"
      : "border-ink bg-white";

  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={full}
      onClick={onSelect}
      className={`block w-full rounded-2xl border-2 px-3.5 pb-3 pt-2.5 text-left ${tone}`}
    >
      <span className="flex items-center justify-between gap-3">
        <span className="truncate text-[15px] font-bold">{label}</span>
        {full ? (
          <span className="shrink-0 rounded-full bg-neutral-500 px-2 py-0.5 text-[11px] font-bold text-white">만석</span>
        ) : (
          <span className="shrink-0 font-mono text-[13px] font-bold">
            {countFormat.format(room.participantCount)} / {countFormat.format(room.capacity)}
          </span>
        )}
      </span>
      <span
        className={`mt-2 block h-2.5 overflow-hidden rounded-full border ${
          full ? "border-neutral-500 bg-neutral-500" : "border-ink bg-white"
        }`}
      >
        {!full && <span className="block h-full bg-ink" style={{ width: `${ratio * 100}%` }} />}
      </span>
    </button>
  );
}
