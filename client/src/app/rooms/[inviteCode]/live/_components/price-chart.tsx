import { LISTING_PRICE, type Tick } from "../_lib/use-live-market";

type Props = {
  ticks: Tick[];
  lineColor: string;
  fillColor?: string;
  baselineColor: string;
  baselineLabel?: string;
  className?: string;
};

// x·y를 0~100 퍼센트 좌표로 만든다. SVG는 늘려 그리고, 글자와 점은 HTML로 얹어 찌그러지지 않게 한다.
function project(ticks: Tick[]) {
  const prices = ticks.map((t) => t.price);
  const min = Math.min(LISTING_PRICE, ...prices);
  const max = Math.max(LISTING_PRICE, ...prices);
  const pad = (max - min) * 0.12 || 1;
  const lo = min - pad;
  const hi = max + pad;
  const y = (price: number) => ((hi - price) / (hi - lo)) * 100;
  const x = (i: number) => (i / Math.max(ticks.length - 1, 1)) * 100;
  return { x, y };
}

export function PriceChart({ ticks, lineColor, fillColor, baselineColor, baselineLabel, className }: Props) {
  if (ticks.length === 0) {
    return (
      <div className={`grid place-items-center text-xs text-muted ${className ?? ""}`}>첫 체결을 기다리는 중이에요</div>
    );
  }
  const { x, y } = project(ticks);
  const points = ticks.map((t, i) => `${x(i)},${y(t.price)}`);
  const line = `M${points.join(" L")}`;
  const area = `${line} L100,100 L0,100 Z`;
  const baseY = y(LISTING_PRICE);
  const lastIndex = ticks.length - 1;

  return (
    <div className={`relative ${className ?? ""}`}>
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="absolute inset-0 h-full w-full overflow-visible">
        {fillColor && <path d={area} fill={fillColor} />}
        <line
          x1="0"
          x2="100"
          y1={baseY}
          y2={baseY}
          stroke={baselineColor}
          strokeWidth="1.5"
          strokeDasharray="4 4"
          vectorEffect="non-scaling-stroke"
        />
        <path
          d={line}
          fill="none"
          stroke={lineColor}
          strokeWidth="2.5"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>

      {baselineLabel && (
        <span
          className="absolute right-0 -translate-y-full pb-1 text-[11px] text-muted"
          style={{ top: `${baseY}%` }}
        >
          {baselineLabel}
        </span>
      )}

      <span
        className="absolute size-6 -translate-x-1/2 -translate-y-1/2 rounded-full opacity-25 transition-[top] duration-300"
        style={{ left: `${x(lastIndex)}%`, top: `${y(ticks[lastIndex].price)}%`, background: lineColor }}
      />
      <span
        className="absolute size-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-ink transition-[top] duration-300"
        style={{ left: `${x(lastIndex)}%`, top: `${y(ticks[lastIndex].price)}%`, background: lineColor }}
      />
    </div>
  );
}

export function VolumeBars({ ticks }: { ticks: Tick[] }) {
  const max = Math.max(...ticks.map((t) => t.buy + t.sell), 1);
  return (
    <div className="flex h-8 items-end gap-[3px]">
      {ticks.map((t) => (
        <span
          key={t.seq}
          className={`flex-1 rounded-t-[2px] ${
            t.buy + t.sell === 0 ? "bg-neutral-200" : t.buy >= t.sell ? "bg-buy" : "bg-sell"
          }`}
          style={{ height: `${Math.max(((t.buy + t.sell) / max) * 100, 8)}%` }}
        />
      ))}
    </div>
  );
}
