"use client";

import { Badge, Card, ProgressBar, Skeleton } from "@DBArena/ui";
import { Clock, GraduationCap } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { learningRepository } from "@/lib/mock/repositories";
import type { LearningPath } from "@/lib/mock/types";

const LEVEL_TONE = { Beginner: "success", Intermediate: "warning", Advanced: "danger" } as const;

export default function LearningPage() {
  const [paths, setPaths] = useState<LearningPath[] | null>(null);

  useEffect(() => {
    learningRepository.listPaths().then(setPaths);
  }, []);

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Learning</h1>
        <p className="text-sm text-fg-muted">Structured paths from SQL fundamentals to query optimization and interview prep.</p>
      </div>

      {!paths ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-44 rounded-lg" />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {paths.map((p) => {
            const done = p.lessons.filter((l) => l.completed).length;
            const pct = Math.round((done / p.lessons.length) * 100);
            return (
              <Link key={p.slug} href={`/learning/${p.slug}`}>
                <Card className="flex h-full flex-col gap-3 p-4 transition-colors hover:border-accent">
                  <div className="flex items-center justify-between">
                    <GraduationCap className="h-5 w-5 text-accent" aria-hidden />
                    <Badge tone={LEVEL_TONE[p.level]}>{p.level}</Badge>
                  </div>
                  <div>
                    <h3 className="font-semibold">{p.title}</h3>
                    <p className="mt-1 text-sm text-fg-muted">{p.description}</p>
                  </div>
                  <div className="mt-auto flex flex-col gap-1.5">
                    <ProgressBar value={pct} size="sm" />
                    <div className="flex items-center justify-between text-xs text-fg-muted">
                      <span>{done}/{p.lessons.length} lessons</span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" aria-hidden />
                        {p.estimatedHours}h
                      </span>
                    </div>
                  </div>
                </Card>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
