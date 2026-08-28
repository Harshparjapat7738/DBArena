"use client";

import { useEffect } from "react";
import { applyTheme, buildInlineThemeScript } from "./cssVars";
import { findTheme, DEFAULT_DARK_THEME_ID } from "./themes";
import { THEME_STORAGE_KEY, useThemeStore } from "./themeStore";

/**
 * Renders the blocking pre-hydration `<script>` that picks and applies a
 * theme before first paint. Place this inside `<head>` in the root layout,
 * as early as possible - it is intentionally NOT a client component and
 * has no dependency on React running at all, since its entire purpose is
 * to act before React does.
 */
export function ThemeScript() {
  // eslint-disable-next-line react/no-danger -- inline theme script must run synchronously pre-hydration, see cssVars.ts
  return <script dangerouslySetInnerHTML={{ __html: buildInlineThemeScript(THEME_STORAGE_KEY, DEFAULT_DARK_THEME_ID) }} />;
}

/**
 * Keeps `document.documentElement`'s CSS variables in sync with
 * `useThemeStore` after hydration, and persists the choice to
 * `localStorage` for the next visit's {@link ThemeScript} to pick up.
 * `localStorage` here is a plain UI preference, not the auth token
 * frontend/CLAUDE.md's "no localStorage for auth" rule is about.
 */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const themeId = useThemeStore((s) => s.themeId);

  useEffect(() => {
    const theme = findTheme(themeId);
    if (!theme) {
      return;
    }
    applyTheme(theme);
    try {
      localStorage.setItem(THEME_STORAGE_KEY, themeId);
    } catch {
      // Private browsing / storage disabled - the theme still applies for this tab, it just won't persist.
    }
  }, [themeId]);

  return <>{children}</>;
}
