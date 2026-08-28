"use client";

import { ApiError } from "@dbforge/api-client";
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
        if (err instanceof ApiError && err.status !== 401) {
          console.error("session bootstrap failed:", err);
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
