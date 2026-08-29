"use client";

import { Avatar, Skeleton, Tabs } from "@DBArena/ui";
import { Flame, Trophy } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuthStore } from "@/lib/auth/authStore";
import { leaderboardRepository } from "@/lib/mock/repositories";
import type { LeaderboardEntry, LeaderboardScope } from "@/lib/mock/types";

const SCOPES: { value: LeaderboardScope; label: string }[] = [
  { value: "global", label: "Global" },
  { value: "weekly", label: "Weekly" },
  { value: "monthly", label: "Monthly" },
  { value: "POSTGRES", label: "PostgreSQL" },
  { value: "MYSQL", label: "MySQL" },
  { value: "MONGODB", label: "MongoDB" },
];

const MEDAL = ["text-warning", "text-fg-muted", "text-[#cd7f32]"];

export default function LeaderboardPage() {
  const user = useAuthStore((s) => s.user);
  const [scope, setScope] = useState<LeaderboardScope>("global");
  const [entries, setEntries] = useState<LeaderboardEntry[] | null>(null);

  useEffect(() => {
    // Deliberately doesn't reset `entries` to null first (stale-while-revalidate
    // instead of a loading flash on every tab click) - also sidesteps calling
    // setState synchronously in the effect body, which react-hooks/set-state-in-effect
    // flags regardless of UX intent.
    let cancelled = false;
    leaderboardRepository.getLeaderboard(scope, user?.displayName ?? "You").then((result) => {
      if (!cancelled) setEntries(result);
    });
    return () => {
      cancelled = true;
    };
  }, [scope, user?.displayName]);

  const me = entries?.find((e) => e.isCurrentUser);

  return (
    <div className="mx-auto flex max-w-4xl flex-col gap-6 px-6 py-8">
      <div>
        <h1 className="text-2xl font-semibold">Leaderboard</h1>
        <p className="text-sm text-fg-muted">See how you stack up - overall, this week, this month, or per engine.</p>
      </div>

      <Tabs items={SCOPES.map((s) => ({ value: s.value, label: s.label }))} value={scope} onChange={(v) => setScope(v as LeaderboardScope)} />

      {me && (
        <div className="flex items-center gap-3 rounded-lg border border-accent bg-accent/10 p-3">
          <Trophy className="h-5 w-5 text-accent" aria-hidden />
          <span className="text-sm">
            You&apos;re ranked <span className="font-semibold">#{me.rank}</span> with{" "}
            <span className="font-semibold">{me.xp.toLocaleString()} XP</span>.
          </span>
        </div>
      )}

      {!entries ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 10 }).map((_, i) => (
            <Skeleton key={i} className="h-14 rounded-lg" />
          ))}
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-border">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border bg-bg-elevated text-xs text-fg-muted">
                <th className="px-4 py-2 font-medium">Rank</th>
                <th className="px-4 py-2 font-medium">User</th>
                <th className="px-4 py-2 font-medium">XP</th>
                <th className="px-4 py-2 font-medium">Solved</th>
                <th className="px-4 py-2 font-medium">Streak</th>
              </tr>
            </thead>
            <tbody>
              {entries.map((e) => (
                <tr
                  key={e.userId}
                  className={`border-b border-border last:border-0 ${e.isCurrentUser ? "bg-accent/10" : ""}`}
                >
                  <td className="px-4 py-2 font-mono">
                    {e.rank <= 3 ? <Trophy className={`h-4 w-4 ${MEDAL[e.rank - 1]}`} aria-label={`#${e.rank}`} /> : `#${e.rank}`}
                  </td>
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-2">
                      <Avatar name={e.displayName} size="sm" />
                      <span className="font-medium">{e.isCurrentUser ? "You" : e.displayName}</span>
                    </div>
                  </td>
                  <td className="px-4 py-2">{e.xp.toLocaleString()}</td>
                  <td className="px-4 py-2">{e.solved}</td>
                  <td className="px-4 py-2">
                    <span className="flex items-center gap-1">
                      <Flame className="h-3.5 w-3.5 text-warning" aria-hidden />
                      {e.streak}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
