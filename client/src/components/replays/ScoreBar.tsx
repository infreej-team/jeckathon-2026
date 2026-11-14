// 명경기 지수(0~100)를 가로 막대로 보여 준다.
export function ScoreBar({ score, className }: { score: number; className?: string }) {
  return (
    <div className={`h-3 overflow-hidden rounded-full border-2 border-ink ${className ?? ""}`}>
      <div className="h-full bg-ink" style={{ width: `${Math.min(Math.max(score, 0), 100)}%` }} />
    </div>
  );
}
