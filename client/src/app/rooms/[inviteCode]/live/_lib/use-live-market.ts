"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { recentSeconds, type TimedTick } from "@/lib/market-series";
import type { RoomStatus } from "@/lib/rooms";
import { webSocketUrl } from "@/lib/userId";

// 서버 MarketIndex.INITIAL_VALUE. 결산 등락률도 이 값을 기준으로 계산한다.
export const LISTING_PRICE = 100;
// 서버 RoomBucket.LIMIT_PER_USER와 같은 값이다.
export const LIMIT_PER_SECOND = 3;
// 차트에 보여 줄 최근 초 수
const WINDOW = 40;

export type Side = "buy" | "sell";
export type Counts = Record<Side, number>;
export type Tick = { seq: number; price: number; buy: number; sell: number };
export type Connection = "connecting" | "open" | "closed";

type TickPayload = { tickId: number; recordedAt: string; indexValue: number; buyCount: number; sellCount: number };
type StoredTick = TimedTick & { seq: number };
// 서버가 보낸 최근 틱과, 그 틱들보다 앞선 시점의 지수. 앞선 지수를 모르면 null.
type History = { ticks: StoredTick[]; base: number | null };

const toStored = (t: TickPayload): StoredTick => ({
  seq: t.tickId,
  time: Date.parse(t.recordedAt),
  price: t.indexValue,
  buy: t.buyCount,
  sell: t.sellCount,
});

// REST로 받은 과거 틱과 웹소켓으로 받은 틱이 겹칠 수 있어 tickId로 합친다.
// 틱은 1초에 많아야 하나라서 최근 WINDOW개면 차트 구간을 덮는다. complete는 incoming이 방의 첫 틱부터 담았다는 뜻이다.
function mergeTicks(current: History, incoming: StoredTick[], complete = false): History {
  const bySeq = new Map(current.ticks.map((t) => [t.seq, t]));
  for (const t of incoming) bySeq.set(t.seq, t);
  const all = [...bySeq.values()].sort((a, b) => a.seq - b.seq);
  const cut = Math.max(all.length - WINDOW, 0);
  const base = cut > 0 ? all[cut - 1].price : complete ? LISTING_PRICE : current.base;
  return { ticks: all.slice(cut), base };
}

const ZERO: Counts = { buy: 0, sell: 0 };

