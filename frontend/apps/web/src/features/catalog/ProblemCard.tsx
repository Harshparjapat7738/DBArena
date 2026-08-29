import type { ProblemSummary } from "@DBArena/api-client";
import { Badge, Card } from "@DBArena/ui";
import { Bookmark, CheckCircle2, Circle, Clock, Database } from "lucide-react";
import Link from "next/link";
import type { ProblemMeta, ProblemStatus } from "@/lib/mock/types";
import { DifficultyBadge, EngineBadge } from "./badges";

const STATUS_ICON: Record<ProblemStatus, React.ComponentType<{ className?: string }>> = {
  solved: CheckCircle2,
  attempted: Circle,
  "not-started": Circle,
};

const STATUS_CLASS: Record<ProblemStatus, string> = {
  solved: "text-success",
  attempted: "text-warning",
  "not-started": "text-fg-muted",
};

export interface ProblemCardProps {
  problem: ProblemSummary;
  /** Estimated time / completion rate / dataset - absent for a problem this session's mock layer has no meta for yet. */
  meta?: ProblemMeta | null;
  status?: ProblemStatus;
  bookmarked?: boolean;
  onToggleBookmark?: () => void;
  href?: string;
}

/**
 * The one problem card used everywhere a problem is listed - Practice
 * grid, Dashboard recommendations, related problems, bookmarks. `meta`/
 * `status`/`bookmarked` are all optional so it degrades gracefully for a
 * real backend problem this session's mock layer hasn't annotated.
 */
export function ProblemCard({ problem, meta, status = "not-started", bookmarked, onToggleBookmark, href }: ProblemCardProps) {
  const StatusIcon = STATUS_ICON[status];

  return (
    <Card className="group relative flex h-full flex-col gap-3 p-4 transition-colors hover:border-accent">
      <Link href={href ?? `/practice/${problem.slug}`} className="absolute inset-0" aria-label={problem.title} />
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2">
          <StatusIcon className={`h-4 w-4 shrink-0 ${STATUS_CLASS[status]}`} aria-label={status.replace("-", " ")} />
          <h3 className="font-semibold leading-snug">{problem.title}</h3>
        </div>
        <DifficultyBadge difficulty={problem.difficulty} />
      </div>

      <div className="flex flex-wrap gap-1.5">
        {problem.allowedEngines.map((engine) => (
          <EngineBadge key={engine} engine={engine} />
        ))}
      </div>

      {meta && (
        <div className="flex flex-wrap items-center gap-3 text-xs text-fg-muted">
          <span className="flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" aria-hidden />
            {meta.estimatedMinutes} min
          </span>
          <span>{meta.completionRatePct}% completion</span>
          <span className="flex items-center gap-1">
            <Database className="h-3.5 w-3.5" aria-hidden />
            {meta.datasetSlug}
          </span>
        </div>
      )}

      {meta && meta.companyTags.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {meta.companyTags.map((c) => (
            <Badge key={c} tone="neutral">
              {c}
            </Badge>
          ))}
        </div>
      )}

      {problem.tags.length > 0 && (
        <div className="mt-auto flex flex-wrap gap-1.5 pt-1 text-xs text-fg-muted">
          {problem.tags.map((tag) => (
            <span key={tag} className="rounded bg-bg px-1.5 py-0.5">
              #{tag}
            </span>
          ))}
        </div>
      )}

      {onToggleBookmark && (
        <button
          type="button"
          onClick={(e) => {
            e.preventDefault();
            onToggleBookmark();
          }}
          aria-label={bookmarked ? "Remove bookmark" : "Bookmark this problem"}
          aria-pressed={bookmarked}
          className="absolute right-3 top-3 z-10 rounded-md p-1 text-fg-muted opacity-0 transition-opacity hover:text-accent focus-visible:opacity-100 focus-visible:outline-none group-hover:opacity-100"
        >
          <Bookmark className={`h-4 w-4 ${bookmarked ? "fill-accent text-accent" : ""}`} aria-hidden />
        </button>
      )}
    </Card>
  );
}
