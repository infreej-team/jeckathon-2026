"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getNickname } from "@/lib/nickname";
import { userIdHeaders } from "@/lib/userId";

// 서버 CommentCreateRequest의 content 길이 제한과 같다.
export const MAX_COMMENT_LENGTH = 200;
// 서버 CommentService.MAX_PAGE_SIZE
const PAGE_SIZE = 100;
// 코멘트는 웹소켓으로 오지 않으므로 since로 새 코멘트만 주기적으로 가져온다.
const POLL_MS = 3000;

export type Comment = {
  id: number;
  nickname: string;
  content: string;
  chartTime: string;
  likeCount: number;
  createdAt: string;
  likedByMe: boolean;
};

type Page = { items: Comment[]; totalCount: number };

// 서버 정렬(chartTime, id)과 같게 유지한다.
function merge(current: Comment[], incoming: Comment[]): Comment[] {
  const byId = new Map(current.map((c) => [c.id, c]));
  for (const c of incoming) byId.set(c.id, c);
  return [...byId.values()].sort((a, b) => a.chartTime.localeCompare(b.chartTime) || a.id - b.id);
}

const latestCreatedAt = (comments: Comment[]) =>
  comments.reduce<string | null>((max, c) => (max === null || c.createdAt > max ? c.createdAt : max), null);

export function useComments(inviteCode: string) {
  const [comments, setComments] = useState<Comment[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const sinceRef = useRef<string | null>(null);
  // 폴링과 작성 응답이 같은 코멘트를 줄 수 있어, 개수는 처음 보는 id만 센다.
  const seenRef = useRef(new Set<number>());
  const base = `/api/rooms/${encodeURIComponent(inviteCode)}/comments`;

  const get = useCallback(
    async (query: string, signal?: AbortSignal): Promise<Page> => {
      const res = await fetch(`${base}?${query}`, { headers: userIdHeaders(), signal });
      if (!res.ok) throw new Error(`코멘트 조회 실패: ${res.status}`);
      return res.json();
    },
    [base],
  );

  // 새로 본 코멘트 수를 돌려준다.
  const accept = useCallback((incoming: Comment[]) => {
    const fresh = incoming.filter((c) => !seenRef.current.has(c.id));
    fresh.forEach((c) => seenRef.current.add(c.id));
    const newest = latestCreatedAt(incoming);
    if (newest && (!sinceRef.current || newest > sinceRef.current)) sinceRef.current = newest;
    setComments((current) => merge(current, incoming));
    return fresh.length;
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    let timer: ReturnType<typeof setInterval> | undefined;

    const load = async () => {
      // 오래된 순으로 오므로 100개가 넘으면 마지막 페이지를 다시 받는다.
      let page = await get(`page=0&size=${PAGE_SIZE}`, controller.signal);
      if (page.totalCount > PAGE_SIZE) {
        page = await get(`page=${Math.floor((page.totalCount - 1) / PAGE_SIZE)}&size=${PAGE_SIZE}`, controller.signal);
      }
      setTotalCount(page.totalCount);
      accept(page.items);
    };

    const poll = async () => {
      if (!sinceRef.current) return load();
      const page = await get(`since=${encodeURIComponent(sinceRef.current)}&size=${PAGE_SIZE}`, controller.signal);
      const fresh = accept(page.items);
      if (fresh) setTotalCount((n) => n + fresh);
    };

    load()
      .catch(() => {})
      .finally(() => {
        if (!controller.signal.aborted) timer = setInterval(() => poll().catch(() => {}), POLL_MS);
      });
    return () => {
      controller.abort();
      clearInterval(timer);
    };
  }, [get, accept]);

  // 실패하면 서버가 준 사용자용 메시지를 돌려준다.
  const post = useCallback(
    async (content: string): Promise<string | null> => {
      const text = content.trim().slice(0, MAX_COMMENT_LENGTH);
      if (!text) return null;
      const res = await fetch(base, {
        method: "POST",
        headers: { "Content-Type": "application/json", ...userIdHeaders() },
        body: JSON.stringify({ nickname: getNickname() ?? "익명", content: text, chartTime: new Date().toISOString() }),
      });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        return body?.message ?? "한마디를 보내지 못했어요.";
      }
      const created: Comment = await res.json();
      if (accept([created])) setTotalCount((n) => n + 1);
      return null;
    },
    [base, accept],
  );

  return { comments, totalCount, post };
}
