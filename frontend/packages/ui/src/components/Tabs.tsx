"use client";

import { useId } from "react";
import { cn } from "../lib/cn";

export interface TabItem {
  value: string;
  label: React.ReactNode;
  icon?: React.ComponentType<{ className?: string }>;
  badge?: React.ReactNode;
}

export interface TabsProps {
  items: TabItem[];
  value: string;
  onChange: (value: string) => void;
  className?: string;
}

/**
 * Lightweight controlled tab list - no external dependency, full keyboard
 * support (arrow keys move + activate, matching the WAI-ARIA "automatic
 * activation" tabs pattern). Renders only the tablist; the caller renders
 * whichever panel matches `value` (keeps this reusable for both "swap
 * content" tabs like Result/Messages/Explain and "navigate" tabs like
 * Bookmarks/History).
 */
export function Tabs({ items, value, onChange, className }: TabsProps) {
  const name = useId();

  function handleKeyDown(e: React.KeyboardEvent, index: number) {
    if (e.key !== "ArrowRight" && e.key !== "ArrowLeft") return;
    e.preventDefault();
    const dir = e.key === "ArrowRight" ? 1 : -1;
    const next = items[(index + dir + items.length) % items.length];
    if (next) {
      onChange(next.value);
      const el = document.getElementById(`${name}-${next.value}`);
      el?.focus();
    }
  }

  return (
    <div role="tablist" className={cn("flex items-center gap-1 border-b border-border", className)}>
      {items.map((item, i) => {
        const active = item.value === value;
        return (
          <button
            key={item.value}
            id={`${name}-${item.value}`}
            role="tab"
            type="button"
            aria-selected={active}
            tabIndex={active ? 0 : -1}
            onClick={() => onChange(item.value)}
            onKeyDown={(e) => handleKeyDown(e, i)}
            className={cn(
              "flex items-center gap-1.5 border-b-2 px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-accent",
              active ? "border-accent text-fg" : "border-transparent text-fg-muted hover:text-fg",
            )}
          >
            {item.icon && <item.icon className="h-3.5 w-3.5" />}
            {item.label}
            {item.badge !== undefined && <span className="ml-0.5">{item.badge}</span>}
          </button>
        );
      })}
    </div>
  );
}
