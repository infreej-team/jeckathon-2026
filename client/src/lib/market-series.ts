// 서버는 거래가 있던 1초에만 틱을 저장한다. 거래가 없던 구간은 지수가 변하지 않으므로
// 차트는 직전 값으로 채운 일정 간격 시계열로 그린다.

export type TimedTick = { time: number; price: number; buy: number; sell: number };

/**
 * from~to를 count칸으로 나눈 시계열을 만든다. 각 칸은 그 시각의 지수와 직전 칸 이후의 거래량이다.
 * ticks는 시간 오름차순이어야 하고, basePrice는 ticks보다 앞선 시점의 지수다.
 */
export function toTimeSeries(ticks: TimedTick[], from: number, to: number, count: number, basePrice: number): TimedTick[] {
  const step = count > 1 ? (to - from) / (count - 1) : 0;
  const series: TimedTick[] = [];
  let price = basePrice;
  let j = 0;
  for (let i = 0; i < count; i++) {
    const time = count > 1 ? from + step * i : to;
    let buy = 0;
    let sell = 0;
    while (j < ticks.length && ticks[j].time <= time) {
      if (i > 0 || ticks[j].time > time - step) {
        buy += ticks[j].buy;
        sell += ticks[j].sell;
      }
      price = ticks[j].price;
      j++;
    }
    series.push({ time, price, buy, sell });
  }
  return series;
}

/** to에서 끝나는 최근 seconds초를 1초 간격으로 채운다. */
export function recentSeconds(ticks: TimedTick[], to: number, seconds: number, basePrice: number): TimedTick[] {
  return toTimeSeries(ticks, to - (seconds - 1) * 1000, to, seconds, basePrice);
}
