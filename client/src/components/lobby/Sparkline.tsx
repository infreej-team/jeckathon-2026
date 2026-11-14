type Props = {
  series: number[];
  className?: string;
};

const WIDTH = 64;
const HEIGHT = 28;

export function Sparkline({ series, className }: Props) {
  if (series.length < 2) {
    return null;
  }
  const min = Math.min(...series);
  const max = Math.max(...series);
  const range = max - min || 1;
  const points = series
    .map((value, i) => {
      const x = (i / (series.length - 1)) * WIDTH;
      const y = HEIGHT - 2 - ((value - min) / range) * (HEIGHT - 4);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");

  return (
    <svg
      viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
      width={WIDTH}
      height={HEIGHT}
      className={className}
      fill="none"
      aria-hidden="true"
    >
      <polyline points={points} stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
