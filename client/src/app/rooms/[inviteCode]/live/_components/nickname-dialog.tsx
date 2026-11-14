"use client";

import { useState, type FormEvent } from "react";
import { MAX_NICKNAME_LENGTH, saveNickname } from "@/lib/nickname";

// 처음 방에 들어왔는데 이 브라우저에 닉네임이 없을 때 띄운다. 정하기 전에는 닫을 수 없다.
export function NicknameDialog() {
  const [draft, setDraft] = useState("");

  const submit = (e: FormEvent) => {
    e.preventDefault();
    saveNickname(draft);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center">
      <form
        role="dialog"
        aria-modal="true"
        aria-labelledby="nickname-title"
        onSubmit={submit}
        className="w-full max-w-[430px] rounded-t-3xl border-2 border-ink bg-white px-5 pb-8 pt-6 sm:rounded-3xl sm:pb-6"
      >
        <h2 id="nickname-title" className="text-lg font-black">
          응원석에서 쓸 닉네임을 정해 주세요
        </h2>
        <p className="mt-1 text-sm text-muted">한마디를 남길 때 보여요. 이 브라우저에 저장돼요.</p>
        <input
          autoFocus
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          maxLength={MAX_NICKNAME_LENGTH}
          placeholder="예: 존버개미"
          aria-label="닉네임"
          className="mt-4 h-12 w-full rounded-xl border-2 border-ink px-3 text-base outline-none placeholder:text-neutral-400 focus:shadow-[2px_2px_0_var(--color-ink)]"
        />
        <p className="mt-1.5 text-right font-num text-xs text-muted">
          {draft.trim().length}/{MAX_NICKNAME_LENGTH}
        </p>
        <button
          type="submit"
          disabled={!draft.trim()}
          className="mt-3 h-12 w-full rounded-xl border-2 border-ink bg-fan text-base font-black shadow-[0_3px_0_var(--color-ink)] active:translate-y-0.5 active:shadow-none disabled:opacity-50"
        >
          입장하기
        </button>
      </form>
    </div>
  );
}
