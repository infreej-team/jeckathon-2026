import Link from 'next/link';
import { Suspense } from 'react';

type Params = Promise<{ matchId: string }>;

// cacheComponents에서는 params를 Suspense 안에서 읽어야 빌드의 프리렌더가 멈추지 않는다.
export default function MatchPage({ params }: { params: Params }) {
  return (
    <main className="mx-auto w-full max-w-md p-5">
      <h1 className="mb-4 text-xl font-bold">경기방</h1>
      <Suspense>
        <ChartLink params={params} />
      </Suspense>
    </main>
  );
}

async function ChartLink({ params }: { params: Params }) {
  const { matchId } = await params;
  return (
    <Link href={'/matches/' + encodeURIComponent(matchId) + '/chart'} className="underline">
      팬심 차트 열기
    </Link>
  );
}
