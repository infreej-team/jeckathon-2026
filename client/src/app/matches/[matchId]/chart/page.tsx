import MatchChart from './match-chart';

export default async function ChartPage({ params }: { params: Promise<{ matchId: string }> }) {
  const { matchId } = await params;
  return <MatchChart key={matchId} inviteCode={matchId} />;
}
