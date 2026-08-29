"use client";

import { Badge, Button, Card, CardContent, ProgressBar, Skeleton } from "@DBArena/ui";
import { CheckCircle2, ChevronRight, Circle, Clock } from "lucide-react";
import Link from "next/link";
import { notFound, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { learningRepository } from "@/lib/mock/repositories";
import type { LearningPath, Lesson } from "@/lib/mock/types";

export default function LearningPathPage() {
  const params = useParams<{ pathSlug: string }>();
  const [path, setPath] = useState<LearningPath | null | undefined>(undefined);

  useEffect(() => {
    learningRepository.getPath(params.pathSlug).then(setPath);
  }, [params.pathSlug]);

  async function completeLesson(lesson: Lesson) {
    if (!path || lesson.completed) return;
    await learningRepository.completeLesson(path.slug, lesson.slug);
    setPath(await learningRepository.getPath(path.slug));
  }

  if (path === undefined) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-24 rounded-lg" />
        <Skeleton className="h-64 rounded-lg" />
      </div>
    );
  }
  if (path === null) notFound();

  const done = path.lessons.filter((l) => l.completed).length;
  const pct = Math.round((done / path.lessons.length) * 100);
  const nextLesson = path.lessons.find((l) => !l.completed);

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 px-6 py-8">
      <div>
        <div className="mb-2 flex items-center gap-2">
          <Badge tone="neutral">{path.level}</Badge>
          <Badge tone="info">{path.engine}</Badge>
        </div>
        <h1 className="text-2xl font-semibold">{path.title}</h1>
        <p className="mt-1 text-sm text-fg-muted">{path.description}</p>
      </div>

      <Card>
        <CardContent className="flex flex-col gap-3 py-4">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium">{done} of {path.lessons.length} lessons complete</span>
            <span className="text-fg-muted">{pct}%</span>
          </div>
          <ProgressBar value={pct} />
          {nextLesson && (
            <Button size="sm" className="mt-1 self-start" onClick={() => completeLesson(nextLesson)}>
              Mark &ldquo;{nextLesson.title}&rdquo; complete
            </Button>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-2">
        {path.lessons.map((lesson, i) => (
          <Card key={lesson.slug} className="flex items-center gap-3 p-4">
            <button
              type="button"
              onClick={() => completeLesson(lesson)}
              aria-label={lesson.completed ? "Completed" : "Mark complete"}
              className="shrink-0"
            >
              {lesson.completed ? (
                <CheckCircle2 className="h-5 w-5 text-success" aria-hidden />
              ) : (
                <Circle className="h-5 w-5 text-fg-muted hover:text-accent" aria-hidden />
              )}
            </button>
            <div className="min-w-0 flex-1">
              <div className="text-sm font-medium">
                {i + 1}. {lesson.title}
              </div>
              <p className="truncate text-xs text-fg-muted">{lesson.summary}</p>
            </div>
            <span className="flex shrink-0 items-center gap-1 text-xs text-fg-muted">
              <Clock className="h-3 w-3" aria-hidden />
              {lesson.durationMinutes}m
            </span>
            {lesson.practiceProblemSlug && (
              <Link
                href={`/practice/${lesson.practiceProblemSlug}`}
                className="flex shrink-0 items-center gap-0.5 text-xs font-medium text-accent hover:underline"
              >
                Practice <ChevronRight className="h-3 w-3" aria-hidden />
              </Link>
            )}
          </Card>
        ))}
      </div>
    </div>
  );
}
