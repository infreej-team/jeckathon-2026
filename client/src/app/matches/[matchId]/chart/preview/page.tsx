import Image from 'next/image';
import Link from 'next/link';
import { Suspense } from 'react';

type Params = Promise<{ matchId: string }>;

// cacheComponents에서는 params를 Suspense 안에서 읽어야 빌드의 프리렌더가 멈추지 않는다.
export default function ChartPreviewPage({ params }: { params: Params }) {
  return (
    <main className="mx-auto w-full max-w-[876px] p-4">
      <Suspense>
        <BackLink params={params} />
      </Suspense>
      <Image src="/fan-chart-landscape.svg" alt="팬심 차트 가로형 시안" width={844} height={390}
        className="h-auto w-full" unoptimized />
    </main>
  );
}

async function BackLink({ params }: { params: Params }) {
  const { matchId } = await params;
  return (
    <Link href={'/matches/' + encodeURIComponent(matchId) + '/chart'} className="mb-3 inline-block underline">
      팬심 차트로 돌아가기
    </Link>
  );
}
