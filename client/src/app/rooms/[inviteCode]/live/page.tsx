import type { Metadata } from "next";
import { Suspense } from "react";
import { LiveRoom } from "./_components/live-room";

export const metadata: Metadata = {
  title: "응원석 | 팬닥 FANDAQ",
};

// 응원 팀 고르기에서 "입장하기"를 누르면 오는 실시간 트레이딩 화면
export default function LiveRoomPage() {
  return (
    <Suspense>
      <LiveRoom />
    </Suspense>
  );
}
