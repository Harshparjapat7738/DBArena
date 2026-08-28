import { create } from "zustand";
import { DEFAULT_DARK_THEME_ID, findTheme } from "./themes";

export const THEME_STORAGE_KEY = "dbforge.theme";

interface ThemeState {
  themeId: string;
  setThemeId: (id: string) => void;
}

/**
 * Client-only Zustand store (frontend/CLAUDE.md: "Client state: Zustand").
 * Initial value is read from the `data-theme` attribute the pre-hydration
 * inline script (`buildInlineThemeScript`) already stamped onto `<html>`
 * before React ever ran - so the store's first render always agrees with
 * what's already on screen, never causing a hydration flash of its own.
 * On the server (no `document`) this falls back to the dark default; it's
 * immediately corrected client-side, and nothing server-rendered depends
 * on the exact theme id.
 */
export const useThemeStore = create<ThemeState>((set) => ({
  themeId: typeof document !== "undefined" ? document.documentElement.dataset.theme || DEFAULT_DARK_THEME_ID : DEFAULT_DARK_THEME_ID,
  setThemeId: (id: string) => {
    if (!findTheme(id)) {
      return;
    }
    set({ themeId: id });
  },
}));
