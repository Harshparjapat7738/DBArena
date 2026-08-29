/**
 * Thin, SSR-safe localStorage JSON helper. Every mock repository that
 * needs to remember something across a session (XP, bookmarks, submission
 * history, lesson progress...) goes through this rather than touching
 * `window.localStorage` directly - one place to reason about "what does
 * this app persist client-side and why" (root CLAUDE.md's ban is on
 * *auth tokens* in localStorage specifically; this is ordinary,
 * non-sensitive UI/progress state, the same category Settings' theme
 * picker already uses localStorage for).
 */

const PREFIX = "dbarena.mock.";

export function readJson<T>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(PREFIX + key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export function writeJson<T>(key: string, value: T): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(PREFIX + key, JSON.stringify(value));
  } catch {
    // Private browsing / storage full / disabled - fail silently, this is
    // convenience state only, never load-bearing.
  }
}
