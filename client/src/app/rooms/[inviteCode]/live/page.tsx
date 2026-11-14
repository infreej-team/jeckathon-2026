import type { Metadata } from "next";

import { TradingScreen } from "@/app/trade/_components/trading-screen";

export const metadata: Metadata = {
  title: "트레이딩 | 팬닥 FANDAQ",
};

// 응원 팀 고르기 화면에서 방에 입장하면 오는 트레이딩 화면.
export default function RoomLivePage() {
  return <TradingScreen />;
}
