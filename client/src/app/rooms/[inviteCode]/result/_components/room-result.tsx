"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";

import { ScoreBar } from "@/components/replays/ScoreBar";
import { fetchAllTicks, replayGrade, type MarketTick } from "@/lib/results";
import type { TeamView } from "@/lib/teams";

import { PriceChart } from "../../live/_components/price-chart";
import { formatCount, formatPrice } from "../../live/_lib/format";
import { LISTING_PRICE, type Tick } from "../../live/_lib/use-live-market";
import { useRoom, type RoomInfo } from "../../live/_lib/use-room";

// 차트에 그릴 최대 점 수. 경기 하나가 수천 초라 그대로 그리면 선이 뭉개진다.
const CHART_POINTS = 150;

type Summary = {
  chart: Tick[];
  high: number;
  low: number;
  buys: number;
  sells: number;
};

// 최고·최저는 서버 결산처럼 상장가를 포함해 센다.
function summarize(ticks: MarketTick[]): Summary {
  const step = Math.max(Math.ceil(ticks.length / CHART_POINTS), 1);
  const chart = ticks
    .filter((_, i) => i % step === 0 || i === ticks.length - 1)
    .map((t) => ({ seq: t.tickId, price: t.indexValue, buy: t.buyCount, sell: t.sellCount }));
  const prices = ticks.map((t) => t.indexValue);
  return {
    chart,
    high: Math.max(LISTING_PRICE, ...prices),
    low: Math.min(LISTING_PRICE, ...prices),
    buys: ticks.reduce((sum, t) => sum + t.buyCount, 0),
    sells: ticks.reduce((sum, t) => sum + t.sellCount, 0),
  };
}

const endedFormat = new Intl.DateTimeFormat("ko-KR", {
  month: "long",
  day: "numeric",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "Asia/Seoul",
});

const formatRate = (rate: number) => `${rate >= 0 ? "+" : "-"}${Math.abs(rate).toFixed(2)}%`;

function duration(startedAt: string | null, endedAt: string | null): string {
  if (!startedAt || !endedAt) return "-";
  const minutes = Math.max(Math.round((Date.parse(endedAt) - Date.parse(startedAt)) / 60_000), 0);
  return `${minutes}분`;
}

export function RoomResult() {
  const { inviteCode } = useParams<{ inviteCode: string }>();
  const { info, error } = useRoom(inviteCode);
  const [summary, setSummary] = useState<Summary | null>(null);
  const [ticksFailed, setTicksFailed] = useState(false);
  const ended = info?.room.status === "ENDED";

  useEffect(() => {
    if (!ended) return;
    const controller = new AbortController();
    fetchAllTicks(inviteCode, controller.signal)
      .then((ticks) => setSummary(summarize(ticks)))
      .catch(() => {
        if (!controller.signal.aborted) setTicksFailed(true);
      });
    return () => controller.abort();
  }, [inviteCode, ended]);

  if (!info) {
    return (
      <Notice>
        {error === "notFound" && "방을 찾을 수 없어요."}
        {error === "failed" && "방 정보를 불러오지 못했어요."}
        {error === null && "결산을 불러오는 중이에요."}
      </Notice>
    );
  }

  if (!ended) {
    return (
      <Notice action={{ href: `/rooms/${inviteCode}/live`, label: "응원석으로 가기" }}>
        아직 장이 마감되지 않았어요. 경기가 끝나면 결산을 볼 수 있어요.
      </Notice>
    );
  }

  return <Report info={info} summary={summary} ticksFailed={ticksFailed} />;
}

