// 임시값. 서버가 아직 주지 못하는 값을 방마다 고정된 가짜 값으로 만든다.
// 시세(지수·등락률·차트)와 스코어 API가 생기면 이 파일을 지우고 실제 값으로 바꾼다.

export type PlaceholderQuote = {
  index: number;
  changeRate: number;
  series: number[];
  score: [number, number];
};

const BASE_INDEX = 1000;
const POINTS = 40;

function hash(text: string): number {
  let h = 2166136261;
  for (let i = 0; i < text.length; i++) {
    h = Math.imul(h ^ text.charCodeAt(i), 16777619);
  }
  return h >>> 0;
}

// 같은 seed에서 항상 같은 수열을 내는 난수
function random(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state + 0x6d2b79f5) | 0;
    let t = Math.imul(state ^ (state >>> 15), 1 | state);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

export function placeholderQuote(inviteCode: string): PlaceholderQuote {
  const next = random(hash(inviteCode));
  const drift = (next() - 0.5) * 0.012;
  const series = [BASE_INDEX];
  for (let i = 1; i < POINTS; i++) {
    const step = drift + (next() - 0.5) * 0.03;
    series.push(Math.max(series[i - 1] * (1 + step), 1));
  }
  const index = series[POINTS - 1];
  return {
    index,
    changeRate: ((index - BASE_INDEX) / BASE_INDEX) * 100,
    series,
    score: [Math.floor(next() * 3), Math.floor(next() * 3)],
  };
}
