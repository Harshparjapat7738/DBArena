import { THEMES, type ThemeColors, type ThemeDefinition } from "./themes";

/**
 * The CSS custom property each {@link ThemeColors} key maps to.
 * `tailwind-theme.css` (Tailwind's `@theme` block) points its `--color-*`
 * tokens at these same names, so every Tailwind utility (`bg-bg`,
 * `text-fg`, `border-border`, ...) re-resolves automatically the instant
 * these variables change - no class names ever change when the theme
 * does.
 */
export const CSS_VAR_NAMES: Record<keyof ThemeColors, string> = {
  bg: "--db-bg",
  bgElevated: "--db-bg-elevated",
  fg: "--db-fg",
  fgMuted: "--db-fg-muted",
  border: "--db-border",
  accent: "--db-accent",
  accentFg: "--db-accent-fg",
  success: "--db-success",
  warning: "--db-warning",
  danger: "--db-danger",
  info: "--db-info",
  editorBg: "--db-editor-bg",
  editorFg: "--db-editor-fg",
  selection: "--db-selection",
};

/** Applies every color in `theme` as an inline custom property on `element` (defaults to `<html>`). */
export function applyTheme(theme: ThemeDefinition, element: HTMLElement = document.documentElement): void {
  for (const key of Object.keys(CSS_VAR_NAMES) as (keyof ThemeColors)[]) {
    element.style.setProperty(CSS_VAR_NAMES[key], theme.colors[key]);
  }
  element.setAttribute("data-theme", theme.id);
  element.setAttribute("data-theme-appearance", theme.appearance);
  element.style.colorScheme =
    theme.appearance === "light" || theme.appearance === "high-contrast-light" ? "light" : "dark";
}

/**
 * Everything the pre-hydration blocking script needs, and nothing else -
 * shipped inline in `<head>` (see {@link buildInlineThemeScript}) so it has
 * to be small: just id + appearance + colors, not the display `name` or
 * `category` a settings page needs later.
 */
type MinimalThemeMap = Record<string, { a: ThemeDefinition["appearance"]; c: ThemeColors }>;

function toMinimalThemeMap(): MinimalThemeMap {
  const map: MinimalThemeMap = {};
  for (const theme of THEMES) {
    map[theme.id] = { a: theme.appearance, c: theme.colors };
  }
  return map;
}

/**
 * Source for the blocking inline `<script>` `ThemeProvider` places in
 * `<head>`, before any React hydration - this is what prevents a flash of
 * the wrong theme (or wrong light/dark) on first paint. Deliberately
 * framework-free plain JS as a string: it must execute synchronously
 * before the app's JS bundle loads, so it cannot import anything.
 */
export function buildInlineThemeScript(storageKey: string, defaultThemeId: string): string {
  const themeMap = JSON.stringify(toMinimalThemeMap());
  const varNames = JSON.stringify(CSS_VAR_NAMES);
  return `(function(){try{
    var key=${JSON.stringify(storageKey)};
    var id=localStorage.getItem(key)||${JSON.stringify(defaultThemeId)};
    var themes=${themeMap};
    var vars=${varNames};
    var theme=themes[id]||themes[${JSON.stringify(defaultThemeId)}];
    var root=document.documentElement;
    for(var k in vars){root.style.setProperty(vars[k],theme.c[k]);}
    root.setAttribute('data-theme',id);
    root.setAttribute('data-theme-appearance',theme.a);
    root.style.colorScheme=(theme.a==='light'||theme.a==='high-contrast-light')?'light':'dark';
  }catch(e){}})();`;
}
