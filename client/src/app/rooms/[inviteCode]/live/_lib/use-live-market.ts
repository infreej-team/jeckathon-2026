"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { RoomStatus } from "@/lib/rooms";
import { webSocketUrl } from "@/lib/userId";

// 서버 MarketIndex.INITIAL_VALUE. 결산 등락률도 이 값을 기준으로 계산한다.
export const LISTING_PRICE = 100;
// 서버 RoomBucket.LIMIT_PER_USER와 같은 값이다.
export const LIMIT_PER_SECOND = 3;
const WINDOW = 40;

export type Side = "buy" | "sell";
export type Counts = Record<Side, number>;
export type Tick = { seq: number; price: number; buy: number; sell: number };
export type Connection = "connecting" | "open" | "closed";

type TickPayload = { tickId: number; indexValue: number; buyCount: number; sellCount: number };

const toTick = (t: TickPayload): Tick => ({ seq: t.tickId, price: t.indexValue, buy: t.buyCount, sell: t.sellCount });

// REST로 받은 과거 틱과 웹소켓으로 받은 틱이 겹칠 수 있어 tickId로 합친다.
function mergeTicks(current: Tick[], incoming: Tick[]): Tick[] {
  const bySeq = new Map(current.map((t) => [t.seq, t]));
  for (const t of incoming) bySeq.set(t.seq, t);
  return [...bySeq.values()].sort((a, b) => a.seq - b.seq).slice(-WINDOW);
}

const ZERO: Counts = { buy: 0, sell: 0 };

// 방 하나의 실시간 시세 연결. 웹소켓 메시지 형식은 서버 room/ws, trade, market의 *Message 주석을 따른다.
export function useLiveMarket(inviteCode: string) {
  const [ticks, setTicks] = useState<Tick[]>([]);
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

  const writePending = (next: Counts) => {
    pendingRef.current = next;
    setPending(next);
  };

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/rooms/${encodeURIComponent(inviteCode)}/market/ticks?size=${WINDOW}`, { signal: controller.signal })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(String(res.status)))))
      .then((body: { items: TickPayload[] }) => setTicks((current) => mergeTicks(current, body.items.map(toTick))))
      .catch(() => {});
    return () => controller.abort();
  }, [inviteCode]);

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
            setTicks((current) => mergeTicks(current, [toTick(message)]));
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
