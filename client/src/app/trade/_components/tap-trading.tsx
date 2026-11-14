"use client";

import { useEffect, useState } from "react";
import { formatCount, formatPrice, isUp } from "../_lib/format";
import { MOCK_ROOM } from "../_lib/mock-room";
import { LIMIT_PER_SECOND, type Side, type TradingSession } from "../_lib/use-trading-session";
import { BurstLayer, useBursts } from "./bursts";
import { Arrow, ChangeText } from "./change-text";
import { PriceChart } from "./price-chart";

type Props = { session: TradingSession; onBasicMode: () => void };

// 연타 중 화면이 꺼지지 않게 한다. 지원하지 않는 브라우저에서는 조용히 넘어간다.
function useWakeLock() {
  useEffect(() => {
    let lock: WakeLockSentinel | undefined;
    let released = false;
    navigator.wakeLock
      ?.request("screen")
      .then((l) => {
        if (released) l.release();
        else lock = l;
      })
      .catch(() => {});
    return () => {
      released = true;
      lock?.release().catch(() => {});
    };
  }, []);
}

export function TapTrading({ session, onBasicMode }: Props) {
  const { ticks, pending, crowd, price, secondRate, press } = session;
  useWakeLock();

  const crowdTotal = crowd.buy + crowd.sell;
  const buyShare = Math.round((crowd.buy / crowdTotal) * 100);

  return (
    <main className="mx-auto flex h-dvh w-full max-w-[430px] flex-col overflow-hidden bg-ink text-white">
      <TapZone side="buy" pending={pending.buy} onPress={press}>
        <div className="flex items-center justify-between px-4 pt-4">
          <button
            type="button"
            onPointerDown={(e) => e.stopPropagation()}
            onClick={onBasicMode}
            className="flex items-center gap-1.5 rounded-full bg-black/20 px-4 py-2.5 text-sm font-bold"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
              <path d="M15 5l-7 7 7 7" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            기본 모드
          </button>
          <span className="rounded-full bg-ink px-3 py-1.5 text-xs font-bold">
            LIVE {MOCK_ROOM.minute}&apos; · {MOCK_ROOM.score[0]} : {MOCK_ROOM.score[1]}
          </span>
        </div>
      </TapZone>

      <section className="px-4 py-4">
        <div className="flex items-end justify-between">
          <div>
            <p className="text-xs font-black text-fan">{MOCK_ROOM.ticker}</p>
            <p className="font-num text-4xl font-extrabold tabular-nums">{formatPrice(price)}</p>
          </div>
          <div className="text-right font-num text-sm font-bold tabular-nums">
            <p className={isUp(price) ? "text-buy-soft" : "text-sell-soft"}><ChangeText price={price} /></p>
            <p className={`mt-1 ${secondRate >= 0 ? "text-buy-soft" : "text-sell-soft"}`}>
              <span className="font-sans">이번 1초 </span>
              <Arrow up={secondRate >= 0} />
              {Math.abs(secondRate).toFixed(2)}%
            </p>
          </div>
        </div>
        <PriceChart
          ticks={ticks}
          lineColor="var(--color-buy-soft)"
          baselineColor="#555"
          className="mt-3 h-12"
        />
        <div className="mt-4 flex justify-between text-xs font-bold">
          <span className="text-buy-soft">
            지금 매수 중 {formatCount(crowd.buy)}명 ({buyShare}%)
          </span>
          <span className="text-sell-soft">
            매도 중 {formatCount(crowd.sell)}명 ({100 - buyShare}%)
          </span>
        </div>
        <div className="mt-1.5 flex h-4 overflow-hidden rounded-md border-2 border-white">
          <div className="bg-buy transition-[width] duration-500" style={{ width: `${buyShare}%` }} />
          <div className="flex-1 bg-sell" />
        </div>
      </section>

      <TapZone side="sell" pending={pending.sell} onPress={press}>
        <div className="absolute inset-x-0 bottom-6 flex justify-center">
          <span className="flex items-center gap-1.5 rounded-full bg-black/25 px-4 py-2 text-xs font-medium">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="7" y="3" width="10" height="18" rx="2" />
              <path d="M3 9v6M21 9v6" strokeLinecap="round" />
            </svg>
            누를 때마다 진동 · 화면 꺼짐 방지 ON
          </span>
        </div>
      </TapZone>
    </main>
  );
}

const ZONE = {
  buy: {
    title: "매수",
    caption: "설레면 아무 데나 연타",
    className: "rounded-b-[32px] bg-buy",
    chip: "bg-[#ffb3b8] text-buy",
  },
  sell: {
    title: "매도",
    caption: "열받으면 아무 데나 연타",
    className: "rounded-t-[32px] bg-sell",
    chip: "bg-[#b3c4ff] text-sell",
  },
} as const;

type TapZoneProps = {
  side: Side;
  pending: number;
  onPress: (side: Side) => void;
  children: React.ReactNode;
};

// 영역 어디를 눌러도 주문이 들어간다. 한 손가락 연타가 목적이라 pointerdown에서 바로 처리한다.
function TapZone({ side, pending, onPress, children }: TapZoneProps) {
  const zone = ZONE[side];
  const { bursts, spawn } = useBursts();
  const [pulse, setPulse] = useState(0);
  const maxed = pending >= LIMIT_PER_SECOND;

  const handle = () => {
    if (maxed) return;
    onPress(side);
    navigator.vibrate?.(15);
    setPulse((p) => p + 1);
    spawn(15 + Math.random() * 70, 35 + Math.random() * 35);
  };

  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`${zone.title} 연타`}
      onPointerDown={handle}
      onKeyDown={(e) => (e.key === " " || e.key === "Enter") && handle()}
      className={`relative flex flex-1 touch-manipulation select-none flex-col overflow-hidden ${zone.className}`}
    >
      {children}

      <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center">
        {side === "buy" &&
          [96, 176, 256].map((size) => (
            <span
              key={size}
              className="absolute left-[60%] top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-white/25"
              style={{ width: size, height: size }}
            />
          ))}
        {pulse > 0 && (
          <span
            key={pulse}
            className="absolute left-1/2 top-1/2 size-56 animate-ring rounded-full border-4 border-white/60"
          />
        )}
        <p className="relative font-display text-[88px] leading-none">{zone.title}</p>
        <p className="relative mt-2 text-base font-bold">{zone.caption}</p>
      </div>

      {maxed && (
        <span className="pointer-events-none absolute right-6 top-[62%] -rotate-6 animate-pop rounded-xl border-2 border-ink bg-fan px-3 py-1.5 font-num text-xl font-extrabold text-ink shadow-[2px_2px_0_var(--color-ink)]"
        >
          x{LIMIT_PER_SECOND} MAX!
        </span>
      )}

      <BurstLayer bursts={bursts}>
        <span
          className={`block rounded-lg border-2 border-ink px-2.5 py-0.5 font-num text-lg font-extrabold shadow-[2px_2px_0_var(--color-ink)] ${zone.chip}`}
        >
          +1
        </span>
      </BurstLayer>
    </div>
  );
}
