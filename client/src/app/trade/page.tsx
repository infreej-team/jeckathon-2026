import type { Metadata } from "next";
import { TradingScreen } from "./_components/trading-screen";

export const metadata: Metadata = {
  title: "트레이딩 | 팬닥 FANDAQ",
};

export default function TradePage() {
  return <TradingScreen />;
}
