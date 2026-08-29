import type { ProblemSummary } from "@DBArena/api-client";
import { Card } from "@DBArena/ui";
import Link from "next/link";
import { DifficultyBadge, EngineBadge } from "./badges";

export function ProblemCard({ problem }: { problem: ProblemSummary }) {
  return (
    <Link href={`/catalog/${problem.slug}`}>
      <Card className="flex h-full flex-col gap-3 p-4 transition-colors hover:border-accent">
        <div className="flex items-start justify-between gap-2">
          <h3 className="font-semibold leading-snug">{problem.title}</h3>
          <DifficultyBadge difficulty={problem.difficulty} />
        </div>
        <div className="flex flex-wrap gap-1.5">
          {problem.allowedEngines.map((engine) => (
            <EngineBadge key={engine} engine={engine} />
          ))}
        </div>
        {problem.tags.length > 0 && (
          <div className="mt-auto flex flex-wrap gap-1.5 pt-1 text-xs text-fg-muted">
            {problem.tags.map((tag) => (
              <span key={tag} className="rounded bg-bg px-1.5 py-0.5">
                #{tag}
              </span>
            ))}
          </div>
        )}
      </Card>
    </Link>
  );
}
