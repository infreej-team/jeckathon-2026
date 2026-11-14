"use client";

import { useState, type FormEvent } from "react";
import { formatCount } from "../_lib/format";
import { MOCK_ROOM } from "../_lib/mock-room";
import { MAX_COMMENT_LENGTH, useComments } from "../_lib/use-comments";

// 카드 높이를 고정하려고 최근 두 개만 보여 준다.
const VISIBLE = 2;

export function LiveComments() {
  const { comments, post } = useComments();
  const [draft, setDraft] = useState("");

  const submit = (e: FormEvent) => {
    e.preventDefault();
    post(draft);
    setDraft("");
  };

  return (
    <section className="mt-4 rounded-2xl border-2 border-ink bg-white p-3.5 shadow-[0_3px_0_var(--color-ink)]">
      <div className="flex items-center justify-between">
        <h2 className="flex items-center gap-1.5 text-sm font-black">
          <span className="size-2 rounded-full border-2 border-ink bg-fan" />
          실시간 한마디
        </h2>
        <span className="text-xs text-neutral-700">{formatCount(MOCK_ROOM.chatting)}명 수다 중</span>
      </div>

      <ul className="mt-2 space-y-1" aria-live="polite">
        {comments.slice(-VISIBLE).map((c) => (
          <li key={c.id} className="flex items-center gap-2 text-sm">
            <span
              className={`grid size-5 shrink-0 place-items-center rounded-full border-2 border-ink text-[10px] font-black ${
                c.id % 2 ? "bg-fan text-ink" : "bg-ink text-white"
              }`}
            >
              {c.nickname[0]}
            </span>
            <span className="shrink-0 font-black">{c.nickname}</span>
            <span className="min-w-0 flex-1 truncate">{c.content}</span>
            <span className="shrink-0 font-num text-xs text-muted">{c.minute}&apos;</span>
          </li>
        ))}
      </ul>

      <form onSubmit={submit} className="mt-2.5 flex gap-2">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          maxLength={MAX_COMMENT_LENGTH}
          placeholder="지금 이 순간, 한마디"
          aria-label="한마디 입력"
          className="h-10 min-w-0 flex-1 rounded-xl border-2 border-ink px-3 text-sm outline-none placeholder:text-neutral-400 focus:shadow-[2px_2px_0_var(--color-ink)]"
        />
        <button
          type="submit"
          disabled={!draft.trim()}
          aria-label="보내기"
          className="grid size-10 shrink-0 place-items-center rounded-xl border-2 border-ink bg-fan shadow-[2px_2px_0_var(--color-ink)] active:translate-y-0.5 active:shadow-none disabled:opacity-50"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M21 3L10 14M21 3l-7 18-4-7-7-4 18-7z" strokeLinejoin="round" />
          </svg>
        </button>
      </form>
    </section>
  );
}
