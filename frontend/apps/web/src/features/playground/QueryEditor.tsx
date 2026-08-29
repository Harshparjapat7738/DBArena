"use client";

import { useMemo, useRef } from "react";

/**
 * A plain, line-numbered `<textarea>` editor - this workspace's stack has
 * no Monaco package yet (frontend/CLAUDE.md lists `packages/editor` as not
 * built), and adding one is a real dependency decision, not something to
 * pull in silently for a mock-execution pass. Styled to read like a real
 * code editor (monospace, editor-bg tokens, synced line-number gutter) and
 * wired for the one keyboard shortcut that matters most (Ctrl/Cmd+Enter to
 * run) - swap this for `@monaco-editor/react` later without touching
 * anything that calls it, since the props are already just value/onChange.
 */
export function QueryEditor({
  value,
  onChange,
  onRun,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  onRun?: () => void;
  placeholder?: string;
}) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const gutterRef = useRef<HTMLDivElement>(null);
  const lineCount = useMemo(() => Math.max(1, value.split("\n").length), [value]);

  function syncScroll() {
    if (gutterRef.current && textareaRef.current) {
      gutterRef.current.scrollTop = textareaRef.current.scrollTop;
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      onRun?.();
      return;
    }
    // Tab inserts two spaces instead of moving focus - expected editor behavior.
    if (e.key === "Tab") {
      e.preventDefault();
      const el = e.currentTarget;
      const start = el.selectionStart;
      const end = el.selectionEnd;
      const next = value.slice(0, start) + "  " + value.slice(end);
      onChange(next);
      requestAnimationFrame(() => el.setSelectionRange(start + 2, start + 2));
    }
  }

  return (
    <div className="flex h-full overflow-hidden bg-editor-bg font-mono text-sm">
      <div
        ref={gutterRef}
        aria-hidden
        className="select-none overflow-hidden px-3 py-3 text-right text-editor-fg/40"
        style={{ lineHeight: "1.5rem" }}
      >
        {Array.from({ length: lineCount }).map((_, i) => (
          <div key={i}>{i + 1}</div>
        ))}
      </div>
      <textarea
        ref={textareaRef}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onScroll={syncScroll}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        spellCheck={false}
        className="h-full flex-1 resize-none bg-transparent py-3 pr-3 text-editor-fg outline-none placeholder:text-editor-fg/40"
        style={{ lineHeight: "1.5rem" }}
        aria-label="Query editor"
      />
    </div>
  );
}
