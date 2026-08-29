import { ApiError } from "@DBArena/api-client";
import { Badge, Button, Card, CardContent } from "@DBArena/ui";
import { Clock, Database as DatabaseIcon, TerminalSquare } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { AiAssistantPanel } from "@/features/ai-assistant/AiAssistantPanel";
import { DifficultyBadge, EngineBadge } from "@/features/catalog/badges";
import { BookmarkButton } from "@/features/catalog/BookmarkButton";
import { ProblemCard } from "@/features/catalog/ProblemCard";
import { ShareButton } from "@/features/catalog/ShareButton";
import { SchemaTable } from "@/features/datasets/SchemaTable";
import { createServerCatalogApi } from "@/lib/api/clients";
import { datasetsRepository, problemsRepository } from "@/lib/mock/repositories";

/**
 * Problem detail - tries the real backend first (unchanged behavior from
 * the old `/catalog/[slug]`), falls back to the mock fixture catalog for
 * every slug the real catalog-service doesn't have yet, so a learner never
 * hits a dead 404 while browsing Practice. `isRealProblem` threads through
 * to `AiAssistantPanel` so the real Hint call only ever targets a slug the
 * backend actually knows.
 */
export default async function ProblemDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const catalogApi = createServerCatalogApi();

  let title: string;
  let difficulty: import("@DBArena/api-client").Difficulty;
  let tags: string[];
  let allowedEngines: import("@DBArena/api-client").EngineKind[];
  let statementMarkdown: string;
  let isRealProblem = true;

  try {
    const problem = await catalogApi.getProblem(slug);
    ({ title, difficulty, tags, allowedEngines, statementMarkdown } = problem);
  } catch (err) {
    if (!(err instanceof ApiError && err.status === 404)) throw err;
    isRealProblem = false;
    const mock = await problemsRepository.getMockProblem(slug);
    if (!mock) notFound();
    title = mock.title;
    difficulty = mock.difficulty;
    tags = mock.tags;
    allowedEngines = mock.allowedEngines;
    statementMarkdown = (await problemsRepository.getMockStatement(slug)) ?? "No statement written yet for this problem.";
  }

  const meta = await problemsRepository.getMeta(slug);
  const dataset = meta ? await datasetsRepository.getDataset(meta.datasetSlug) : null;
  const related = await problemsRepository.getRelated(slug, 4);
  const primaryEngine = allowedEngines[0];
  const playgroundHref = `/playground?problem=${encodeURIComponent(slug)}${
    dataset ? `&dataset=${encodeURIComponent(dataset.slug)}` : ""
  }${primaryEngine ? `&engine=${primaryEngine}` : ""}`;

  return (
    <div className="mx-auto grid max-w-6xl grid-cols-1 gap-6 px-6 py-8 lg:grid-cols-[1fr_380px]">
      <div className="flex min-w-0 flex-col gap-4">
        <div>
          <div className="mb-2 flex flex-wrap items-center gap-2">
            <DifficultyBadge difficulty={difficulty} />
            {allowedEngines.map((engine) => (
              <EngineBadge key={engine} engine={engine} />
            ))}
            {meta && (
              <Badge tone="neutral" className="gap-1">
                <Clock className="h-3 w-3" aria-hidden />
                {meta.estimatedMinutes} min · {meta.completionRatePct}% completion
              </Badge>
            )}
          </div>
          <h1 className="text-2xl font-semibold">{title}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            {tags.map((tag) => (
              <span key={tag} className="rounded bg-bg-elevated px-1.5 py-0.5 text-xs text-fg-muted">
                #{tag}
              </span>
            ))}
            {meta?.companyTags.map((c) => (
              <Badge key={c} tone="info">
                {c}
              </Badge>
            ))}
          </div>
        </div>

        <Card>
          <CardContent className="prose-sm max-w-none whitespace-pre-wrap py-5 text-sm leading-relaxed text-fg">
            {statementMarkdown}
          </CardContent>
        </Card>

        {dataset && (
          <div className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-sm font-semibold text-fg">
                <DatabaseIcon className="h-4 w-4 text-accent" aria-hidden />
                Dataset: {dataset.name}
              </h2>
              <Link href={`/datasets/${dataset.slug}`} className="text-xs font-medium text-accent hover:underline">
                View full dataset →
              </Link>
            </div>
            <p className="text-sm text-fg-muted">{dataset.description}</p>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {dataset.entities.slice(0, 2).map((entity) => (
                <SchemaTable key={entity.name} entity={entity} engine={primaryEngine} />
              ))}
            </div>
          </div>
        )}

        {related.length > 0 && (
          <div className="flex flex-col gap-3">
            <h2 className="text-sm font-semibold text-fg">Related problems</h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {related.map((p) => (
                <ProblemCard key={p.slug} problem={p} meta={undefined} />
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex flex-wrap gap-2">
          <Link href={playgroundHref} className="flex-1">
            <Button className="w-full">
              <TerminalSquare className="h-4 w-4" aria-hidden />
              Open Playground
            </Button>
          </Link>
          <BookmarkButton slug={slug} title={title} difficulty={difficulty} />
          <ShareButton path={`/practice/${slug}`} />
        </div>

        <AiAssistantPanel problemSlug={slug} problemTitle={title} isRealProblem={isRealProblem} />
      </div>
    </div>
  );
}
