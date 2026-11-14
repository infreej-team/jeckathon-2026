import { Suspense } from "react";

import { TeamSelect } from "@/components/team-select/TeamSelect";

// 로비에서 "응원석 들어가기"를 누르면 오는 화면. 응원 팀과 방을 고른다.
export default function RoomEntryPage() {
  return (
    <Suspense>
      <TeamSelect />
    </Suspense>
  );
}
