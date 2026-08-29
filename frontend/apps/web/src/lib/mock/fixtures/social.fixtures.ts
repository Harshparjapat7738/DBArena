import type { BadgeDef, LeaderboardEntry } from "../types";

const NAMES = [
  "Ava Chen", "Marcus Lee", "Priya Nair", "Jonah Blake", "Lin Zhao", "Sofia Reyes",
  "Kai Nakamura", "Deja Fields", "Tobias Vance", "Amara Osei", "Noor Haddad", "Felix Brandt",
  "Ines Duarte", "Ravi Kapoor", "Elin Solberg", "Malik Johnson", "Yara Haddad", "Owen Fitzgerald",
  "Chidi Okafor", "Greta Lindqvist",
];

export function buildLeaderboard(currentUserName: string, currentUserRank = 47): LeaderboardEntry[] {
  const entries: LeaderboardEntry[] = NAMES.map((name, i) => ({
    rank: i + 1,
    userId: `u-${i + 1}`,
    displayName: name,
    xp: 18400 - i * 620 + (i % 3 === 0 ? 90 : 0),
    solved: 210 - i * 7,
    streak: Math.max(3, 40 - i * 2),
  }));

  // Splice the current user in around their real rank so "nearby ranking" has someone to show.
  const insertAt = Math.min(currentUserRank - 1, entries.length);
  entries.splice(insertAt, 0, {
    rank: currentUserRank,
    userId: "me",
    displayName: currentUserName,
    xp: 4120,
    solved: 58,
    streak: 6,
    isCurrentUser: true,
  });

  return entries.map((e, i) => ({ ...e, rank: i + 1 }));
}

export const BADGES: BadgeDef[] = [
  { id: "first-blood", name: "First Blood", description: "Solved your first problem.", icon: "Sparkles", tier: "bronze", earned: true, earnedAt: "2026-01-12" },
  { id: "streak-7", name: "Week Warrior", description: "Kept a 7-day streak.", icon: "Flame", tier: "bronze", earned: true, earnedAt: "2026-02-03" },
  { id: "streak-30", name: "Iron Streak", description: "Kept a 30-day streak.", icon: "Flame", tier: "gold", earned: false },
  { id: "join-master", name: "Join Master", description: "Solved 15 JOIN problems.", icon: "GitMerge", tier: "silver", earned: true, earnedAt: "2026-04-18" },
  { id: "window-whisperer", name: "Window Whisperer", description: "Solved 10 window-function problems.", icon: "LayoutPanelTop", tier: "silver", earned: false },
  { id: "mongo-explorer", name: "Mongo Explorer", description: "Solved 5 MongoDB aggregation problems.", icon: "Boxes", tier: "bronze", earned: false },
  { id: "three-engines", name: "Polyglot", description: "Solved the same problem in all three engines.", icon: "Layers", tier: "gold", earned: false },
  { id: "hundred-club", name: "Century Club", description: "Solved 100 problems total.", icon: "Trophy", tier: "gold", earned: false },
];
