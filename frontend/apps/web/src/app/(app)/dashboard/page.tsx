"use client";

import type { ProblemSummary } from "@DBArena/api-client";
import { Badge, Button, Card, CardContent, ProgressBar, Skeleton, StatCard } from "@DBArena/ui";
import {
  ArrowRight,
  BookOpen,
  Boxes,
  CalendarCheck,
  ChevronRight,
  Clock,
  Database,
  Flame,
  Gauge,
  Layers,
  Network,
  Sparkles,
  Terminal,
  Trophy,
  Zap,
} from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ProblemCard } from "@/features/catalog/ProblemCard";
import { useAuthStore } from "@/lib/auth/authStore";
import {
  dailyChallengeRepository,
  datasetsRepository,
  leaderboardRepository,
  learningRepository,
  problemsRepository,
  progressRepository,
} from "@/lib/mock/repositories";
import type {
  ActivityItem,
  BadgeDef,
  DailyChallenge,
  Dataset,
  LeaderboardEntry,
  LearningPath,
  ProblemMeta,
  SkillMastery,
  StreakInfo,
  XpProgress,
} from "@/lib/mock/types";

const QUICK_PRACTICE = [
  { label: "SQL", icon: Terminal, href: "/practice", tone: "accent" as const },
  { label: "PostgreSQL", icon: Database, href: "/practice?engine=POSTGRES", tone: "info" as const },
  { label: "MySQL", icon: Database, href: "/practice?engine=MYSQL", tone: "warning" as const },
  { label: "MongoDB", icon: Boxes, href: "/practice?engine=MONGODB", tone: "success" as const },
  { label: "Query Optimization", icon: Gauge, href: "/learning/query-optimization", tone: "danger" as const },
  { label: "Database Design", icon: Network, href: "/learning/database-design", tone: "neutral" as const },
];

