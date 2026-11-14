"use client";

import { useState } from "react";
import { useTradingSession } from "../_lib/use-trading-session";
import { BasicTrading } from "./basic-trading";
import { TapTrading } from "./tap-trading";

// 두 모드가 같은 세션을 공유해야 모드를 바꿔도 시세와 누적 주문이 이어진다.
export function TradingScreen() {
  const session = useTradingSession();
  const [mode, setMode] = useState<"basic" | "tap">("basic");

  return mode === "basic" ? (
    <BasicTrading session={session} onTapMode={() => setMode("tap")} />
  ) : (
    <TapTrading session={session} onBasicMode={() => setMode("basic")} />
  );
}
