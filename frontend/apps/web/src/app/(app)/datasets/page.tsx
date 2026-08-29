import { DatasetGrid } from "@/features/datasets/DatasetGrid";
import { datasetsRepository } from "@/lib/mock/repositories";

export const metadata = { title: "Datasets · DBArena" };

/**
 * Dataset catalog - the core product idea made browsable: one dataset,
 * materialized into Postgres, MySQL, and MongoDB alike. A Server Component
 * (the mock dataset fixtures need no localStorage/personalization), same
 * posture as the original Catalog page's server-rendered list.
 */
export default async function DatasetsPage() {
  const datasets = await datasetsRepository.listDatasets();

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Datasets</h1>
        <p className="text-sm text-fg-muted">
          Every dataset here is authored once and materialized identically into PostgreSQL, MySQL, and MongoDB -
          browse the schema, sample rows, and problems for each.
        </p>
      </div>
      <DatasetGrid datasets={datasets} />
    </div>
  );
}