function Report({ info, summary, ticksFailed }: { info: RoomInfo; summary: Summary | null; ticksFailed: boolean }) {
  const { room } = info;
  const rate = room.changeRate ?? 0;
  const closing = LISTING_PRICE * (1 + rate / 100);
  const up = rate >= 0;
  const score = room.replayScore;

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white px-4 pb-8 pt-4">
      <header className="flex items-center gap-3">
        <Link
          href="/"
          aria-label="로비로"
          className="flex size-10 shrink-0 items-center justify-center rounded-xl border-2 border-ink"
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        <h1 className="flex min-w-0 items-center gap-1.5 truncate text-lg font-black tracking-tight">
          {info.home && info.away ? (
            <>
              <Flag team={info.home} />
              {info.home.name}
              <span className="text-xs font-bold text-muted">vs</span>
              <Flag team={info.away} />
              {info.away.name}
            </>
          ) : (
            room.name
          )}
        </h1>
      </header>

      <section className="mt-6">
        <div className="flex flex-wrap items-center gap-2 text-xs">
          <span className="rounded-full bg-neutral-400 px-3 py-1 font-bold text-white">장 마감</span>
          <span className="rounded-md bg-ink px-2 py-0.5 font-num text-sm font-extrabold text-fan">{info.ticker}</span>
          {room.endedAt && <span className="text-muted">{endedFormat.format(new Date(room.endedAt))} 마감</span>}
        </div>
        <p className="mt-4 text-xs text-muted">종가 · 상장가 {formatPrice(LISTING_PRICE)}</p>
        <div className={up ? "text-up" : "text-down"}>
          <p className="font-num text-5xl font-extrabold tracking-tight tabular-nums">{formatPrice(closing)}</p>
          <p className="mt-1 font-num text-base font-bold tabular-nums">
            {up ? "▲" : "▼"} {formatRate(rate)}
          </p>
        </div>
      </section>

      <section className="mt-6 rounded-3xl border-2 border-ink bg-brand p-4 shadow-[5px_5px_0_var(--color-ink)]">
        <p className="text-xs font-semibold">명경기 지수</p>
        {score === null ? (
          <p className="mt-2 text-sm font-bold">거래가 없어 명경기 지수를 매기지 않았어요.</p>
        ) : (
          <>
            <div className="mt-1 flex items-end justify-between gap-3">
              <p className="font-display text-[44px] leading-none">
                {Math.round(score)}
                <span className="ml-1 text-lg">/ 100</span>
              </p>
              <span className="mb-1 rounded-lg bg-ink px-2.5 py-1 font-display text-sm text-brand">
                {replayGrade(score)}
              </span>
            </div>
            <ScoreBar score={score} className="mt-3 bg-white" />
            <p className="mt-2 text-[11px] text-neutral-700">출렁임(변동성)이 클수록, 마감 등락이 클수록 점수가 올라요.</p>
          </>
        )}
      </section>

      <section className="mt-6 rounded-2xl border-2 border-ink p-3.5 shadow-[0_3px_0_var(--color-ink)]">
        <h2 className="text-sm font-black">경기 전체 팬심 차트</h2>
        {summary ? (
          <PriceChart
            ticks={summary.chart}
            lineColor={up ? "var(--color-buy)" : "var(--color-sell)"}
            fillColor={up ? "rgb(232 38 47 / 0.1)" : "rgb(45 91 227 / 0.1)"}
            baselineColor="#9a9a9a"
            baselineLabel="상장가"
            className="mt-5 h-40"
          />
        ) : (
          <p className="grid h-40 place-items-center text-xs text-muted">
            {ticksFailed ? "차트를 불러오지 못했어요." : "차트를 불러오는 중이에요."}
          </p>
        )}
      </section>

      <dl className="mt-6 grid grid-cols-2 overflow-hidden rounded-2xl border-2 border-ink text-sm">
        <Stat label="최고가" value={summary ? formatPrice(summary.high) : "-"} className="text-up" />
        <Stat label="최저가" value={summary ? formatPrice(summary.low) : "-"} className="text-down" />
        <Stat label="변동폭" value={room.volatility === null ? "-" : `${room.volatility.toFixed(2)}%`} />
        <Stat label="경기 시간" value={duration(room.startedAt, room.endedAt)} />
        <Stat label="총 매수" value={summary ? formatCount(summary.buys) : "-"} className="text-buy" />
        <Stat label="총 매도" value={summary ? formatCount(summary.sells) : "-"} className="text-sell" />
      </dl>

      <div className="mt-8 grid grid-cols-2 gap-3">
        <Link
          href="/"
          className="flex h-14 items-center justify-center rounded-xl border-2 border-ink text-base font-bold active:translate-y-px"
        >
          로비로
        </Link>
        <Link
          href="/replays"
          className="flex h-14 items-center justify-center rounded-xl bg-ink text-base font-bold text-white active:translate-y-px"
        >
          명경기 순위
        </Link>
      </div>
    </main>
  );
}

function Stat({ label, value, className }: { label: string; value: string; className?: string }) {
  return (
    <div className="border-b border-r border-ink px-3 py-3 [&:nth-child(2n)]:border-r-0 [&:nth-last-child(-n+2)]:border-b-0">
      <dt className="text-xs text-muted">{label}</dt>
      <dd className={`mt-1 font-num text-lg font-bold tabular-nums ${className ?? ""}`}>{value}</dd>
    </div>
  );
}

function Flag({ team }: { team: TeamView }) {
  if (!team.flag) return null;
  return <span className={`fi fi-${team.flag} shrink-0 rounded-[2px] ring-1 ring-ink/70`} />;
}

function Notice({ children, action }: { children: React.ReactNode; action?: { href: string; label: string } }) {
  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col items-center justify-center gap-4 bg-white px-4 text-center text-sm">
      <p className="text-muted">{children}</p>
      <Link href={action?.href ?? "/"} className="rounded-xl border-2 border-ink px-4 py-2 font-bold">
        {action?.label ?? "로비로 가기"}
      </Link>
    </main>
  );
}
