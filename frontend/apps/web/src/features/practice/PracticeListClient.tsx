"use client";

import type { ProblemListQuery, ProblemSummary } from "@DBArena/api-client";
import { hasMorePages } from "@DBArena/api-client";
import { Button, EmptyState, Skeleton } from "@DBArena/ui";
import { ListChecks } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { catalogApi } from "@/lib/api/clients";
import { bookmarksRepository, problemsRepository } from "@/lib/mock/repositories";
import type { ProblemMeta, ProblemStatus } from "@/lib/mock/types";
import { ProblemCard } from "@/features/catalog/ProblemCard";
import { DEFAULT_REFINEMENTS, PracticeRefinements, type RefinementState } from "./PracticeRefinements";

const DIFFICULTY_RANK = { EASY: 0, MEDIUM: 1, HARD: 2 } as const;

/**
 * Practice's list: real catalog-service problems (server-paginated, same
 * `catalogApi.listProblems` the old Catalog page used) unioned with the
 * mock fixture catalog (same q/difficulty/engine/tag filter applied
 * client-side via `problemsRepository`, so both respect the same URL-
 * driven filters `PracticeFilters`/the old `CatalogFilters` set) - then
 * refined further by topic/dataset/status/sort/bookmarked, which have no
 * real backend equivalent yet. "Load more" only paginates the real
 * portion; the mock portion is small and fully loaded up front.
 */
export function PracticeListClient({
  initialItems,
  initialNextCursor,
  query,
}: {
  initialItems: ProblemSummary[];
  initialNextCursor: string | null;
  query: ProblemListQuery;
}) {
  const [realItems, setRealItems] = useState(initialItems);
  const [nextCursor, setNextCursor] = useState(initialNextCursor);
  const [mockItems, setMockItems] = useState<ProblemSummary[]>([]);
  const [metaMap, setMetaMap] = useState<Record<string, ProblemMeta>>({});
  const [statusMap, setStatusMap] = useState<Record<string, ProblemStatus>>({});
  const [bookmarked, setBookmarked] = useState<Set<string>>(new Set());
  const [refinements, setRefinements] = useState<RefinementState>(DEFAULT_REFINEMENTS);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // No effect syncing `initialItems`/`initialNextCursor` props into state:
  // the parent page passes a `key` derived from the query string, so this
  // whole component remounts (fresh `useState(initialItems)`) whenever the
  // server-driven filters change, rather than an effect copying props into
  // state on every render (react-hooks/set-state-in-effect flags that).

  useEffect(() => {
    problemsRepository
      .listMockProblems({ q: query.q, difficulty: query.difficulty, engine: query.engine, tag: query.tag })
      .then(setMockItems);
  }, [query.q, query.difficulty, query.engine, query.tag]);

  useEffect(() => {
    Promise.all([problemsRepository.getAllMeta(), problemsRepository.getAllStatuses(), bookmarksRepository.list()]).then(
      ([meta, statuses, bookmarks]) => {
        setMetaMap(meta);
        setStatusMap(statuses);
        setBookmarked(new Set(bookmarks.map((b) => b.problemSlug)));
      },
    );
  }, []);

  async function loadMore() {
    if (!nextCursor || loading) return;
    setLoading(true);
    setError(null);
    try {
      const page = await catalogApi.listProblems({ ...query, cursor: nextCursor });
      setRealItems((prev) => [...prev, ...page.items]);
      setNextCursor(hasMorePages(page) ? page.nextCursor : null);
    } catch {
      setError("Couldn't load more problems. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  async function toggleBookmark(problem: ProblemSummary) {
    const nowBookmarked = await bookmarksRepository.toggle(problem);
    setBookmarked((prev) => {
      const next = new Set(prev);
      if (nowBookmarked) next.add(problem.slug);
      else next.delete(problem.slug);
      return next;
    });
  }

  const combined = useMemo(() => {
    const bySlug = new Map<string, ProblemSummary>();
    for (const p of [...mockItems, ...realItems]) bySlug.set(p.slug, p);
    let items = Array.from(bySlug.values());

    if (refinements.topic) items = items.filter((p) => metaMap[p.slug]?.topics.includes(refinements.topic));
    if (refinements.dataset) items = items.filter((p) => metaMap[p.slug]?.datasetSlug === refinements.dataset);
    if (refinements.status) items = items.filter((p) => (statusMap[p.slug] ?? "not-started") === refinements.status);
    if (refinements.bookmarkedOnly) items = items.filter((p) => bookmarked.has(p.slug));

    const sorted = [...items];
    if (refinements.sort === "difficulty") {
      sorted.sort((a, b) => DIFFICULTY_RANK[a.difficulty] - DIFFICULTY_RANK[b.difficulty]);
    } else if (refinements.sort === "completion") {
      sorted.sort((a, b) => (metaMap[b.slug]?.completionRatePct ?? 50) - (metaMap[a.slug]?.completionRatePct ?? 50));
    } else if (refinements.sort === "newest") {
      sorted.reverse();
    }
    // "recommended" (default): unsolved-first, then by completion rate desc, so the
    // learner sees approachable, not-yet-done problems before ones they've cleared.
    else {
      sorted.sort((a, b) => {
        const aSolved = statusMap[a.slug] === "solved" ? 1 : 0;
        const bSolved = statusMap[b.slug] === "solved" ? 1 : 0;
        if (aSolved !== bSolved) return aSolved - bSolved;
        return (metaMap[b.slug]?.completionRatePct ?? 50) - (metaMap[a.slug]?.completionRatePct ?? 50);
      });
    }
    return sorted;
  }, [mockItems, realItems, metaMap, statusMap, bookmarked, refinements]);

  return (
    <div className="flex flex-col gap-4">
      <PracticeRefinements value={refinements} onChange={setRefinements} />

      {combined.length === 0 ? (
        <EmptyState
          icon={ListChecks}
          title="No problems match these filters"
          description="Try clearing a filter or search term - there's more to find."
          action={
            <Button variant="secondary" onClick={() => setRefinements(DEFAULT_REFINEMENTS)}>
              Reset refinements
            </Button>
          }
        />
      ) : (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {combined.map((problem) => (
            <ProblemCard
              key={problem.slug}
              problem={problem}
              meta={metaMap[problem.slug]}
              status={statusMap[problem.slug]}
              bookmarked={bookmarked.has(problem.slug)}
              onToggleBookmark={() => toggleBookmark(problem)}
            />
          ))}
          {loading && Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-40 rounded-lg" />)}
        </div>
      )}

      {error && <p className="text-center text-sm text-danger">{error}</p>}

      {nextCursor && (
        <div className="flex justify-center pt-2">
          <Button variant="secondary" onClick={loadMore} disabled={loading}>
            {loading ? "Loading…" : "Load more"}
          </Button>
        </div>
      )}
    </div>
  );
}
