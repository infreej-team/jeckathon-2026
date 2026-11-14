import { useSyncExternalStore } from "react";

// 로그인이 없으므로 닉네임을 브라우저(localStorage)에 둔다. 사용자 id는 userId.ts가 맡는다.

const NICKNAME_KEY = "fandaq:nickname";
// 서버 CommentCreateRequest의 nickname 길이 제한
export const MAX_NICKNAME_LENGTH = 20;

// 저장소를 못 쓰는 환경(사생활 보호 모드 등)에서는 이 탭 동안만 기억한다.
const memory = new Map<string, string>();

function read(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch {
    return memory.get(key) ?? null;
  }
}

function write(key: string, value: string) {
  try {
    localStorage.setItem(key, value);
  } catch {
    memory.set(key, value);
  }
}

const nicknameListeners = new Set<() => void>();

export function getNickname(): string | null {
  return read(NICKNAME_KEY);
}

export function saveNickname(nickname: string) {
  const value = nickname.trim().slice(0, MAX_NICKNAME_LENGTH);
  if (!value) return;
  write(NICKNAME_KEY, value);
  nicknameListeners.forEach((listener) => listener());
}

function subscribeNickname(listener: () => void) {
  nicknameListeners.add(listener);
  // 다른 탭에서 바꾼 닉네임도 따라간다.
  window.addEventListener("storage", listener);
  return () => {
    nicknameListeners.delete(listener);
    window.removeEventListener("storage", listener);
  };
}

// 서버 렌더링 중에는 브라우저 저장소를 모르므로 undefined, 저장된 닉네임이 없으면 null
export function useNickname(): string | null | undefined {
  return useSyncExternalStore(subscribeNickname, getNickname, () => undefined);
}
