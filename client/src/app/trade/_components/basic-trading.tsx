"use client";

import Link from "next/link";
import { useState } from "react";
import { formatCount, formatPrice, isUp } from "../_lib/format";
import { MOCK_ROOM } from "../_lib/mock-room";
import { LIMIT_PER_SECOND, type Side, type TradingSession } from "../_lib/use-trading-session";
import { BurstLayer, useBursts } from "./bursts";
import { ChangeText } from "./change-text";
import { PriceChart, VolumeBars } from "./price-chart";

type Props = { session: TradingSession; onTapMode: () => void };

const ICON_BUTTON =
  "grid size-11 place-items-center rounded-xl border-2 border-ink shadow-[2px_2px_0_var(--color-ink)] active:translate-y-0.5 active:shadow-none";

function mood(secondRate: number) {
  if (secondRate > 0.3) return "가즈아 중";
  if (secondRate < -0.3) return "멘붕 중";
  return "눈치보는 중";
}

export function BasicTrading({ session, onTapMode }: Props) {
  const { ticks, events, pending, total, price, secondRate, press } = session;
  const up = isUp(price);
  const volume = ticks.reduce((acc, t) => ({ buy: acc.buy + t.buy, sell: acc.sell + t.sell }), { buy: 0, sell: 0 });
  const { bursts, spawn } = useBursts();
  const [copied, setCopied] = useState(false);

  const onPress = (side: Side) => {
    if (pending[side] >= LIMIT_PER_SECOND) return;
    press(side);
    navigator.vibrate?.(10);
    if (side === "buy") spawn(62 + Math.random() * 28, 10 + Math.random() * 30);
  };

  const copyLink = async () => {
    await navigator.clipboard.writeText(location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <main className="mx-auto flex min-h-dvh w-full max-w-[430px] flex-col bg-white px-4 pb-6 pt-4">
      <header className="flex items-start justify-between">
        <Link href="/" aria-label="뒤로" className={`${ICON_BUTTON} bg-white`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </Link>
        <div className="text-center">
          <p className="text-xs text-muted">{MOCK_ROOM.title}</p>
          <h1 className="mt-0.5 text-lg font-black tracking-tight">
            {MOCK_ROOM.home} {MOCK_ROOM.score[0]} : {MOCK_ROOM.score[1]} {MOCK_ROOM.away}
          </h1>
        </div>
        <button type="button" onClick={copyLink} aria-label="초대 링크 복사" className={`${ICON_BUTTON} relative bg-fan`}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M10 14a4 4 0 005.7 0l3-3a4 4 0 00-5.7-5.7l-1 1" strokeLinecap="round" />
            <path d="M14 10a4 4 0 00-5.7 0l-3 3a4 4 0 005.7 5.7l1-1" strokeLinecap="round" />
          </svg>
          {copied && (
            <span className="absolute right-0 top-full mt-2 whitespace-nowrap rounded-md bg-ink px-2 py-1 text-xs text-white">
              복사됨
            </span>
          )}
        </button>
      </header>

      <div className="mt-3 flex justify-center gap-2">
        <span className="flex items-center gap-1.5 rounded-full bg-ink px-3 py-1 text-xs font-bold text-white">
          <span className="size-2 rounded-full bg-fan" />
          LIVE {MOCK_ROOM.minute}&apos;
        </span>
        <span className="flex items-center gap-1.5 rounded-full border-2 border-ink px-3 py-0.5 text-xs font-medium">
          🐜 개미 {formatCount(MOCK_ROOM.participants)}명 참전 중
        </span>
      </div>

      <section className="mt-5">
        <div className="flex items-center gap-2">
          <span className="rounded-md bg-ink px-1.5 py-0.5 text-sm font-black text-fan">{MOCK_ROOM.ticker}</span>
          <span className="text-xs text-muted">킥오프 상장가 {formatPrice(1000)}</span>
        </div>
        <div className="flex items-start justify-between">
          <div className={up ? "text-buy" : "text-sell"}>
            <p className="font-num text-5xl font-extrabold tracking-tight tabular-nums">{formatPrice(price)}</p>
            <p className="mt-1 font-num text-sm font-bold tabular-nums"><ChangeText price={price} /></p>
          </div>
          <span className="mt-3 -rotate-6 rounded-lg border-2 border-ink bg-white px-2.5 py-1.5 text-sm font-black shadow-[2px_2px_0_var(--color-ink)]">
            {mood(secondRate)}
          </span>
        </div>
      </section>

      <section className="mt-4 rounded-2xl border-2 border-ink bg-white p-3.5 shadow-[0_3px_0_var(--color-ink)]">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <h2 className="text-sm font-black">팬심 차트</h2>
            <span className="rounded border border-ink px-1.5 text-[11px] font-bold">1초봉</span>
          </div>
          <span className="flex items-center gap-1 text-xs font-bold">
            전체 보기 · 댓글
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M7 17L17 7M9 7h8v8" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </span>
        </div>
        <PriceChart
          ticks={ticks}
          events={events}
          lineColor={up ? "var(--color-buy)" : "var(--color-sell)"}
          fillColor={up ? "rgb(232 38 47 / 0.1)" : "rgb(45 91 227 / 0.1)"}
          baselineColor="#9a9a9a"
          baselineLabel="상장가"
          className="mt-6 h-44"
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

      <section className="mt-4 rounded-2xl border-2 border-dashed border-ink p-3.5">
        <div className="flex items-center justify-between text-sm">
          <div className="flex items-center gap-3">
            <span className="font-black">이번 1초 내 주문</span>
            <OrderDots count={pending.sell} side="sell" />
            <OrderDots count={pending.buy} side="buy" />
          </div>
          <span className="text-xs text-muted">
            누적 <span className="font-num font-bold text-ink">{total}회</span>
          </span>
        </div>
        <div className="mt-2.5 h-1.5 overflow-hidden rounded-full bg-neutral-200">
          <div key={ticks[ticks.length - 1].seq} className="h-full animate-fill bg-ink" />
        </div>
        <p className="mt-2 text-xs text-neutral-700">모았다가 1초마다 한 번에 체결돼요. 계속 눌러요!</p>
      </section>

      <div className="relative mt-auto grid grid-cols-2 gap-3 pt-8">
        <BurstLayer bursts={bursts}>
          <span className="block whitespace-nowrap rounded-xl border-2 border-ink bg-fan px-3 py-1 text-lg font-black shadow-[2px_2px_0_var(--color-ink)]">
            +1 팬심
          </span>
        </BurstLayer>
        <TradeButton side="sell" title="매도" caption="손절각 · 열받음" onPress={onPress} maxed={pending.sell >= LIMIT_PER_SECOND} />
        <TradeButton side="buy" title="매수" caption="가즈아 · 설렘" onPress={onPress} maxed={pending.buy >= LIMIT_PER_SECOND} />
      </div>

      <button type="button" onClick={onTapMode} className="mt-4 text-center text-sm font-bold">
        손가락이 근질근질하면? <span className="underline underline-offset-4">연타 모드로 전환</span>
      </button>
    </main>
  );
}

function OrderDots({ count, side }: { count: number; side: Side }) {
  const border = side === "buy" ? "border-buy" : "border-sell";
  const fill = side === "buy" ? "bg-buy" : "bg-sell";
  return (
    <span className="flex gap-1" aria-label={`${side === "buy" ? "매수" : "매도"} ${count}회`}>
      {Array.from({ length: LIMIT_PER_SECOND }, (_, i) => (
        <span key={i} className={`size-3 rounded-full border-2 ${border} ${i < count ? fill : "bg-white"}`} />
      ))}
    </span>
  );
}

type TradeButtonProps = {
  side: Side;
  title: string;
  caption: string;
  maxed: boolean;
  onPress: (side: Side) => void;
};

function TradeButton({ side, title, caption, maxed, onPress }: TradeButtonProps) {
  return (
    <button
      type="button"
      onPointerDown={() => onPress(side)}
      className={`flex h-24 touch-manipulation select-none flex-col items-center justify-center rounded-2xl border-2 border-ink text-white shadow-[0_4px_0_var(--color-ink)] transition active:translate-y-1 active:shadow-none ${
        side === "buy" ? "bg-buy" : "bg-sell"
      } ${maxed ? "opacity-80" : ""}`}
    >
      <span className="font-display text-4xl leading-none">{title}</span>
      <span className="mt-1.5 text-xs font-medium">{maxed ? "이번 1초 최대 3회!" : caption}</span>
    </button>
  );
}
