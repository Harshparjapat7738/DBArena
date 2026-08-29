"use client";

import { Badge } from "@DBArena/ui";
import { Flame, Zap } from "lucide-react";
import Link from "next/link";
import { useEffect, useState } from "react";
import { progressRepository } from "@/lib/mock/repositories";
import type { StreakInfo, XpProgress } from "@/lib/mock/types";

/**
 * The always-visible streak + XP glance every page's top bar shows -
 * mirrors the "streak flame" pattern from Duolingo/LeetCode: cheap to
 * check, expensive to lose, so it stays in view everywhere, not just on
 * the dashboard. Reads from the mock progress repository (localStorage-
 * backed) - swapping to a real endpoint later is a one-line change here.
 */
export function HeaderStatus() {
  const [xp, setXp] = useState<XpProgress | null>(null);
  const [streak, setStreak] = useState<StreakInfo | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([progressRepository.getXp(), progressRepository.getStreak()]).then(([x, s]) => {
      if (!cancelled) {
        setXp(x);
        setStreak(s);
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!xp || !streak) return <div className="h-7 w-32 animate-pulse rounded-full bg-bg-elevated" />;

  return (
    <Link href="/progress" className="flex items-center gap-2">
      <Badge tone={streak.current > 0 ? "warning" : "neutral"} className="gap-1">
        <Flame className="h-3.5 w-3.5" aria-hidden />
        {streak.current} day{streak.current === 1 ? "" : "s"}
      </Badge>
      <Badge tone="accent" className="gap-1">
        <Zap className="h-3.5 w-3.5" aria-hidden />
        {xp.rank} · {xp.xp.toLocaleString()} XP
      </Badge>
    </Link>
  );
}
