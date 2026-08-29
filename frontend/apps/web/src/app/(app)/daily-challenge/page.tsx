"use client";

import { Badge, Button, Card, CardContent, Skeleton } from "@DBArena/ui";
import { CalendarCheck, CheckCircle2, Circle, Clock, Flame, Zap } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { DifficultyBadge, EngineBadge } from "@/features/catalog/badges";
import { dailyChallengeRepository, progressRepository } from "@/lib/mock/repositories";
import type { DailyChallenge, StreakInfo } from "@/lib/mock/types";

export default function DailyChallengePage() {
  const [today, setToday] = useState<DailyChallenge | null>(null);
  const [history, setHistory] = useState<DailyChallenge[] | null>(null);
  const [streak, setStreak] = useState<StreakInfo | null>(null);

  useEffect(() => {
    Promise.all([dailyChallengeRepository.getToday(), dailyChallengeRepository.getHistory(), progressRepository.getStreak()]).then(
      ([t, h, s]) => {
        setToday(t);
        setHistory(h);
        setStreak(s);
      },
    );
  }, []);

  if (!today || !history || !streak) {
    return (
      <div className="mx-auto flex max-w-3xl flex-col gap-4 px-6 py-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-40 rounded-lg" />
        <Skeleton className="h-64 rounded-lg" />
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-3xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Daily Challenge</h1>
        <p className="text-sm text-fg-muted">One curated problem a day - keep your streak alive.</p>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile icon={Flame} label="Current streak" value={`${streak.current}d`} tone="text-warning" />
        <StatTile icon={Flame} label="Longest streak" value={`${streak.longest}d`} tone="text-fg-muted" />
        <StatTile icon={Zap} label="Freezes left" value={String(streak.freezesAvailable)} tone="text-info" />
        <StatTile icon={CalendarCheck} label="Completed (7d)" value={`${history.filter((h) => h.completed).length}/${history.length}`} tone="text-success" />
      </div>

      <Card className={today.completed ? "border-success/40" : "border-accent/40"}>
        <CardContent className="flex flex-col gap-3 py-5">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium uppercase tracking-wide text-fg-muted">Today</span>
            {today.completed ? <Badge tone="success">Completed</Badge> : <Badge tone="accent">New</Badge>}
          </div>
          <h2 className="text-xl font-semibold">{today.title}</h2>
          <div className="flex flex-wrap items-center gap-2">
            <DifficultyBadge difficulty={today.difficulty} />
            <EngineBadge engine={today.engine} />
            <Badge tone="neutral">{today.topic}</Badge>
            <span className="flex items-center gap-1 text-xs text-fg-muted">
              <Clock className="h-3.5 w-3.5" aria-hidden />
              {today.estimatedMinutes} min
            </span>
            <span className="flex items-center gap-1 text-xs text-accent">
              <Zap className="h-3.5 w-3.5" aria-hidden />+{today.xpReward} XP
            </span>
          </div>
          <Link href={`/practice/${today.problemSlug}`} className="self-start">
            <Button variant={today.completed ? "secondary" : "primary"}>
              {today.completed ? "Review challenge" : "Start Challenge"}
            </Button>
          </Link>
        </CardContent>
      </Card>

      <section className="flex flex-col gap-3">
        <h2 className="text-sm font-semibold text-fg">Past challenges</h2>
        <Card>
          <CardContent className="flex flex-col divide-y divide-border py-0">
            {history.map((h) => (
              <Link
                key={h.date}
                href={`/practice/${h.problemSlug}`}
                className="flex items-center gap-3 py-2.5 text-sm hover:bg-bg/50"
              >
                {h.completed ? (
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-success" aria-hidden />
                ) : (
                  <Circle className="h-4 w-4 shrink-0 text-fg-muted" aria-hidden />
                )}
                <span className="w-24 shrink-0 text-xs text-fg-muted">{h.date}</span>
                <span className="min-w-0 flex-1 truncate">{h.title}</span>
                <DifficultyBadge difficulty={h.difficulty} />
                <EngineBadge engine={h.engine} />
              </Link>
            ))}
          </CardContent>
        </Card>
      </section>
    </div>
  );
}

function StatTile({ icon: Icon, label, value, tone }: { icon: React.ComponentType<{ className?: string }>; label: string; value: string; tone: string }) {
  return (
    <Card className="flex flex-col items-center gap-1 p-3 text-center">
      <Icon className={`h-5 w-5 ${tone}`} aria-hidden />
      <span className="text-lg font-semibold">{value}</span>
      <span className="text-xs text-fg-muted">{label}</span>
    </Card>
  );
}
