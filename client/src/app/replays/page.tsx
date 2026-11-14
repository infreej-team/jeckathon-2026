import type { Metadata } from "next";

import { Replays } from "@/components/replays/Replays";

export const metadata: Metadata = {
  title: "명경기 순위 | 팬닥 FANDAQ",
};

// 로비 "명경기 순위" 링크와 결산 화면에서 오는 화면. 명경기 지수가 높은 순으로 끝난 방을 보여 준다.
export default function ReplaysPage() {
  return <Replays />;
}
