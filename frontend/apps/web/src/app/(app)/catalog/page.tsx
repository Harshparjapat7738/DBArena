import type { Difficulty, EngineKind, ProblemListQuery } from "@DBArena/api-client";
import { CatalogFilters } from "@/features/catalog/CatalogFilters";
import { ProblemListClient } from "@/features/catalog/ProblemListClient";
import { createServerCatalogApi } from "@/lib/api/clients";

export const metadata = {
  title: "Catalog · DBArena",
};

/**
 * Server Component: the catalogue is explicitly NOT a client island per
 * frontend/CLAUDE.md. Reads `searchParams` (Next 15: async), fetches the
 * first page + tag list directly against api-gateway server-side (public
 * data, no auth, no CORS concern), and hands the results down to the
 * client islands that need interactivity.
 */
export default async function CatalogPage({
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
        <h1 className="text-2xl font-semibold">Problem catalog</h1>
        <p className="text-sm text-fg-muted">Browse and practice real-world database problems.</p>
      </div>

      <CatalogFilters tags={tags} />

      <ProblemListClient initialItems={page.items} initialNextCursor={page.nextCursor} query={query} />
    </div>
  );
}
