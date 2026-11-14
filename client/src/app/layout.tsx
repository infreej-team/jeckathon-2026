import type { Metadata } from "next";
import { Black_Han_Sans, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

// 제목용 한글 글꼴
const displayKo = Black_Han_Sans({
  variable: "--font-display-ko",
  weight: "400",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "개경장 — 개미들의 경기장",
  description: "경기를 보며 매수·매도로 팬심을 표현하는 팬심 주식 차트",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${geistMono.variable} ${displayKo.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
