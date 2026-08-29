import { ApiError } from "@DBArena/api-client";
import { Card, CardContent } from "@DBArena/ui";
import { notFound } from "next/navigation";
import { DifficultyBadge, EngineBadge } from "@/features/catalog/badges";
import { HintPanel } from "@/features/ai-assistant/HintPanel";
import { createServerCatalogApi } from "@/lib/api/clients";

export default async function ProblemDetailPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const catalogApi = createServerCatalogApi();

  let problem;
  try {
    problem = await catalogApi.getProblem(slug);
  } catch (err) {
    if (err instanceof ApiError && err.status === 404) notFound();
    throw err;
  }

  return (
    <div className="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-6 py-8 lg:grid-cols-[1fr_360px]">
      <div className="flex flex-col gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <DifficultyBadge difficulty={problem.difficulty} />
            {problem.allowedEngines.map((engine) => (
              <EngineBadge key={engine} engine={engine} />
            ))}
          </div>
          <h1 className="text-2xl font-semibold">{problem.title}</h1>
          {problem.tags.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1.5">
              {problem.tags.map((tag) => (
                <span key={tag} className="rounded bg-bg-elevated px-1.5 py-0.5 text-xs text-fg-muted">
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>

        <Card>
          <CardContent className="prose-sm max-w-none whitespace-pre-wrap py-5 text-sm leading-relaxed text-fg">
            {problem.statementMarkdown}
          </CardContent>
        </Card>
      </div>

      <div>
        <HintPanel problemSlug={problem.slug} />
      </div>
    </div>
  );
}