// 방 하나의 실시간 시세 연결. 웹소켓 메시지 형식은 서버 room/ws, trade, market의 *Message 주석을 따른다.
export function useLiveMarket(inviteCode: string) {
  const [history, setHistory] = useState<History>({ ticks: [], base: null });
  const [now, setNow] = useState(() => Date.now());
  const [pending, setPending] = useState<Counts>(ZERO);
  const [status, setStatus] = useState<RoomStatus | null>(null);
  const [participantCount, setParticipantCount] = useState<number | null>(null);
  const [connection, setConnection] = useState<Connection>("connecting");
  const [error, setError] = useState<string | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  // 같은 렌더 안의 연타도 한도를 넘지 않게 상태와 별도로 동기 값을 둔다.
  const pendingRef = useRef<Counts>(ZERO);
  // 보낸 순서대로 ack가 오므로, ack가 어느 쪽 주문의 응답인지 이 큐로 맞춘다.
  const sentRef = useRef<Side[]>([]);
  // 서버 시각 - 브라우저 시각. 틱 시각과 차트 끝 시각을 같은 시계로 맞춘다.
  const clockOffsetRef = useRef(0);

  const writePending = (next: Counts) => {
    pendingRef.current = next;
    setPending(next);
  };

  useEffect(() => {
    const controller = new AbortController();
    // 하나 더 받아서, 차트 구간보다 앞선 지수를 알아낸다.
    fetch(`/api/rooms/${encodeURIComponent(inviteCode)}/market/ticks?size=${WINDOW + 1}`, { signal: controller.signal })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(String(res.status)))))
      .then((body: { items: TickPayload[] }) =>
        setHistory((current) => mergeTicks(current, body.items.map(toStored), body.items.length <= WINDOW)),
      )
      .catch(() => {});
    return () => controller.abort();
  }, [inviteCode]);

  // 거래가 없으면 틱이 오지 않으므로 차트를 1초마다 직접 밀어 준다.
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now() + clockOffsetRef.current), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    let disposed = false;
    let attempt = 0;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;

    const connect = () => {
      const ws = new WebSocket(webSocketUrl());
      wsRef.current = ws;
      setConnection("connecting");

      ws.onopen = () => {
        attempt = 0;
        setConnection("open");
        ws.send(JSON.stringify({ type: "room.join", roomId: inviteCode }));
      };

      ws.onmessage = (event) => {
        const message = JSON.parse(event.data);
        switch (message.type) {
          case "room.joined":
            setError(null);
            setStatus(message.status);
            setParticipantCount(message.participantCount);
            break;
          case "room.participants":
            setParticipantCount(message.participantCount);
            break;
          case "room.status":
            setStatus(message.status);
            break;
          case "market.tick":
            clockOffsetRef.current = Date.parse(message.recordedAt) - Date.now();
            setHistory((current) => mergeTicks(current, [toStored(message)]));
            // 서버가 1초 구간을 체결했으므로 이번 구간 주문 수를 비운다.
            writePending(ZERO);
            sentRef.current = [];
            break;
          case "trade.ack": {
            const side = sentRef.current.shift();
            if (side && message[side] === 0 && pendingRef.current[side] > 0) {
              writePending({ ...pendingRef.current, [side]: pendingRef.current[side] - 1 });
            }
            break;
          }
          case "error":
            setError(message.message);
            break;
        }
      };

      ws.onclose = () => {
        if (disposed) return;
        setConnection("closed");
        sentRef.current = [];
        retryTimer = setTimeout(connect, Math.min(1000 * 2 ** attempt++, 10_000));
      };
    };

    connect();
    return () => {
      disposed = true;
      clearTimeout(retryTimer);
      wsRef.current?.close();
    };
  }, [inviteCode]);

  // 주문을 보냈으면 true. 경기 중이 아니거나 이번 1초 한도를 다 썼으면 false.
  const press = useCallback(
    (side: Side) => {
      const ws = wsRef.current;
      if (status !== "LIVE" || !ws || ws.readyState !== WebSocket.OPEN) return false;
      if (pendingRef.current[side] >= LIMIT_PER_SECOND) return false;
      writePending({ ...pendingRef.current, [side]: pendingRef.current[side] + 1 });
      sentRef.current.push(side);
      ws.send(
        JSON.stringify({ type: "trade", roomId: inviteCode, buy: side === "buy" ? 1 : 0, sell: side === "sell" ? 1 : 0 }),
      );
      return true;
    },
    [inviteCode, status],
  );

  const ticks = useMemo<Tick[]>(() => {
    const stored = history.ticks;
    const lastTime = stored.at(-1)?.time;
    // 경기 중이 아니면 마지막 틱에서 멈춘다. 경기 전이라 틱이 없으면 빈 차트를 보여 준다.
    if (status !== "LIVE" && lastTime === undefined) return [];
    const end = status === "LIVE" ? Math.max(now, lastTime ?? now) : (lastTime ?? now);
    const base = history.base ?? stored[0]?.price ?? LISTING_PRICE;
    return recentSeconds(stored, end, WINDOW, base).map((t) => ({ seq: t.time, price: t.price, buy: t.buy, sell: t.sell }));
  }, [history, now, status]);

  const last = ticks[ticks.length - 1];
  const prev = ticks[ticks.length - 2] ?? last;
  const price = last?.price ?? LISTING_PRICE;

  return {
    ticks,
    pending,
    status,
    participantCount,
    connection,
    error,
    price,
    secondRate: last && prev ? ((last.price - prev.price) / prev.price) * 100 : 0,
    press,
  };
}

export type LiveMarket = ReturnType<typeof useLiveMarket>;
