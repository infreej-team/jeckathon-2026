"use client";

import { useCallback, useState } from "react";
import { MOCK_ROOM } from "./mock-room";

// 서버 코멘트 API(POST·GET /api/rooms/{inviteCode}/comments)를 붙이기 전까지 쓰는 로컬 목록이다.
// 화면은 이 훅이 돌려주는 값만 읽으므로, API를 붙일 때는 이 파일만 바꾼다.

// 서버 CommentCreateRequest의 content 길이 제한과 같다.
export const MAX_COMMENT_LENGTH = 200;
const MY_NICKNAME = "나";

export type LiveComment = { id: number; nickname: string; content: string; minute: number };

const SEED: LiveComment[] = [
  { id: 1, nickname: "민지", content: "동점골 각 나온다, 존버 간다", minute: 73 },
  { id: 2, nickname: "재훈", content: "제발 한 골만… 손흥민 가자", minute: 74 },
];

let nextId = SEED.length + 1;

export function useComments() {
  const [comments, setComments] = useState(SEED);

  const post = useCallback((content: string) => {
    const text = content.trim().slice(0, MAX_COMMENT_LENGTH);
    if (!text) return;
    setComments((c) => [...c, { id: nextId++, nickname: MY_NICKNAME, content: text, minute: MOCK_ROOM.minute }]);
  }, []);

  return { comments, post };
}