interface DashboardData {
  xp: XpProgress;
  streak: StreakInfo;
  mastery: SkillMastery[];
  activity: ActivityItem[];
  badges: BadgeDef[];
  daily: DailyChallenge;
  recommended: ProblemSummary[];
  recommendedMeta: Record<string, ProblemMeta>;
  datasets: Dataset[];
  leaderboard: LeaderboardEntry[];
  paths: LearningPath[];
}

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [data, setData] = useState<DashboardData | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      progressRepository.getXp(),
      progressRepository.getStreak(),
      progressRepository.getMastery(),
      progressRepository.getActivity(6),
      progressRepository.getBadges(),
      dailyChallengeRepository.getToday(),
      problemsRepository.getRecommended(4),
      problemsRepository.getAllMeta(),
      datasetsRepository.listDatasets(),
      leaderboardRepository.getLeaderboard("global", user?.displayName ?? "You"),
      learningRepository.listPaths(),
    ]).then(([xp, streak, mastery, activity, badges, daily, recommended, recommendedMeta, datasets, leaderboard, paths]) => {
      if (cancelled) return;
      setData({ xp, streak, mastery, activity, badges, daily, recommended, recommendedMeta, datasets, leaderboard, paths });
    });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!data) return <DashboardSkeleton />;

  const continuePath = data.paths.find((p) => p.lessons.some((l) => l.completed) && p.lessons.some((l) => !l.completed));
  const nextLesson = continuePath?.lessons.find((l) => !l.completed);
  const pathProgress = continuePath
    ? Math.round((continuePath.lessons.filter((l) => l.completed).length / continuePath.lessons.length) * 100)
    : 0;

  const myRank = data.leaderboard.find((e) => e.isCurrentUser);
  const nearby = data.leaderboard.filter((e) => myRank && Math.abs(e.rank - myRank.rank) <= 2);
  const earnedBadges = data.badges.filter((b) => b.earned);
  const nextBadge = data.badges.find((b) => !b.earned);

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-8 px-6 py-8">
      {/* 1. Welcome header */}
      <section className="flex flex-col gap-4">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold">
              Welcome back{user?.displayName ? `, ${user.displayName.split(" ")[0]}` : ""} 👋
            </h1>
            <p className="text-sm text-fg-muted">Here&apos;s where you left off, and what&apos;s worth doing today.</p>
          </div>
          <div className="flex gap-2">
            <Link href="/playground">
              <Button variant="secondary">Open Playground</Button>
            </Link>
            <Link href="/daily-challenge">
              <Button>
                <Sparkles className="h-4 w-4" aria-hidden />
                Today&apos;s Challenge
              </Button>
            </Link>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <StatCard label="Streak" value={`${data.streak.current} days`} icon={Flame} tone="warning" hint={`Best: ${data.streak.longest}`} />
          <StatCard label="XP" value={data.xp.xp.toLocaleString()} icon={Zap} tone="accent" hint={data.xp.rank} />
          <StatCard label="Global rank" value={myRank ? `#${myRank.rank}` : "—"} icon={Trophy} tone="info" hint={`${myRank?.solved ?? 0} solved`} />
          <StatCard label="Badges" value={`${earnedBadges.length}/${data.badges.length}`} icon={Layers} tone="success" hint="earned" />
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* 2. Continue learning */}
        <Card>
          <CardContent className="flex flex-col gap-3 py-5">
            <div className="flex items-center gap-2 text-sm font-semibold">
              <BookOpen className="h-4 w-4 text-accent" aria-hidden />
              Continue learning
            </div>
            {continuePath && nextLesson ? (
              <>
                <div>
                  <div className="text-base font-semibold">{continuePath.title}</div>
                  <p className="text-sm text-fg-muted">Next: {nextLesson.title}</p>
                </div>
                <ProgressBar value={pathProgress} />
                <div className="flex items-center justify-between text-xs text-fg-muted">
                  <span>{pathProgress}% complete</span>
                  <span>{continuePath.lessons.length} lessons</span>
                </div>
                <Link href={`/learning/${continuePath.slug}`}>
                  <Button size="sm" className="mt-1">
                    Continue <ArrowRight className="h-3.5 w-3.5" aria-hidden />
                  </Button>
                </Link>
              </>
            ) : (
              <>
                <p className="text-sm text-fg-muted">Pick a learning path to get a structured start.</p>
                <Link href="/learning">
                  <Button size="sm">Browse learning paths</Button>
                </Link>
              </>
            )}
          </CardContent>
        </Card>

        {/* 3. Daily challenge */}
        <Card className={data.daily.completed ? "border-success/40" : ""}>
          <CardContent className="flex flex-col gap-3 py-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-sm font-semibold">
                <CalendarCheck className="h-4 w-4 text-accent" aria-hidden />
                Daily challenge
              </div>
              {data.daily.completed && <Badge tone="success">Completed</Badge>}
            </div>
            <div className="text-base font-semibold">{data.daily.title}</div>
            <div className="flex flex-wrap items-center gap-3 text-xs text-fg-muted">
              <Badge tone="neutral">{data.daily.engine}</Badge>
              <Badge tone="neutral">{data.daily.topic}</Badge>
              <span className="flex items-center gap-1">
                <Clock className="h-3.5 w-3.5" aria-hidden />
                {data.daily.estimatedMinutes} min
              </span>
              <span className="flex items-center gap-1 text-accent">
                <Zap className="h-3.5 w-3.5" aria-hidden />+{data.daily.xpReward} XP
              </span>
            </div>
            <Link href={`/practice/${data.daily.problemSlug}`}>
              <Button size="sm" className="mt-1" variant={data.daily.completed ? "secondary" : "primary"}>
                {data.daily.completed ? "Review" : "Start Challenge"} <ArrowRight className="h-3.5 w-3.5" aria-hidden />
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>

      {/* 4. Quick practice */}
      <section className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold">Quick practice</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
          {QUICK_PRACTICE.map((q) => (
            <Link key={q.label} href={q.href}>
              <Card className="flex h-full flex-col items-center gap-2 p-4 text-center transition-colors hover:border-accent">
                <q.icon className="h-6 w-6 text-accent" aria-hidden />
                <span className="text-sm font-medium">{q.label}</span>
              </Card>
            </Link>
          ))}
        </div>
      </section>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1.4fr_1fr]">
        <div className="flex flex-col gap-6">
          {/* 5. Skill mastery */}
          <section className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Skill mastery</h2>
              <Link href="/progress" className="text-xs font-medium text-accent hover:underline">
                Full breakdown →
              </Link>
            </div>
            <Card>
              <CardContent className="flex flex-col gap-3 py-4">
                {data.mastery.map((m) => (
                  <div key={m.topic} className="flex items-center gap-3">
                    <span className="w-36 shrink-0 truncate text-sm text-fg">{m.topic}</span>
                    <ProgressBar
                      value={m.masteryPct}
                      tone={m.masteryPct >= 70 ? "success" : m.masteryPct >= 40 ? "warning" : "danger"}
                      className="flex-1"
                    />
                    <span className="w-10 shrink-0 text-right text-xs text-fg-muted">{m.masteryPct}%</span>
                  </div>
                ))}
              </CardContent>
            </Card>
          </section>

          {/* 7. Recommended problems */}
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">Recommended for you</h2>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {data.recommended.map((p) => (
                <ProblemCard key={p.slug} problem={p} meta={data.recommendedMeta[p.slug]} />
              ))}
            </div>
          </section>

          {/* 8. Dataset spotlight */}
          <section className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Dataset spotlight</h2>
              <Link href="/datasets" className="text-xs font-medium text-accent hover:underline">
                Browse all →
              </Link>
            </div>
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              {data.datasets.slice(0, 4).map((d) => (
                <Link key={d.slug} href={`/datasets/${d.slug}`}>
                  <Card className="flex h-full flex-col gap-2 p-4 transition-colors hover:border-accent">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold">{d.name}</span>
                      <Badge tone="neutral">{d.category}</Badge>
                    </div>
                    <p className="text-xs text-fg-muted">{d.description}</p>
                    <div className="mt-auto flex flex-wrap gap-1.5 pt-1">
                      {d.engines.map((e) => (
                        <Badge key={e} tone="info">
                          {e}
                        </Badge>
                      ))}
                    </div>
                  </Card>
                </Link>
              ))}
            </div>
          </section>
        </div>

        <div className="flex flex-col gap-6">
          {/* 6. Recent activity */}
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">Recent activity</h2>
            <Card>
              <CardContent className="flex flex-col divide-y divide-border py-0">
                {data.activity.length === 0 ? (
                  <p className="py-4 text-sm text-fg-muted">
                    Nothing yet - solve your first problem to start your activity feed.
                  </p>
                ) : (
                  data.activity.map((a) => <ActivityRow key={a.id} item={a} />)
                )}
              </CardContent>
            </Card>
          </section>

          {/* 9. Leaderboard preview */}
          <section className="flex flex-col gap-3">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Leaderboard</h2>
              <Link href="/leaderboard" className="text-xs font-medium text-accent hover:underline">
                Full leaderboard →
              </Link>
            </div>
            <Card>
              <CardContent className="flex flex-col divide-y divide-border py-0">
                {nearby.map((e) => (
                  <div key={e.userId} className={`flex items-center gap-3 py-2 text-sm ${e.isCurrentUser ? "bg-accent/10 -mx-4 px-4" : ""}`}>
                    <span className="w-6 shrink-0 text-fg-muted">#{e.rank}</span>
                    <span className="flex-1 truncate font-medium">{e.isCurrentUser ? "You" : e.displayName}</span>
                    <span className="text-fg-muted">{e.xp.toLocaleString()} XP</span>
                  </div>
                ))}
              </CardContent>
            </Card>
          </section>

          {/* 10. Achievements / streak */}
          <section className="flex flex-col gap-3">
            <h2 className="text-lg font-semibold">Achievements</h2>
            <Card>
              <CardContent className="flex flex-col gap-3 py-4">
                <div className="flex flex-wrap gap-2">
                  {earnedBadges.map((b) => (
                    <Badge key={b.id} tone="accent">
                      {b.name}
                    </Badge>
                  ))}
                </div>
                {nextBadge && (
                  <div className="rounded-md border border-dashed border-border p-3 text-xs text-fg-muted">
                    Next up: <span className="font-medium text-fg">{nextBadge.name}</span> - {nextBadge.description}
                  </div>
                )}
                <Link href="/profile" className="flex items-center gap-1 text-xs font-medium text-accent hover:underline">
                  View all achievements <ChevronRight className="h-3 w-3" aria-hidden />
                </Link>
              </CardContent>
            </Card>
          </section>
        </div>
      </div>
    </div>
  );
}

