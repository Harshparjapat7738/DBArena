"use client";

import type { ProblemListQuery, ProblemSummary } from "@dbforge/api-client";
import { hasMorePages } from "@dbforge/api-client";
import { Button, Skeleton } from "@dbforge/ui";
import { useState } from "react";
import { catalogApi } from "@/lib/api/clients";
import { ProblemCard } from "./ProblemCard";

/**
 * The one genuinely-needed client island in the catalog page: true
 * "load more" incremental pagination needs client-side state, which a
 * Server Component can't hold. Seeded from the server-rendered first page
 * so there is no loading flash on initial visit.
 */
export function ProblemListClient({
  initialItems,
  initialNextCursor,
  query,
}: {
  initialItems: ProblemSummary[];
  initialNextCursor: string | null;
  query: ProblemListQuery;
}) {
  const [items, setItems] = useState(initialItems);
  const [nextCursor, setNextCursor] = useState(initialNextCursor);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadMore() {
    if (!nextCursor || loading) return;
    setLoading(true);
    setError(null);
    try {
      const page = await catalogApi.listProblems({ ...query, cursor: nextCursor });
      setItems((prev) => [...prev, ...page.items]);
      setNextCursor(hasMorePages(page) ? page.nextCursor : null);
    } catch {
      setError("Couldn't load more problems. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  if (items.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-border p-12 text-center text-sm text-fg-muted">
        No problems match these filters yet. Try clearing a filter.
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((problem) => (
          <ProblemCard key={problem.slug} problem={problem} />
        ))}
        {loading &&
          Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32 rounded-lg" />)}
      </div>

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
