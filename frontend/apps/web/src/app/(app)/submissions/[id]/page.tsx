"use client";

import { Button, Card, CardContent, Skeleton } from "@DBArena/ui";
import { AlertOctagon, CheckCircle2, Gauge, Rows3, Sparkles, TerminalSquare, Timer, XCircle } from "lucide-react";
import Link from "next/link";
import { notFound, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { EngineBadge } from "@/features/catalog/badges";
import { submissionsRepository } from "@/lib/mock/repositories";
import type { Submission, SubmissionVerdict } from "@/lib/mock/types";

const VERDICT: Record<
  SubmissionVerdict,
  { label: string; icon: React.ComponentType<{ className?: string }>; iconClass: string; bannerClass: string }
> = {
  ACCEPTED: { label: "Accepted", icon: CheckCircle2, iconClass: "text-success", bannerClass: "border-success/40 bg-success/10" },
  WRONG_ANSWER: { label: "Wrong Answer", icon: XCircle, iconClass: "text-warning", bannerClass: "border-warning/40 bg-warning/10" },
  RUNTIME_ERROR: { label: "Runtime Error", icon: AlertOctagon, iconClass: "text-danger", bannerClass: "border-danger/40 bg-danger/10" },
};

export default function SubmissionResultPage() {
  const params = useParams<{ id: string }>();
  const [submission, setSubmission] = useState<Submission | null | undefined>(undefined);

  useEffect(() => {
    submissionsRepository.get(params.id).then(setSubmission);
  }, [params.id]);

  if (submission === undefined) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 rounded-lg" />
      </div>
    );
  }
  if (submission === null) notFound();

  const verdict = VERDICT[submission.status];

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 px-6 py-8">
      <div>
        <Link href={`/practice/${submission.problemSlug}`} className="text-xs font-medium text-accent hover:underline">
          ← {submission.problemTitle}
        </Link>
        <div className="mt-2 flex items-center gap-3">
          <verdict.icon className={`h-7 w-7 ${verdict.iconClass}`} aria-hidden />
          <h1 className="text-2xl font-semibold">{verdict.label}</h1>
          <EngineBadge engine={submission.engine} />
        </div>
        <p className="mt-1 text-sm text-fg-muted">Submitted {new Date(submission.submittedAt).toLocaleString()}</p>
      </div>

      {submission.message && (
        <div className={`rounded-md border p-3 text-sm text-fg ${verdict.bannerClass}`}>{submission.message}</div>
      )}

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <MetricTile icon={Timer} label="Execution time" value={`${submission.executionMs}ms`} />
        <MetricTile icon={Rows3} label="Rows returned" value={String(submission.rowsReturned)} />
        <MetricTile icon={CheckCircle2} label="Tests passed" value={`${submission.testsPassed}/${submission.testsTotal}`} />
        <MetricTile icon={Gauge} label="Plan cost / rows examined" value={`${submission.planCost} / ${submission.rowsExamined}`} />
      </div>
      <p className="text-xs text-fg-muted">
        These are simulated execution metrics for this demo, not a claim about algorithmic complexity - a real run
        reports plan cost and rows examined, never a Big-O bound.
      </p>

      <Card>
        <CardContent className="py-4">
          <div className="mb-2 text-xs font-medium text-fg-muted">Your query</div>
          <pre className="overflow-x-auto rounded-md bg-editor-bg p-3 font-mono text-sm text-editor-fg">{submission.query}</pre>
        </CardContent>
      </Card>

      <div className="flex flex-wrap gap-2">
        <Link
          href={`/playground?problem=${submission.problemSlug}&engine=${submission.engine}&query=${encodeURIComponent(submission.query)}`}
        >
          <Button variant="secondary">
            <TerminalSquare className="h-4 w-4" aria-hidden />
            Open in Playground
          </Button>
        </Link>
        <Link href={`/practice/${submission.problemSlug}`}>
          <Button variant="secondary">
            <Sparkles className="h-4 w-4" aria-hidden />
            Ask AI about this
          </Button>
        </Link>
        {submission.status !== "ACCEPTED" && (
          <Link href={`/playground?problem=${submission.problemSlug}&engine=${submission.engine}`}>
            <Button>Retry</Button>
          </Link>
        )}
      </div>
    </div>
  );
}

function MetricTile({ icon: Icon, label, value }: { icon: React.ComponentType<{ className?: string }>; label: string; value: string }) {
  return (
    <Card className="flex flex-col items-center gap-1 p-3 text-center">
      <Icon className="h-5 w-5 text-accent" aria-hidden />
      <span className="text-base font-semibold">{value}</span>
      <span className="text-xs text-fg-muted">{label}</span>
    </Card>
  );
}
