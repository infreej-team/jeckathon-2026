import Link from 'next/link';

export default async function MatchPage({ params }: { params: Promise<{ matchId: string }> }) {
  const { matchId } = await params;

  return (
    <main className="mx-auto w-full max-w-md p-5">
      <h1 className="mb-4 text-xl font-bold">경기방</h1>
      <Link href={'/matches/' + encodeURIComponent(matchId) + '/chart'} className="underline">
        팬심 차트 열기
      </Link>
    </main>
  );
}
