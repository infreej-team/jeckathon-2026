import type { Metadata } from "next";
import { Suspense } from "react";
import { RoomResult } from "./_components/room-result";

export const metadata: Metadata = {
  title: "장 마감 결산 | 팬닥 FANDAQ",
};

// 로비에서 끝난 방의 "리포트 보기"를 누르면 오는 결산 화면
export default function RoomResultPage() {
  return (
    <Suspense>
      <RoomResult />
    </Suspense>
  );
}
