"use client";

import { useCallback, useEffect, useState } from "react";

// 서버 시세 방송이 생기기 전까지 쓰는 로컬 시뮬레이션이다.
// 화면은 이 훅이 돌려주는 값만 읽으므로, 웹소켓을 붙일 때는 이 파일만 바꾼다.

export const LISTING_PRICE = 1000;
// 서버 RoomBucket.LIMIT_PER_USER와 같은 값이다.
export const LIMIT_PER_SECOND = 3;
const WINDOW = 40;

export type Side = "buy" | "sell";
export type Counts = Record<Side, number>;
export type Tick = { seq: number; price: number; buy: number; sell: number };
export type MatchEvent = { seq: number; label: string; side: Side };

type SessionState = {
  ticks: Tick[];
  pending: Counts;
  total: number;
  crowd: Counts;
};

const EVENTS: MatchEvent[] = [
  { seq: 12, label: "23' 실점", side: "sell" },
  { seq: 34, label: "67' 동점골", side: "buy" },
];

// SSR과 첫 렌더가 같은 값을 그리도록 고정 시드를 쓴다.
function seeded(seed: number) {
  let a = seed;
  return () => {
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const round2 = (n: number) => Math.round(n * 100) / 100;

function initialTicks(): Tick[] {
  const rand = seeded(7);
  const ticks: Tick[] = [];
  for (let seq = 0; seq < WINDOW; seq++) {
    let price: number;
    if (seq <= 12) price = 1004 + (rand() - 0.5) * 6;
    else if (seq <= 34) price = 905 + ((seq - 13) / 21) * 18 + (rand() - 0.5) * 7;
    else price = 945 + (rand() - 0.5) * 8;
    if (seq === WINDOW - 1) price = 937.04;

    const down = seq === 13 || (seq > 34 && rand() < 0.4) || (seq > 12 && rand() < 0.55);
    const major = 4 + Math.floor(rand() * 9);
    const minor = Math.floor(rand() * 4);
    ticks.push({
      seq,
      price: round2(price),
      buy: down ? minor : major,
      sell: down ? major : minor,
    });
  }
  return ticks;
}

const INITIAL: SessionState = {
  ticks: initialTicks(),
  pending: { buy: 0, sell: 0 },
  total: 27,
  crowd: { buy: 412, sell: 194 },
};

// 주문 1건이 움직이는 가격. 1초에 3회 연타하면 약 ±0.6% 움직인다.
const PRICE_PER_ORDER = 2;
const clamp = (n: number, min: number, max: number) => Math.min(max, Math.max(min, n));

export function useTradingSession() {
  const [state, setState] = useState<SessionState>(INITIAL);

  useEffect(() => {
    // 1초마다 모인 주문을 한 번에 체결한다. 가격은 매수·매도 차이로만 움직이고, 주문이 없으면 그대로다.
    const id = setInterval(() => {
      setState((s) => {
        const last = s.ticks[s.ticks.length - 1];
        const { buy, sell } = s.pending;
        const net = buy - sell;
        return {
          ticks: [
            ...s.ticks.slice(1 - WINDOW),
            { seq: last.seq + 1, price: Math.max(1, round2(last.price + net * PRICE_PER_ORDER)), buy, sell },
          ],
          pending: { buy: 0, sell: 0 },
          total: s.total,
          crowd: { buy: clamp(s.crowd.buy + net, 0, 1500), sell: clamp(s.crowd.sell - net, 0, 1500) },
        };
      });
    }, 1000);
    return () => clearInterval(id);
  }, []);

  const press = useCallback((side: Side) => {
    setState((s) => {
      if (s.pending[side] >= LIMIT_PER_SECOND) return s;
      return { ...s, pending: { ...s.pending, [side]: s.pending[side] + 1 }, total: s.total + 1 };
    });
  }, []);

  const { ticks } = state;
  const last = ticks[ticks.length - 1];
  const prev = ticks[ticks.length - 2] ?? last;
  const firstSeq = ticks[0].seq;

  return {
    ticks,
    events: EVENTS.filter((e) => e.seq >= firstSeq),
    pending: state.pending,
    total: state.total,
    crowd: state.crowd,
    price: last.price,
    secondRate: ((last.price - prev.price) / prev.price) * 100,
    press,
  };
}

export type TradingSession = ReturnType<typeof useTradingSession>;
