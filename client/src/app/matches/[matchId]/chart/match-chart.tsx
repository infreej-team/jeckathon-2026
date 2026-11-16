'use client';

import Link from 'next/link';
import { useEffect, useRef, useState } from 'react';
import { recentSeconds } from '@/lib/market-series';
import styles from './chart.module.css';

type Status = 'WAITING' | 'LIVE' | 'ENDED';
type Room = { roomId: number; inviteCode: string; name: string; status: Status };
type Tick = { tickId: number; recordedAt: string; indexValue: number; buyCount: number; sellCount: number };
const statusText = { WAITING: '경기 대기', LIVE: '경기 진행 중', ENDED: '경기 종료' };
// 차트에 보여 줄 최근 초 수
const CHART_SECONDS = 120;
// 서버 MarketIndex.INITIAL_VALUE
const LISTING_PRICE = 100;

async function get<T>(url: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal, cache: 'no-store' });
  if (!response.ok) throw new Error('서버 응답 오류 (' + response.status + ')');
  return response.json();
}

function Trend({ ticks, live }: { ticks: Tick[]; live: boolean }) {
  const values = ticks.map((tick) => tick.indexValue);
  const min = Math.min(...values) - 0.1;
  const max = Math.max(...values) + 0.1;
  const points = ticks.map((tick, i) => ({
    x: 36 + 295 * i / Math.max(ticks.length - 1, 1),
    y: 110 - 96 * (tick.indexValue - min) / (max - min),
  }));
  const last = points.at(-1);
  const maxVolume = Math.max(1, ...ticks.map((tick) => tick.buyCount + tick.sellCount));
  const barWidth = Math.min(6, 180 / ticks.length);
  const formatTime = new Intl.DateTimeFormat("ko-KR", { hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" });

  return (
    <svg className={styles.chart} viewBox="0 0 344 160" role="img" aria-label="시간순 팬심 지수">
      {[14, 62, 110].map((y, i) => (
        <g key={y}>
          <line x1="36" x2="331" y1={y} y2={y} />
          <text x="0" y={y + 3}>{(max - i * (max - min) / 2).toFixed(1)}</text>
        </g>
      ))}
      {live && last && <line className={styles.liveCursor} x1={last.x} x2={last.x} y1="14" y2="110" />}
      <polyline points={points.map(({ x, y }) => x + ',' + y).join(' ')} />
      {last && <circle cx={last.x} cy={last.y} r="4" />}
      {ticks.map((tick, i) => {
        const height = 19 * (tick.buyCount + tick.sellCount) / maxVolume;
        return <rect key={tick.tickId} x={points[i].x - barWidth / 2} y={136 - height}
          width={barWidth} height={height} fill={tick.buyCount >= tick.sellCount ? "#e01f35" : "#2b59f0"} />;
      })}
      <text x="36" y="153">{formatTime.format(new Date(ticks[0].recordedAt))}</text>
      {ticks.length > 1 && <text x="331" y="153" textAnchor="end">{formatTime.format(new Date(ticks[ticks.length - 1].recordedAt))}</text>}
    </svg>
  );
}

export default function MatchChart({ inviteCode }: { inviteCode: string }) {
  const path = '/matches/' + encodeURIComponent(inviteCode);
  const [room, setRoom] = useState<Room | null>(null);
  const [status, setStatus] = useState<Status | null>(null);
  // 서버는 거래가 있던 초에만 틱을 저장한다. base는 이 틱들보다 앞선 시점의 지수다.
  const [history, setHistory] = useState<{ ticks: Tick[]; base: number; fetchedAt: number }>(
    { ticks: [], base: LISTING_PRICE, fetchedAt: 0 },
  );
  const [participants, setParticipants] = useState<number | null>(null);
  const [joined, setJoined] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [feedback, setFeedback] = useState('');
  const tradeSocket = useRef<WebSocket | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setTimeout>;
    const api = '/api/rooms/' + encodeURIComponent(inviteCode);
    async function refresh() {
      try {
        const data = await get<Room>(api, controller.signal);
        setRoom(data);
        setStatus(data.status);
        // 하나 더 받아서, 차트 구간보다 앞선 지수를 알아낸다.
        const page = await get<{ items: Tick[] }>(
          '/api/rooms/' + data.roomId + '/market/ticks?size=' + (CHART_SECONDS + 1), controller.signal,
        );
        const items = [...page.items].reverse();
        const complete = items.length <= CHART_SECONDS;
        setHistory({
          ticks: complete ? items : items.slice(1),
          base: complete ? LISTING_PRICE : items[0].indexValue,
          fetchedAt: Date.now(),
        });
        setLoadError('');
      } catch (error) {
        if (!controller.signal.aborted) {
          setLoadError(error instanceof Error ? error.message : '경기 정보를 불러오지 못했어요.');
        }
      } finally {
        if (!controller.signal.aborted) timer = setTimeout(refresh, 1000);
      }
    }
    void refresh();
    return () => { controller.abort(); clearTimeout(timer); };
  }, [inviteCode]);

  const roomCode = room?.inviteCode;
  useEffect(() => {
    if (!roomCode) return;
    let stopped = false;
    let retry: ReturnType<typeof setTimeout>;
    let socket: WebSocket;
    const url = new URL('/api/ws', window.location.origin);
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
    const userId = sessionStorage.getItem('fandaq-user-id') ?? crypto.randomUUID();
    sessionStorage.setItem('fandaq-user-id', userId);
    url.searchParams.set('userId', userId);

    function connect() {
      socket = new WebSocket(url);
      tradeSocket.current = socket;
      socket.onopen = () => socket.send(JSON.stringify({ type: 'room.join', roomId: roomCode }));
      socket.onmessage = (event) => {
        if (stopped) return;
        const message = JSON.parse(event.data) as {
          type: string; roomId?: string; status?: Status; participantCount?: number;
          buy?: number; sell?: number; message?: string;
        };
        if (message.roomId && message.roomId !== roomCode) return;
        if (message.type === 'room.joined') setJoined(true);
        if (message.status) setStatus(message.status);
        if (message.participantCount !== undefined) setParticipants(message.participantCount);
        if (message.type === 'trade.ack') {
          setFeedback(message.buy || message.sell ? '응원이 접수됐어요.' : '잠시 후 다시 응원해 주세요.');
        }
        if (message.type === 'error') setFeedback(message.message ?? '요청을 처리하지 못했어요.');
      };
      socket.onclose = () => {
        if (stopped) return;
        setJoined(false);
        setParticipants(null);
        retry = setTimeout(connect, 2000);
      };
    }
    connect();
    return () => {
      stopped = true;
      clearTimeout(retry);
      tradeSocket.current = null;
      socket.close();
    };
  }, [roomCode]);

  function trade(side: 'buy' | 'sell') {
    const socket = tradeSocket.current;
    if (!joined || status !== 'LIVE' || socket?.readyState !== WebSocket.OPEN) return;
    socket.send(JSON.stringify({
      type: 'trade', roomId: roomCode, buy: Number(side === 'buy'), sell: Number(side === 'sell'),
    }));
  }

  const ticks = toChartTicks(history.ticks, history.base, history.fetchedAt, status === 'LIVE');
  const latest = ticks.at(-1);
  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <Link href={path}>‹ 경기방</Link>
        <strong>개경장</strong>
        <button onClick={async () => {
          try {
            await navigator.clipboard.writeText(location.href);
            setFeedback('링크를 복사했어요.');
          } catch {
            setFeedback('링크를 복사하지 못했어요.');
          }
        }}>공유</button>
      </header>
      <section className={[styles.card, styles.roomCard].join(" ")}>
        <h1>{room?.name ?? '팬심 차트'}</h1>
        <div className={styles.meta}>
          <span className={status === "LIVE" ? styles.liveBadge : undefined}>{status ? statusText[status] : loadError ? '조회 실패' : '경기 정보 조회 중'}</span>
          {participants !== null && <span>참여 {participants}명</span>}
        </div>
      </section>
      {loadError && <p className={styles.error} role="alert">{loadError}</p>}
      <section className={[styles.card, styles.chartCard].join(" ")}>
        <h2>팬심 지수</h2>
        {latest ? <>
          <p className={styles.value}>{latest.indexValue.toFixed(2)}</p>
          <p className={styles.meta}>최근 집계 매수 {latest.buyCount}, 매도 {latest.sellCount}</p>
          <Trend ticks={ticks} live={status === "LIVE"} />
        </> : <p>아직 표시할 시세가 없어요.</p>}
      </section>
      <section className={[styles.card, styles.tradeCard].join(" ")}>
        <h2>응원하기</h2>
        <div className={styles.actions}>
          <button disabled={!joined || status !== 'LIVE'} onClick={() => trade('buy')}>매수 응원</button>
          <button disabled={!joined || status !== 'LIVE'} onClick={() => trade('sell')}>매도 응원</button>
        </div>
        {status === 'LIVE' && !joined && <p>응원 서버에 연결 중이에요.</p>}
        <p role="status">{feedback}</p>
      </section>
    </main>
  );
}

// 거래가 없던 초는 직전 지수로 채워 최근 CHART_SECONDS초를 1초 간격으로 만든다.
// 경기 중이면 조회 시각까지, 아니면 마지막 틱까지 그린다.
function toChartTicks(stored: Tick[], base: number, fetchedAt: number, live: boolean): Tick[] {
  const timed = stored.map((tick) => ({
    time: Date.parse(tick.recordedAt), price: tick.indexValue, buy: tick.buyCount, sell: tick.sellCount,
  }));
  const lastTime = timed.at(-1)?.time;
  if (fetchedAt === 0 || (!live && lastTime === undefined)) return [];
  const end = live ? Math.max(fetchedAt, lastTime ?? fetchedAt) : (lastTime ?? fetchedAt);
  return recentSeconds(timed, end, CHART_SECONDS, base).map((point) => ({
    tickId: point.time,
    recordedAt: new Date(point.time).toISOString(),
    indexValue: point.price,
    buyCount: point.buy,
    sellCount: point.sell,
  }));
}
