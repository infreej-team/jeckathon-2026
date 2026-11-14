"use client";

import { useCallback, useState, type ReactNode } from "react";

type Burst = { id: number; left: number; top: number; rot: number };

let nextId = 0;

// 누를 때마다 떠올랐다 사라지는 스티커. 위치는 컨테이너 기준 퍼센트다.
export function useBursts() {
  const [bursts, setBursts] = useState<Burst[]>([]);

  const spawn = useCallback((left: number, top: number) => {
    const burst = { id: nextId++, left, top, rot: Math.round((Math.random() - 0.5) * 24) };
    setBursts((b) => [...b.slice(-8), burst]);
    setTimeout(() => setBursts((b) => b.filter((x) => x.id !== burst.id)), 700);
  }, []);

  return { bursts, spawn };
}

export function BurstLayer({ bursts, children }: { bursts: Burst[]; children: ReactNode }) {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0 z-10">
      {bursts.map((b) => (
        <span
          key={b.id}
          className="absolute animate-burst"
          style={{ left: `${b.left}%`, top: `${b.top}%`, ["--rot" as string]: `${b.rot}deg` }}
        >
          {children}
        </span>
      ))}
    </div>
  );
}
