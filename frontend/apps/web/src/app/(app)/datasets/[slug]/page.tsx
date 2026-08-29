import { Badge, Button } from "@DBArena/ui";
import { TerminalSquare } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";
import { ProblemCard } from "@/features/catalog/ProblemCard";
import { DatasetEngineTabs } from "@/features/datasets/DatasetEngineTabs";
import { datasetsRepository, problemsRepository } from "@/lib/mock/repositories";

export default async function DatasetDetailPage({ params }: { params: Promise<{ slug: string }> }) {
  const { slug } = await params;
  const dataset = await datasetsRepository.getDataset(slug);
  if (!dataset) notFound();

  const problems = await problemsRepository.listByDataset(slug);
  const primaryEngine = dataset.engines[0];

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="mb-2 flex items-center gap-2">
            <Badge tone="neutral">{dataset.category}</Badge>
            <span className="text-xs text-fg-muted">{dataset.rowCountLabel}</span>
          </div>
          <h1 className="text-2xl font-semibold">{dataset.name}</h1>
          <p className="mt-1 max-w-2xl text-sm text-fg-muted">{dataset.description}</p>
        </div>
        <Link href={`/playground?dataset=${dataset.slug}${primaryEngine ? `&engine=${primaryEngine}` : ""}`}>
          <Button>
            <TerminalSquare className="h-4 w-4" aria-hidden />
            Open in Playground
          </Button>
        </Link>
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-fg">
          Schema - the same dataset, materialized into {dataset.engines.length} engines
        </h2>
        <DatasetEngineTabs entities={dataset.entities} engines={dataset.engines} />
      </section>

      {problems.length > 0 && (
        <section className="flex flex-col gap-3">
          <h2 className="text-sm font-semibold text-fg">Practice with this dataset</h2>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {problems.map((p) => (
              <ProblemCard key={p.slug} problem={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
