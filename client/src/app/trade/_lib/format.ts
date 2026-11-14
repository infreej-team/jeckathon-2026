import { LISTING_PRICE } from "./use-trading-session";

const priceFormat = new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const countFormat = new Intl.NumberFormat("ko-KR");

export const formatPrice = (n: number) => priceFormat.format(n);
export const formatCount = (n: number) => countFormat.format(n);

// 예: ▼ + "62.96 (-6.30%)". 화살표는 고정폭 글꼴에 없어 따로 그린다.
export function formatChange(price: number) {
  const diff = price - LISTING_PRICE;
  const rate = (diff / LISTING_PRICE) * 100;
  const sign = diff >= 0 ? "+" : "-";
  return { arrow: diff >= 0 ? "▲" : "▼", text: `${formatPrice(Math.abs(diff))} (${sign}${Math.abs(rate).toFixed(2)}%)` };
}

export const isUp = (price: number) => price >= LISTING_PRICE;
