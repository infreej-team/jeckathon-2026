import { Suspense } from 'react';
import MatchChart from './match-chart';

type Params = Promise<{ matchId: string }>;

// cacheComponents에서는 params를 Suspense 안에서 읽어야 빌드의 프리렌더가 멈추지 않는다.
export default function ChartPage({ params }: { params: Params }) {
  return (
    <Suspense>
      <Chart params={params} />
    </Suspense>
  );
}

async function Chart({ params }: { params: Params }) {
  const { matchId } = await params;
  return <MatchChart key={matchId} inviteCode={matchId} />;
}
