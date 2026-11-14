// 방·경기 API를 붙이기 전까지 쓰는 화면용 고정 값이다.
export const MOCK_ROOM = {
  home: { name: "대한민국", flag: "🇰🇷" },
  away: { name: "포르투갈", flag: "🇵🇹" },
  score: [1, 1] as const,
  minute: 74,
  ticker: "KOR-2",
  participants: 9206,
  chatting: 1284,
};
