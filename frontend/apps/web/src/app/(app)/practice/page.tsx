import type { Difficulty, EngineKind, ProblemListQuery } from "@DBArena/api-client";
import { CatalogFilters } from "@/features/catalog/CatalogFilters";
import { PracticeListClient } from "@/features/practice/PracticeListClient";
import { createServerCatalogApi } from "@/lib/api/clients";

export const metadata = {
  title: "Practice · DBArena",
};

/**
 * Problem discovery - the renamed, considerably expanded former Catalog
 * page (root CLAUDE.md: refactor in place, don't fork a duplicate). The
 * server-rendered part (real problems + tags from catalog-service) is
 * unchanged from the original Catalog page; `PracticeListClient` unions it
 * with the mock fixture catalog and adds the refinements the real API
 * doesn't support yet. `/catalog` now redirects here (`app/(app)/catalog`).
 */
export default async function PracticePage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | undefined>>;
}) {
  const sp = await searchParams;

  const query: ProblemListQuery = {
    q: sp.q || undefined,
    difficulty: (sp.difficulty as Difficulty) || undefined,
    engine: (sp.engine as EngineKind) || undefined,
    tag: sp.tag || undefined,
    limit: 24,
  };

  const catalogApi = createServerCatalogApi();
  const [page, tags] = await Promise.all([catalogApi.listProblems(query), catalogApi.listTags()]);

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Practice</h1>
        <p className="text-sm text-fg-muted">
          Browse database problems across SQL and MongoDB - filter by difficulty, engine, topic, and dataset.
        </p>
      </div>

      <CatalogFilters tags={tags} />

      {/* `key` forces a remount when the server-driven filters change, instead
          of an effect syncing initialItems/initialNextCursor into state on
          every render - see PracticeListClient's own note. */}
      <PracticeListClient
        key={`${query.q ?? ""}|${query.difficulty ?? ""}|${query.engine ?? ""}|${query.tag ?? ""}`}
        initialItems={page.items}
        initialNextCursor={page.nextCursor}
        query={query}
      />
    </div>
  );
}
