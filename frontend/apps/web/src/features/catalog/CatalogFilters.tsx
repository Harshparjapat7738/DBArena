"use client";

import type { Difficulty, EngineKind, TagCount } from "@dbforge/api-client";
import { Search } from "lucide-react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";

const DIFFICULTIES: Difficulty[] = ["EASY", "MEDIUM", "HARD"];
const ENGINES: EngineKind[] = ["POSTGRES", "MYSQL", "MONGODB"];
const ENGINE_LABEL: Record<EngineKind, string> = {
  POSTGRES: "PostgreSQL",
  MYSQL: "MySQL",
  MONGODB: "MongoDB",
};

/**
 * Client island for the catalog's search/difficulty/engine/tag controls.
 * Every change is pushed into the URL's search params, which drives a
 * fresh Server Component render of `app/(app)/catalog/page.tsx` - the
 * catalogue itself stays server-rendered per frontend/CLAUDE.md, this is
 * the one bit of interactivity that has to run in the browser.
 */
export function CatalogFilters({ tags }: { tags: TagCount[] }) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const [searchText, setSearchText] = useState(searchParams.get("q") ?? "");
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const difficulty = searchParams.get("difficulty") ?? "";
  const engine = searchParams.get("engine") ?? "";
  const tag = searchParams.get("tag") ?? "";

  function pushParams(next: Record<string, string | null>) {
    const params = new URLSearchParams(searchParams.toString());
    for (const [key, value] of Object.entries(next)) {
      if (value) params.set(key, value);
      else params.delete(key);
    }
    params.delete("cursor"); // any filter change resets pagination
    router.push(`${pathname}?${params.toString()}`);
  }

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      if (searchText !== (searchParams.get("q") ?? "")) {
        pushParams({ q: searchText || null });
      }
    }, 350);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchText]);

  return (
    <div className="flex flex-col gap-3 rounded-lg border border-border bg-bg-elevated p-3">
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-fg-muted" aria-hidden />
        <input
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          placeholder="Search problems…"
          className="w-full rounded-md border border-border bg-bg py-2 pl-9 pr-3 text-sm text-fg placeholder:text-fg-muted focus:border-accent focus:outline-none"
        />
      </div>

      <div className="flex flex-wrap gap-2">
        <select
          value={difficulty}
          onChange={(e) => pushParams({ difficulty: e.target.value || null })}
          className="rounded-md border border-border bg-bg px-2.5 py-1.5 text-sm text-fg focus:border-accent focus:outline-none"
        >
          <option value="">All difficulties</option>
          {DIFFICULTIES.map((d) => (
            <option key={d} value={d}>
              {d.charAt(0) + d.slice(1).toLowerCase()}
            </option>
          ))}
        </select>

        <select
          value={engine}
          onChange={(e) => pushParams({ engine: e.target.value || null })}
          className="rounded-md border border-border bg-bg px-2.5 py-1.5 text-sm text-fg focus:border-accent focus:outline-none"
        >
          <option value="">All engines</option>
          {ENGINES.map((e) => (
            <option key={e} value={e}>
              {ENGINE_LABEL[e]}
            </option>
          ))}
        </select>

        <select
          value={tag}
          onChange={(e) => pushParams({ tag: e.target.value || null })}
          className="rounded-md border border-border bg-bg px-2.5 py-1.5 text-sm text-fg focus:border-accent focus:outline-none"
        >
          <option value="">All tags</option>
          {tags.map((t) => (
            <option key={t.tag} value={t.tag}>
              #{t.tag} ({t.count})
            </option>
          ))}
        </select>

        {(difficulty || engine || tag || searchText) && (
          <button
            type="button"
            onClick={() => {
              setSearchText("");
              router.push(pathname);
            }}
            className="ml-auto rounded-md px-2.5 py-1.5 text-sm font-medium text-fg-muted hover:text-fg"
          >
            Clear filters
          </button>
        )}
      </div>
    </div>
  );
}
