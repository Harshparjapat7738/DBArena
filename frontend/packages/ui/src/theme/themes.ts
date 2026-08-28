/**
 * The DBForge theme catalog. Every theme is a flat set of CSS custom
 * property values applied to `document.documentElement` at runtime by
 * `ThemeProvider` - there is no per-theme static CSS file to keep in sync,
 * and no build step regenerates anything when a theme is added here; this
 * file is the single source of truth.
 *
 * Palette scope (frontend/CLAUDE.md doesn't define one; this is a
 * deliberate, documented decision - see backend/CLAUDE.md's own precedent
 * for "proceed with a reasonable, documented choice rather than block"):
 * VS Code's own built-in themes, plus a handful of the most recognizable
 * community themes almost every editor theme-picker offers. "All of VS
 * Code's marketplace themes" is not a bounded set (tens of thousands of
 * published themes) - this list is representative, not exhaustive, and
 * colors are close approximations of each theme's real palette, not
 * pixel-sourced from VS Code's own theme JSON.
 */
export type ThemeAppearance = "light" | "dark" | "high-contrast-dark" | "high-contrast-light";

export type ThemeCategory = "vscode-builtin" | "community";

export interface ThemeColors {
  bg: string;
  bgElevated: string;
  fg: string;
  fgMuted: string;
  border: string;
  accent: string;
  accentFg: string;
  success: string;
  warning: string;
  danger: string;
  info: string;
  editorBg: string;
  editorFg: string;
  selection: string;
}

export interface ThemeDefinition {
  id: string;
  name: string;
  category: ThemeCategory;
  appearance: ThemeAppearance;
  colors: ThemeColors;
}

