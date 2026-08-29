"use client";

import type { EngineKind } from "@DBArena/api-client";
import { Badge, Card, CardContent, ProgressBar, Skeleton, StatCard } from "@DBArena/ui";
import { AlertTriangle, CheckCircle2, Target, TrendingUp, XCircle, Zap } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { EngineBadge } from "@/features/catalog/badges";
import { learningRepository, problemsRepository, progressRepository, submissionsRepository } from "@/lib/mock/repositories";
import type { LearningPath, ProblemStatus, SkillMastery, Submission } from "@/lib/mock/types";

const ENGINES: EngineKind[] = ["POSTGRES", "MYSQL", "MONGODB"];

export default function ProgressPage() {
  const [mastery, setMastery] = useState<SkillMastery[] | null>(null);
  const [submissions, setSubmissions] = useState<Submission[] | null>(null);
  const [statuses, setStatuses] = useState<Record<string, ProblemStatus> | null>(null);
  const [paths, setPaths] = useState<LearningPath[] | null>(null);

  useEffect(() => {
    Promise.all([
      progressRepository.getMastery(),
      submissionsRepository.list(),
      problemsRepository.getAllStatuses(),
      learningRepository.listPaths(),
    ]).then(([m, s, st, p]) => {
      setMastery(m);
      setSubmissions(s);
      setStatuses(st);
      setPaths(p);
    });
  }, []);

  if (!mastery || !submissions || !statuses || !paths) {
    return (
      <div className="mx-auto flex max-w-5xl flex-col gap-4 px-6 py-8">
        <Skeleton className="h-8 w-56" />
        <Skeleton className="h-24 rounded-lg" />
        <Skeleton className="h-64 rounded-lg" />
      </div>
    );
  }

  const solved = Object.values(statuses).filter((s) => s === "solved").length;
  const attempted = Object.values(statuses).filter((s) => s === "attempted").length;
  const accepted = submissions.filter((s) => s.status === "ACCEPTED").length;
  const accuracy = submissions.length > 0 ? Math.round((accepted / submissions.length) * 100) : 0;
  const avgExecutionMs =
    submissions.length > 0 ? Math.round(submissions.reduce((sum, s) => sum + s.executionMs, 0) / submissions.length) : 0;

  const engineCounts: Record<EngineKind, number> = { POSTGRES: 0, MYSQL: 0, MONGODB: 0 };
  for (const s of submissions) if (s.status === "ACCEPTED") engineCounts[s.engine] += 1;
  const maxEngineCount = Math.max(1, ...Object.values(engineCounts));

  const weakTopics = [...mastery].sort((a, b) => a.masteryPct - b.masteryPct).slice(0, 3);
  const recommendedPaths = paths.filter((p) =>
    weakTopics.some((w) => p.title.toLowerCase().includes(w.topic.toLowerCase().split(" ")[0] ?? "")),
  );

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-8 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Progress</h1>
        <p className="text-sm text-fg-muted">Your solving accuracy, topic mastery, and where to focus next.</p>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Solved" value={solved} icon={CheckCircle2} tone="success" />
        <StatCard label="Attempted" value={attempted} icon={Target} tone="warning" />
        <StatCard label="Accuracy" value={`${accuracy}%`} icon={TrendingUp} tone="accent" hint={`${accepted}/${submissions.length} submissions`} />
        <StatCard label="Avg. execution" value={`${avgExecutionMs}ms`} icon={Zap} tone="info" />
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Engine-wise progress</h2>
        <Card>
          <CardContent className="flex flex-col gap-3 py-4">
            {ENGINES.map((e) => (
              <div key={e} className="flex items-center gap-3">
                <span className="w-28 shrink-0">
                  <EngineBadge engine={e} />
                </span>
                <ProgressBar value={(engineCounts[e] / maxEngineCount) * 100} className="flex-1" />
                <span className="w-16 shrink-0 text-right text-xs text-fg-muted">{engineCounts[e]} solved</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </section>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Topic mastery</h2>
        <Card>
          <CardContent className="flex flex-col gap-3 py-4">
            {mastery.map((m) => (
              <div key={m.topic} className="flex items-center gap-3">
                <span className="w-36 shrink-0 truncate text-sm">{m.topic}</span>
                <ProgressBar value={m.masteryPct} tone={m.masteryPct >= 70 ? "success" : m.masteryPct >= 40 ? "warning" : "danger"} className="flex-1" />
                <span className="w-24 shrink-0 text-right text-xs text-fg-muted">{m.problemsSolved}/{m.problemsTotal} solved</span>
              </div>
            ))}
          </CardContent>
        </Card>
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Recent performance</h2>
          <Card>
            <CardContent className="flex flex-col divide-y divide-border py-0">
              {submissions.slice(0, 6).map((s) => (
                <Link key={s.id} href={`/submissions/${s.id}`} className="flex items-center gap-3 py-2.5 text-sm hover:bg-bg/50">
                  {s.status === "ACCEPTED" ? (
                    <CheckCircle2 className="h-4 w-4 shrink-0 text-success" aria-hidden />
                  ) : (
                    <XCircle className="h-4 w-4 shrink-0 text-danger" aria-hidden />
                  )}
                  <span className="min-w-0 flex-1 truncate">{s.problemTitle}</span>
                  <span className="shrink-0 text-xs text-fg-muted">{s.executionMs}ms</span>
                </Link>
              ))}
            </CardContent>
          </Card>
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Weak areas</h2>
          <Card>
            <CardContent className="flex flex-col gap-3 py-4">
              {weakTopics.map((w) => (
                <div key={w.topic} className="flex items-center gap-2 text-sm">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-warning" aria-hidden />
                  <span className="flex-1">{w.topic}</span>
                  <Badge tone="warning">{w.masteryPct}%</Badge>
                </div>
              ))}
              {recommendedPaths.length > 0 && (
                <div className="mt-2 flex flex-col gap-1.5 border-t border-border pt-3">
                  <span className="text-xs font-medium text-fg-muted">Recommended next</span>
                  {recommendedPaths.map((p) => (
                    <Link key={p.slug} href={`/learning/${p.slug}`} className="text-sm font-medium text-accent hover:underline">
                      {p.title} →
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
