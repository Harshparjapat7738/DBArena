"use client";

import { Check, Monitor, Moon, Sun } from "lucide-react";
import { useMemo } from "react";
import { cn } from "../lib/cn";
import {
  DEFAULT_DARK_THEME_ID,
  DEFAULT_LIGHT_THEME_ID,
  THEMES,
  type ThemeDefinition,
  themesByCategory,
} from "./themes";
import { useThemeStore } from "./themeStore";

function prefersDark(): boolean {
  return typeof window !== "undefined" && window.matchMedia?.("(prefers-color-scheme: dark)").matches;
}

/**
 * The settings-page appearance picker: a three-way quick toggle
 * (System/Light/Dark) above a full swatch gallery of every theme in the
 * catalog, grouped by where it comes from. Each swatch is a tiny rendered
 * mockup of that theme's own colors (not a generic preview) - the point is
 * to let someone recognize "oh, that's Dracula" at a glance the way VS
 * Code's own theme picker does, rather than picking from a plain name list.
 */
export function ThemeSwitcher() {
  const themeId = useThemeStore((s) => s.themeId);
  const setThemeId = useThemeStore((s) => s.setThemeId);

  const quickMode = useMemo<"system" | "light" | "dark" | null>(() => {
    if (themeId === DEFAULT_LIGHT_THEME_ID) return "light";
    if (themeId === DEFAULT_DARK_THEME_ID) return "dark";
    return null;
  }, [themeId]);

  function chooseQuick(mode: "system" | "light" | "dark") {
    if (mode === "system") {
      setThemeId(prefersDark() ? DEFAULT_DARK_THEME_ID : DEFAULT_LIGHT_THEME_ID);
      return;
    }
    setThemeId(mode === "light" ? DEFAULT_LIGHT_THEME_ID : DEFAULT_DARK_THEME_ID);
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="inline-flex w-fit rounded-lg border border-border bg-bg p-1">
        <QuickModeButton icon={Monitor} label="System" active={quickMode === null} onClick={() => chooseQuick("system")} />
        <QuickModeButton icon={Sun} label="Light" active={quickMode === "light"} onClick={() => chooseQuick("light")} />
        <QuickModeButton icon={Moon} label="Dark" active={quickMode === "dark"} onClick={() => chooseQuick("dark")} />
      </div>

      <ThemeGroup title="VS Code built-in" themes={themesByCategory("vscode-builtin")} selectedId={themeId} onSelect={setThemeId} />
      <ThemeGroup title="Community favorites" themes={themesByCategory("community")} selectedId={themeId} onSelect={setThemeId} />
    </div>
  );
}

function QuickModeButton({
  icon: Icon,
  label,
  active,
  onClick,
}: {
  icon: typeof Sun;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
        active ? "bg-accent text-accent-fg" : "text-fg-muted hover:text-fg",
      )}
      aria-pressed={active}
    >
      <Icon className="h-4 w-4" aria-hidden />
      {label}
    </button>
  );
}

function ThemeGroup({
  title,
  themes,
  selectedId,
  onSelect,
}: {
  title: string;
  themes: ThemeDefinition[];
  selectedId: string;
  onSelect: (id: string) => void;
}) {
  return (
    <div>
      <h4 className="mb-3 text-sm font-semibold text-fg-muted">{title}</h4>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {themes.map((theme) => (
          <ThemeSwatch key={theme.id} theme={theme} selected={theme.id === selectedId} onSelect={() => onSelect(theme.id)} />
        ))}
      </div>
    </div>
  );
}

function ThemeSwatch({ theme, selected, onSelect }: { theme: ThemeDefinition; selected: boolean; onSelect: () => void }) {
  const { colors } = theme;
  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={selected}
      className={cn(
        "group relative flex flex-col overflow-hidden rounded-lg border-2 text-left transition-transform hover:-translate-y-0.5",
        selected ? "border-accent" : "border-border",
      )}
      title={theme.name}
    >
      <div className="h-16 w-full p-2" style={{ backgroundColor: colors.bg }}>
        <div className="mb-1.5 h-2 w-full rounded-sm" style={{ backgroundColor: colors.bgElevated }} />
        <div className="mb-1 h-1.5 w-3/4 rounded-sm" style={{ backgroundColor: colors.fg }} />
        <div className="mb-1 h-1.5 w-1/2 rounded-sm" style={{ backgroundColor: colors.fgMuted }} />
        <div className="flex gap-1">
          <span className="h-2 w-2 rounded-full" style={{ backgroundColor: colors.accent }} />
          <span className="h-2 w-2 rounded-full" style={{ backgroundColor: colors.success }} />
          <span className="h-2 w-2 rounded-full" style={{ backgroundColor: colors.danger }} />
        </div>
      </div>
      <div className="flex items-center justify-between gap-1 border-t border-border bg-bg-elevated px-2 py-1.5">
        <span className="truncate text-xs font-medium text-fg">{theme.name}</span>
        {selected && <Check className="h-3.5 w-3.5 shrink-0 text-accent" aria-hidden />}
      </div>
    </button>
  );
}

export function themeCount(): number {
  return THEMES.length;
}
