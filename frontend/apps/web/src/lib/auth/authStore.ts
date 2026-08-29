import type { AuthUser } from "@DBArena/api-client";
import { create } from "zustand";

export type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthState {
  status: AuthStatus;
  accessToken: string | null;
  user: AuthUser | null;
  setSession: (accessToken: string, user: AuthUser) => void;
  clearSession: () => void;
}

/**
 * The access token lives here and only here - a plain in-memory Zustand
 * store, never `localStorage` (frontend/CLAUDE.md's explicit rule). A hard
 * refresh loses it, which is exactly the point: {@link AuthProvider}
 * re-establishes a session on mount via the HttpOnly refresh cookie
 * instead, so nothing sensitive ever touches persistent browser storage.
 */
export const useAuthStore = create<AuthState>((set) => ({
  status: "loading",
  accessToken: null,
  user: null,
  setSession: (accessToken, user) => set({ status: "authenticated", accessToken, user }),
  clearSession: () => set({ status: "anonymous", accessToken: null, user: null }),
}));
