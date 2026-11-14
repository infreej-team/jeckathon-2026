"use client";

import Link from "next/link";
import { useState } from "react";
import type { TeamView } from "@/lib/teams";
import { formatCount, formatPrice, isUp, statusLabel } from "../_lib/format";
import type { useComments } from "../_lib/use-comments";
import { LIMIT_PER_SECOND, LISTING_PRICE, type LiveMarket, type Side } from "../_lib/use-live-market";
import { BurstLayer, useBursts } from "./bursts";
import { ChangeText } from "./change-text";
import type { LiveView } from "./live-room";
import { LiveComments } from "./live-comments";
import { PriceChart, VolumeBars } from "./price-chart";

type Props = {
  view: LiveView;
  market: LiveMarket;
  comments: ReturnType<typeof useComments>;
  onTapMode: () => void;
};

const ICON_BUTTON =
  "grid size-11 shrink-0 place-items-center rounded-xl border-2 border-ink shadow-[2px_2px_0_var(--color-ink)] active:translate-y-0.5 active:shadow-none";

export function Flag({ team }: { team: TeamView | null }) {
  if (!team?.flag) return null;
  return <span aria-hidden className={`fi fi-${team.flag} shrink-0 rounded-[2px] ring-1 ring-ink/70`} />;
}

// 경기 중이 아니거나 연결이 끊겼을 때 버튼 대신 보여 줄 안내. 거래할 수 있으면 null
export function tradeNotice(view: LiveView, market: LiveMarket): string | null {
  if (view.status === "WAITING") return "경기가 시작되면 매수·매도할 수 있어요";
  if (view.status === "ENDED") {
    const rate = view.info.room.changeRate;
    return rate === null ? "경기가 끝났어요" : `경기가 끝났어요 · 최종 ${rate >= 0 ? "+" : ""}${rate.toFixed(2)}%`;
  }
  if (market.connection !== "open") return "연결이 끊겨 다시 연결하는 중이에요";
  return null;
}