export const THEMES: ThemeDefinition[] = [
  // ---- VS Code built-ins -------------------------------------------------
  {
    id: "dark-plus",
    name: "Dark+ (Default Dark Modern)",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#1e1e1e",
      bgElevated: "#252526",
      fg: "#d4d4d4",
      fgMuted: "#9d9d9d",
      border: "#3c3c3c",
      accent: "#0e639c",
      accentFg: "#ffffff",
      success: "#89d185",
      warning: "#cca700",
      danger: "#f48771",
      info: "#3794ff",
      editorBg: "#1e1e1e",
      editorFg: "#d4d4d4",
      selection: "#264f78",
    },
  },
  {
    id: "light-plus",
    name: "Light+ (Default Light Modern)",
    category: "vscode-builtin",
    appearance: "light",
    colors: {
      bg: "#ffffff",
      bgElevated: "#f3f3f3",
      fg: "#1e1e1e",
      fgMuted: "#6e6e6e",
      border: "#e5e5e5",
      accent: "#005fb8",
      accentFg: "#ffffff",
      success: "#388a34",
      warning: "#bf8803",
      danger: "#d83b01",
      info: "#005fb8",
      editorBg: "#ffffff",
      editorFg: "#1e1e1e",
      selection: "#add6ff",
    },
  },
  {
    id: "dark-high-contrast",
    name: "Dark High Contrast",
    category: "vscode-builtin",
    appearance: "high-contrast-dark",
    colors: {
      bg: "#000000",
      bgElevated: "#0a0a0a",
      fg: "#ffffff",
      fgMuted: "#d0d0d0",
      border: "#6fc3df",
      accent: "#3794ff",
      accentFg: "#000000",
      success: "#89d185",
      warning: "#f5f543",
      danger: "#f48771",
      info: "#75beff",
      editorBg: "#000000",
      editorFg: "#ffffff",
      selection: "#ffffff40",
    },
  },
  {
    id: "light-high-contrast",
    name: "Light High Contrast",
    category: "vscode-builtin",
    appearance: "high-contrast-light",
    colors: {
      bg: "#ffffff",
      bgElevated: "#f5f5f5",
      fg: "#000000",
      fgMuted: "#292929",
      border: "#0f4a85",
      accent: "#0f4a85",
      accentFg: "#ffffff",
      success: "#0b6125",
      warning: "#6c3c00",
      danger: "#a1260d",
      info: "#0f4a85",
      editorBg: "#ffffff",
      editorFg: "#000000",
      selection: "#0f4a8540",
    },
  },
  {
    id: "monokai",
    name: "Monokai",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#272822",
      bgElevated: "#2d2e27",
      fg: "#f8f8f2",
      fgMuted: "#94907e",
      border: "#3e3d32",
      accent: "#f92672",
      accentFg: "#ffffff",
      success: "#a6e22e",
      warning: "#e6db74",
      danger: "#f92672",
      info: "#66d9ef",
      editorBg: "#272822",
      editorFg: "#f8f8f2",
      selection: "#49483e",
    },
  },
  {
    id: "solarized-dark",
    name: "Solarized Dark",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#002b36",
      bgElevated: "#073642",
      fg: "#839496",
      fgMuted: "#586e75",
      border: "#073642",
      accent: "#268bd2",
      accentFg: "#ffffff",
      success: "#859900",
      warning: "#b58900",
      danger: "#dc322f",
      info: "#2aa198",
      editorBg: "#002b36",
      editorFg: "#839496",
      selection: "#073642",
    },
  },
  {
    id: "solarized-light",
    name: "Solarized Light",
    category: "vscode-builtin",
    appearance: "light",
    colors: {
      bg: "#fdf6e3",
      bgElevated: "#eee8d5",
      fg: "#657b83",
      fgMuted: "#93a1a1",
      border: "#eee8d5",
      accent: "#268bd2",
      accentFg: "#ffffff",
      success: "#859900",
      warning: "#b58900",
      danger: "#dc322f",
      info: "#2aa198",
      editorBg: "#fdf6e3",
      editorFg: "#657b83",
      selection: "#eee8d5",
    },
  },
  {
    id: "abyss",
    name: "Abyss",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#000c18",
      bgElevated: "#0a1a2f",
      fg: "#6688cc",
      fgMuted: "#384887",
      border: "#1d2c4d",
      accent: "#6688cc",
      accentFg: "#000c18",
      success: "#88ff88",
      warning: "#ffcc66",
      danger: "#ff5e5e",
      info: "#569cd6",
      editorBg: "#000c18",
      editorFg: "#6688cc",
      selection: "#103362",
    },
  },
  {
    id: "red",
    name: "Red",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#390000",
      bgElevated: "#4c0000",
      fg: "#f8f8f0",
      fgMuted: "#c98b8b",
      border: "#470000",
      accent: "#c30000",
      accentFg: "#ffffff",
      success: "#a6e22e",
      warning: "#e6db74",
      danger: "#ff0000",
      info: "#f8f8f0",
      editorBg: "#390000",
      editorFg: "#f8f8f0",
      selection: "#650000",
    },
  },
  {
    id: "kimbie-dark",
    name: "Kimbie Dark",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#221a0f",
      bgElevated: "#2b2313",
      fg: "#d3af86",
      fgMuted: "#7e5b38",
      border: "#34281a",
      accent: "#dc3958",
      accentFg: "#ffffff",
      success: "#889b4a",
      warning: "#f79a32",
      danger: "#dc3958",
      info: "#8ab3b5",
      editorBg: "#221a0f",
      editorFg: "#d3af86",
      selection: "#52413c",
    },
  },
  {
    id: "tomorrow-night-blue",
    name: "Tomorrow Night Blue",
    category: "vscode-builtin",
    appearance: "dark",
    colors: {
      bg: "#002451",
      bgElevated: "#00346e",
      fg: "#ffffff",
      fgMuted: "#7285b7",
      border: "#00346e",
      accent: "#6699ff",
      accentFg: "#002451",
      success: "#d1f1a9",
      warning: "#ffc58f",
      danger: "#ff9da4",
      info: "#99ffff",
      editorBg: "#002451",
      editorFg: "#ffffff",
      selection: "#003f8e",
    },
  },
  {
    id: "quiet-light",
    name: "Quiet Light",
    category: "vscode-builtin",
    appearance: "light",
    colors: {
      bg: "#f5f5f5",
      bgElevated: "#ffffff",
      fg: "#333333",
      fgMuted: "#aaaaaa",
      border: "#e0e0e0",
      accent: "#7a3e9d",
      accentFg: "#ffffff",
      success: "#448c27",
      warning: "#9c5518",
      danger: "#b6053b",
      info: "#7a3e9d",
      editorBg: "#f5f5f5",
      editorFg: "#333333",
      selection: "#c9d0d9",
    },
  },
  // ---- Iconic community themes -------------------------------------------
  {
    id: "dracula",
    name: "Dracula",
    category: "community",
    appearance: "dark",
    colors: {
      bg: "#282a36",
      bgElevated: "#343746",
      fg: "#f8f8f2",
      fgMuted: "#6272a4",
      border: "#44475a",
      accent: "#bd93f9",
      accentFg: "#191a21",
      success: "#50fa7b",
      warning: "#f1fa8c",
      danger: "#ff5555",
      info: "#8be9fd",
      editorBg: "#282a36",
      editorFg: "#f8f8f2",
      selection: "#44475a",
    },
  },
  {
    id: "one-dark-pro",
    name: "One Dark Pro",
    category: "community",
    appearance: "dark",
    colors: {
      bg: "#282c34",
      bgElevated: "#21252b",
      fg: "#abb2bf",
      fgMuted: "#5c6370",
      border: "#3e4451",
      accent: "#61afef",
      accentFg: "#0b0e14",
      success: "#98c379",
      warning: "#e5c07b",
      danger: "#e06c75",
      info: "#56b6c2",
      editorBg: "#282c34",
      editorFg: "#abb2bf",
      selection: "#3e4451",
    },
  },
  {
    id: "nord",
    name: "Nord",
    category: "community",
    appearance: "dark",
    colors: {
      bg: "#2e3440",
      bgElevated: "#3b4252",
      fg: "#d8dee9",
      fgMuted: "#4c566a",
      border: "#434c5e",
      accent: "#88c0d0",
      accentFg: "#2e3440",
      success: "#a3be8c",
      warning: "#ebcb8b",
      danger: "#bf616a",
      info: "#81a1c1",
      editorBg: "#2e3440",
      editorFg: "#d8dee9",
      selection: "#434c5e",
    },
  },
  {
    id: "github-dark",
    name: "GitHub Dark",
    category: "community",
    appearance: "dark",
    colors: {
      bg: "#0d1117",
      bgElevated: "#161b22",
      fg: "#c9d1d9",
      fgMuted: "#8b949e",
      border: "#30363d",
      accent: "#58a6ff",
      accentFg: "#0d1117",
      success: "#3fb950",
      warning: "#d29922",
      danger: "#f85149",
      info: "#79c0ff",
      editorBg: "#0d1117",
      editorFg: "#c9d1d9",
      selection: "#163356",
    },
  },
  {
    id: "github-light",
    name: "GitHub Light",
    category: "community",
    appearance: "light",
    colors: {
      bg: "#ffffff",
      bgElevated: "#f6f8fa",
      fg: "#24292f",
      fgMuted: "#57606a",
      border: "#d0d7de",
      accent: "#0969da",
      accentFg: "#ffffff",
      success: "#1a7f37",
      warning: "#9a6700",
      danger: "#cf222e",
      info: "#0969da",
      editorBg: "#ffffff",
      editorFg: "#24292f",
      selection: "#b6e3ff",
    },
  },
  {
    id: "night-owl",
    name: "Night Owl",
    category: "community",
    appearance: "dark",
    colors: {
      bg: "#011627",
      bgElevated: "#01111d",
      fg: "#d6deeb",
      fgMuted: "#637777",
      border: "#1d3b53",
      accent: "#82aaff",
      accentFg: "#011627",
      success: "#addb67",
      warning: "#ecc48d",
      danger: "#ef5350",
      info: "#7fdbca",
      editorBg: "#011627",
      editorFg: "#d6deeb",
      selection: "#1d3b53",
    },
  },
];

export const DEFAULT_DARK_THEME_ID = "dark-plus";
export const DEFAULT_LIGHT_THEME_ID = "light-plus";

export function findTheme(id: string): ThemeDefinition | undefined {
  return THEMES.find((t) => t.id === id);
}

export function themesByCategory(category: ThemeCategory): ThemeDefinition[] {
  return THEMES.filter((t) => t.category === category);
}