function ActivityRow({ item }: { item: ActivityItem }) {
  const content = (
    <div className="flex items-start gap-3 py-2.5">
      <span
        className={`mt-0.5 h-2 w-2 shrink-0 rounded-full ${
          item.type === "solved" ? "bg-success" : item.type === "attempted" ? "bg-warning" : item.type === "badge" ? "bg-accent" : "bg-info"
        }`}
        aria-hidden
      />
      <div className="min-w-0 flex-1">
        <p className="truncate text-sm text-fg">{item.title}</p>
        {item.detail && <p className="truncate text-xs text-fg-muted">{item.detail}</p>}
      </div>
      <span className="shrink-0 text-xs text-fg-muted">{timeAgo(item.timestamp)}</span>
    </div>
  );
  return item.href ? (
    <Link href={item.href} className="hover:bg-bg/50 -mx-4 px-4">
      {content}
    </Link>
  ) : (
    content
  );
}

function timeAgo(iso: string): string {
  const ms = Date.now() - new Date(iso).getTime();
  const hours = Math.floor(ms / 3600000);
  if (hours < 1) return "just now";
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function DashboardSkeleton() {
  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-8 px-6 py-8">
      <div className="flex flex-col gap-3">
        <Skeleton className="h-8 w-72" />
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-16 rounded-lg" />
          ))}
        </div>
      </div>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Skeleton className="h-40 rounded-lg" />
        <Skeleton className="h-40 rounded-lg" />
      </div>
      <Skeleton className="h-32 rounded-lg" />
      <Skeleton className="h-64 rounded-lg" />
    </div>
  );
}
