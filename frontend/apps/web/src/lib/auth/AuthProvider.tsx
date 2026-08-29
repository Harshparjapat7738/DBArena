"use client";

import { ApiError } from "@DBArena/api-client";
import { useEffect } from "react";
import { authApi } from "../api/clients";
import { useAuthStore } from "./authStore";

/**
 * Runs once per app load: tries a silent refresh against the HttpOnly
 * refresh cookie to re-establish a session without the user having to log
 * in again. A 401 here just means "not logged in" (or the cookie expired/
 * was never set) - not an error to surface, so it's swallowed into the
 * "anonymous" status rather than shown to the user.
 */
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const setSession = useAuthStore((s) => s.setSession);
  const clearSession = useAuthStore((s) => s.clearSession);

  useEffect(() => {
    let cancelled = false;
    authApi
      .refresh()
      .then((res) => {
        if (!cancelled) setSession(res.accessToken, res.user);
      })
      .catch((err) => {
        if (cancelled) return;
        // Any failure here just means "start the user out signed out" -
        // never worth surfacing to them, and never worth Next's dev
        // overlay treating it as a crash (it promotes console.error to a
        // full-screen error in dev). A 401 is the expected "no valid
        // session yet" case; anything else (gateway/identity-service not
        // reachable, a stray 404, a network error) is equally a startup
        // condition to log quietly, not an application error.
        if (err instanceof ApiError && err.status !== 401) {
          console.warn("session bootstrap: refresh call failed, starting signed out:", err.status, err.message);
        } else if (!(err instanceof ApiError)) {
          console.warn("session bootstrap: refresh call failed, starting signed out:", err);
        }
        clearSession();
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps -- runs exactly once, deliberately
  }, []);

  return <>{children}</>;
}