export function BasicTrading({ view, market, comments, onTapMode }: Props) {
  const { ticks, pending, price, press, error } = market;
  const { info } = view;
  const up = isUp(price);
  const volume = ticks.reduce((acc, t) => ({ buy: acc.buy + t.buy, sell: acc.sell + t.sell }), { buy: 0, sell: 0 });
  const { bursts, spawn } = useBursts();
  const [copied, setCopied] = useState(false);
  const notice = tradeNotice(view, market);

  const onPress = (side: Side) => {
    if (!press(side)) return;
    navigator.vibrate?.(10);
    if (side === "buy") spawn(62 + Math.random() * 28, 10 + Math.random() * 30);
  };

  // 초대 링크는 응원 팀 고르기 화면으로 보낸다.
  const copyLink = async () => {
    await navigator.clipboard.writeText(`${location.origin}/rooms/${view.inviteCode}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white px-4 pb-6 pt-4">
      <header className="flex items-center justify-between gap-2">
        <Link href={`/rooms/${view.inviteCode}`} aria-label="뒤로" className={`${ICON_BUTTON} bg-white`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        <h1 className="flex min-w-0 items-center gap-1.5 truncate text-lg font-black tracking-tight">
          {info.home && info.away ? (
            <>
              {info.home.name}
              <Flag team={info.home} />
              <span className="text-xs font-bold text-muted">vs</span>
              <Flag team={info.away} />
              {info.away.name}
            </>
          ) : (
            info.room.name
          )}
        </h1>
        <button type="button" onClick={copyLink} aria-label="초대 링크 복사" className={`${ICON_BUTTON} relative bg-fan`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M10 14a4 4 0 005.7 0l3-3a4 4 0 00-5.7-5.7l-1 1" strokeLinecap="round" />
            <path d="M14 10a4 4 0 00-5.7 0l-3 3a4 4 0 005.7 5.7l1-1" strokeLinecap="round" />
          </svg>
          {copied && (
            <span className="absolute right-0 top-full z-20 mt-2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-xs text-white">
              복사됨
            </span>
          )}
        </button>
      </header>

      <div className="mt-3 flex justify-center gap-2">
        <span className="flex items-center gap-1.5 rounded-full bg-ink px-3 py-1 text-xs font-bold text-white">
          <span className={`size-2 rounded-full ${view.status === "LIVE" ? "bg-fan" : "bg-neutral-500"}`} />
          {statusLabel(view.status, view.minute)}
        </span>
        <span className="flex items-center gap-1.5 rounded-full border-2 border-ink px-3 py-0.5 text-xs font-medium">
          🐜 {formatCount(view.participantCount)}명 참전 중
        </span>
      </div>

      <section className="mt-6">
        <div className="flex items-center gap-2">
          <span className="rounded-md bg-ink px-2 py-0.5 font-num text-sm font-extrabold text-fan">{info.ticker}</span>
          <span className="text-xs text-muted">킥오프 상장가 {formatPrice(LISTING_PRICE)}</span>
        </div>
        <div className="mt-2 flex items-end justify-between">
          <div className={up ? "text-buy" : "text-sell"}>
            <p className="font-num text-5xl font-extrabold tracking-tight tabular-nums">{formatPrice(price)}</p>
            <p className="mt-1 font-num text-sm font-bold tabular-nums">
              <ChangeText price={price} />
            </p>
          </div>
          <button
            type="button"
            onClick={onTapMode}
            className="mb-1 rounded-xl border-2 border-ink bg-white px-4 py-2 text-sm font-black shadow-[2px_2px_0_var(--color-ink)] active:translate-y-0.5 active:shadow-none"
          >
            연타 모드
          </button>
        </div>
      </section>

      <section className="mt-4 rounded-2xl border-2 border-ink bg-white p-3.5 shadow-[0_3px_0_var(--color-ink)]">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-black">팬심 차트</h2>
          <span className="flex items-center gap-1 text-xs font-bold">
            전체 보기
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M7 17L17 7M9 7h8v8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </div>
        <PriceChart
          ticks={ticks}
          lineColor={up ? "var(--color-buy)" : "var(--color-sell)"}
          fillColor={up ? "rgb(232 38 47 / 0.1)" : "rgb(45 91 227 / 0.1)"}
          baselineColor="#9a9a9a"
          baselineLabel="상장가"
          className="mt-5 h-36"
        />
        <div className="mt-2 border-t-2 border-ink pt-2">
          <VolumeBars ticks={ticks} />
        </div>
        <div className="mt-2 flex justify-between text-xs">
          <span className="text-muted">거래량 = 클릭 수 (최근 {ticks.length}초)</span>
          <span className="font-num font-bold">
            <span className="text-buy">매수 {formatCount(volume.buy)}</span>
            <span className="text-muted"> · </span>
            <span className="text-sell">매도 {formatCount(volume.sell)}</span>
          </span>
        </div>
      </section>

      <LiveComments comments={comments} startedAt={info.room.startedAt} canPost={view.status !== "WAITING"} />

      {(notice || error) && (
        <p role="status" className="mt-3 text-center text-xs font-bold text-muted">
          {notice ?? error}
        </p>
      )}

      <div className="relative mt-auto grid grid-cols-2 gap-3 pt-4">
        <BurstLayer bursts={bursts}>
          <span className="block whitespace-nowrap rounded-xl border-2 border-ink bg-fan px-3 py-1 text-lg font-black shadow-[2px_2px_0_var(--color-ink)]">
            +1 팬심
          </span>
        </BurstLayer>
        <TradeButton
          side="sell"
          title="매도"
          caption="손절각 · 열받음"
          onPress={onPress}
          disabled={notice !== null}
          maxed={pending.sell >= LIMIT_PER_SECOND}
        />
        <TradeButton
          side="buy"
          title="매수"
          caption="가즈아 · 설렘"
          onPress={onPress}
          disabled={notice !== null}
          maxed={pending.buy >= LIMIT_PER_SECOND}
        />
      </div>
    </main>
  );
}

type TradeButtonProps = {
  side: Side;
  title: string;
  caption: string;
  maxed: boolean;
  disabled: boolean;
  onPress: (side: Side) => void;
};

function TradeButton({ side, title, caption, maxed, disabled, onPress }: TradeButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled}
      onPointerDown={() => onPress(side)}
      className={`flex h-[88px] touch-manipulation select-none flex-col items-center justify-center rounded-2xl border-2 border-ink text-white shadow-[0_4px_0_var(--color-ink)] transition active:translate-y-1 active:shadow-none disabled:translate-y-0 disabled:opacity-40 disabled:shadow-[0_4px_0_var(--color-ink)] ${
        side === "buy" ? "bg-buy" : "bg-sell"
      } ${maxed ? "opacity-80" : ""}`}
    >
      <span className="font-display text-4xl leading-none">{title}</span>
      <span className="mt-1.5 text-xs font-medium">{maxed ? `이번 1초 최대 ${LIMIT_PER_SECOND}회!` : caption}</span>
    </button>
  );
}
