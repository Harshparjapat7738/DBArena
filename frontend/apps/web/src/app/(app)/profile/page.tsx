"use client";

import type { EngineKind } from "@DBArena/api-client";
import { Avatar, Badge, Card, CardContent, ProgressBar, Skeleton, StatCard } from "@DBArena/ui";
import * as Icons from "lucide-react";
import { CheckCircle2, Flame, Lock, Target, Trophy, Zap } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { EngineBadge } from "@/features/catalog/badges";
import { useAuthStore } from "@/lib/auth/authStore";
import { bookmarksRepository, problemsRepository, progressRepository, submissionsRepository } from "@/lib/mock/repositories";
import type { ActivityItem, BadgeDef, Bookmark, ProblemStatus, StreakInfo, Submission, XpProgress } from "@/lib/mock/types";

const ENGINES: EngineKind[] = ["POSTGRES", "MYSQL", "MONGODB"];

function badgeIcon(name: string): React.ComponentType<{ className?: string }> {
  const icons = Icons as unknown as Record<string, React.ComponentType<{ className?: string }>>;
  return icons[name] ?? Trophy;
}

export default function ProfilePage() {
  const user = useAuthStore((s) => s.user);
  const [xp, setXp] = useState<XpProgress | null>(null);
  const [streak, setStreak] = useState<StreakInfo | null>(null);
  const [badges, setBadges] = useState<BadgeDef[] | null>(null);
  const [activity, setActivity] = useState<ActivityItem[] | null>(null);
  const [bookmarks, setBookmarks] = useState<Bookmark[] | null>(null);
  const [statuses, setStatuses] = useState<Record<string, ProblemStatus> | null>(null);
  const [submissions, setSubmissions] = useState<Submission[] | null>(null);

  useEffect(() => {
    progressRepository.getXp().then(setXp);
    progressRepository.getStreak().then(setStreak);
    progressRepository.getBadges().then(setBadges);
    progressRepository.getActivity(8).then(setActivity);
    bookmarksRepository.list().then(setBookmarks);
    problemsRepository.getAllStatuses().then(setStatuses);
    submissionsRepository.list().then(setSubmissions);
  }, []);

  if (!xp || !streak || !badges || !activity || !bookmarks || !statuses || !submissions) {
    return (
      <div className="mx-auto flex max-w-4xl flex-col gap-4 px-6 py-8">
        <Skeleton className="h-24 rounded-lg" />
        <Skeleton className="h-40 rounded-lg" />
      </div>
    );
  }

  const solved = Object.values(statuses).filter((s) => s === "solved").length;
  const engineCounts: Record<EngineKind, number> = { POSTGRES: 0, MYSQL: 0, MONGODB: 0 };
  for (const s of submissions) if (s.status === "ACCEPTED") engineCounts[s.engine] += 1;
  const maxEngineCount = Math.max(1, ...Object.values(engineCounts));

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6 px-6 py-8">
      <div className="flex flex-wrap items-center gap-4">
        <Avatar name={user?.displayName ?? "?"} size="lg" />
        <div>
          <h1 className="text-2xl font-semibold">{user?.displayName}</h1>
          <p className="text-sm text-fg-muted">{user?.email}</p>
          <div className="mt-1 flex items-center gap-2">
            <Badge tone="accent">{xp.rank}</Badge>
            <Badge tone="neutral">Level {xp.level}</Badge>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="XP" value={xp.xp.toLocaleString()} icon={Zap} tone="accent" />
        <StatCard label="Streak" value={`${streak.current}d`} icon={Flame} tone="warning" />
        <StatCard label="Solved" value={solved} icon={CheckCircle2} tone="success" />
        <StatCard label="Badges" value={`${badges.filter((b) => b.earned).length}/${badges.length}`} icon={Trophy} tone="info" />
      </div>

      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Database expertise</h2>
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
        <h2 className="text-lg font-semibold">Achievements</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {badges.map((b) => {
            const Icon = badgeIcon(b.icon);
            return (
              <Card key={b.id} className={`flex flex-col items-center gap-1.5 p-3 text-center ${!b.earned ? "opacity-50" : ""}`}>
                {b.earned ? <Icon className="h-6 w-6 text-accent" aria-hidden /> : <Lock className="h-6 w-6 text-fg-muted" aria-hidden />}
                <span className="text-xs font-medium">{b.name}</span>
                <span className="text-[10px] text-fg-muted">{b.description}</span>
              </Card>
            );
          })}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Recent activity</h2>
          <Card>
            <CardContent className="flex flex-col divide-y divide-border py-0">
              {activity.map((a) => (
                <div key={a.id} className="flex items-center gap-2 py-2 text-sm">
                  <Target className="h-3.5 w-3.5 shrink-0 text-fg-muted" aria-hidden />
                  <span className="min-w-0 flex-1 truncate">{a.title}</span>
                </div>
              ))}
            </CardContent>
          </Card>
        </section>

        <section className="flex flex-col gap-3">
          <h2 className="text-lg font-semibold">Bookmarked problems</h2>
          <Card>
            <CardContent className="flex flex-col divide-y divide-border py-0">
              {bookmarks.length === 0 ? (
                <p className="py-4 text-sm text-fg-muted">No bookmarks yet.</p>
              ) : (
                bookmarks.map((b) => (
                  <Link key={b.problemSlug} href={`/practice/${b.problemSlug}`} className="flex items-center gap-2 py-2 text-sm hover:text-accent">
                    <span className="min-w-0 flex-1 truncate">{b.title}</span>
                  </Link>
                ))
              )}
            </CardContent>
          </Card>
        </section>
      </div>
    </div>
  );
}
