import Image from 'next/image';
import Link from 'next/link';

export default async function ChartPreviewPage({ params }: { params: Promise<{ matchId: string }> }) {
  const { matchId } = await params;

  return (
    <main className="mx-auto w-full max-w-[876px] p-4">
      <Link href={'/matches/' + encodeURIComponent(matchId) + '/chart'} className="mb-3 inline-block underline">
        팬심 차트로 돌아가기
      </Link>
      <Image src="/fan-chart-landscape.svg" alt="팬심 차트 가로형 시안" width={844} height={390}
        className="h-auto w-full" unoptimized />
    </main>
  );
}
