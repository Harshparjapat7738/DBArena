// Mock-domain types for everything the real backend doesn't serve yet
// (progress, XP/streaks, badges, datasets, learning paths, playground
// execution, submissions, leaderboard). Modeled to sit *next to*
// `@DBArena/api-client`'s real types (Difficulty/EngineKind are re-exported
// from there, not redeclared) so a real problem and a fixture problem are
// interchangeable wherever a `ProblemSummary` is expected.
//
// Every repository in `./repositories.ts` returns these shapes from an
// `async` function - that's deliberate, not decorative: it's the seam a
// future session replaces with a real `fetch` one function at a time,
// without touching any component that calls the repository.

import type { Difficulty, EngineKind, ProblemSummary } from "@DBArena/api-client";

export type { Difficulty, EngineKind };

export type ProblemStatus = "solved" | "attempted" | "not-started";

/** Everything about a problem the real catalog API doesn't carry yet (estimated time, completion rate, dataset link, company tags). Keyed by slug so it applies to real and fixture problems alike. */
export interface ProblemMeta {
  slug: string;
  datasetSlug: string;
  estimatedMinutes: number;
  completionRatePct: number;
  companyTags: string[];
  topics: string[];
  attempts: number;
}

/** A `ProblemSummary`-shaped fixture problem - assignable anywhere a real one is, until the backend catalog grows enough to replace it. */
export type FixtureProblem = ProblemSummary;

export interface XpProgress {
  xp: number;
  level: number;
  xpIntoLevel: number;
  xpForNextLevel: number;
  rank: string;
}

export interface StreakInfo {
  current: number;
  longest: number;
  lastActiveDate: string;
  freezesAvailable: number;
}

export interface SkillMastery {
  topic: string;
  masteryPct: number;
  problemsSolved: number;
  problemsTotal: number;
}

export type BadgeTier = "bronze" | "silver" | "gold";

export interface BadgeDef {
  id: string;
  name: string;
  description: string;
  icon: string;
  tier: BadgeTier;
  earned: boolean;
  earnedAt?: string;
}

export type ActivityType = "solved" | "attempted" | "lesson" | "badge" | "streak";

export interface ActivityItem {
  id: string;
  type: ActivityType;
  title: string;
  detail?: string;
  timestamp: string;
  engine?: EngineKind;
  difficulty?: Difficulty;
  href?: string;
}

export interface DailyChallenge {
  date: string;
  problemSlug: string;
  title: string;
  difficulty: Difficulty;
  engine: EngineKind;
  topic: string;
  estimatedMinutes: number;
  xpReward: number;
  completed: boolean;
}

export interface LeaderboardEntry {
  rank: number;
  userId: string;
  displayName: string;
  xp: number;
  solved: number;
  streak: number;
  isCurrentUser?: boolean;
}

export type LeaderboardScope = "global" | "weekly" | "monthly" | EngineKind;

export interface DatasetColumn {
  name: string;
  type: string;
  nullable: boolean;
  primaryKey?: boolean;
  foreignKey?: string;
}

export interface DatasetEntity {
  name: string;
  kind: "table" | "collection";
  columns: DatasetColumn[];
  sampleRows: Record<string, string | number | boolean | null>[];
  relationships: { toEntity: string; type: string }[];
}

export interface Dataset {
  slug: string;
  name: string;
  description: string;
  category: string;
  engines: EngineKind[];
  entities: DatasetEntity[];
  rowCountLabel: string;
  problemCount: number;
}

export type LearningLevel = "Beginner" | "Intermediate" | "Advanced";

export interface Lesson {
  slug: string;
  title: string;
  summary: string;
  durationMinutes: number;
  completed: boolean;
  practiceProblemSlug?: string;
}

export interface LearningPath {
  slug: string;
  title: string;
  description: string;
  level: LearningLevel;
  engine: EngineKind | "SQL";
  lessons: Lesson[];
  estimatedHours: number;
}

export interface QueryHistoryEntry {
  id: string;
  query: string;
  engine: EngineKind;
  datasetSlug: string;
  ranAt: string;
  status: "success" | "error";
  rowCount?: number;
  executionMs?: number;
}

export interface MockQueryResult {
  columns: string[];
  rows: (string | number | boolean | null)[][];
  rowCount: number;
  executionMs: number;
  warnings: string[];
  error: string | null;
  explainPlan: string;
}

export type SubmissionVerdict = "ACCEPTED" | "WRONG_ANSWER" | "RUNTIME_ERROR";

export interface Submission {
  id: string;
  problemSlug: string;
  problemTitle: string;
  engine: EngineKind;
  status: SubmissionVerdict;
  query: string;
  executionMs: number;
  rowsReturned: number;
  testsPassed: number;
  testsTotal: number;
  score: number;
  submittedAt: string;
  planCost: number;
  rowsExamined: number;
  message?: string;
}

export interface Bookmark {
  problemSlug: string;
  title: string;
  difficulty: Difficulty;
  bookmarkedAt: string;
}

export interface AiMessage {
  id: string;
  role: "user" | "assistant";
  action: AiAction;
  content: string;
  timestamp: string;
}

export type AiAction = "hint" | "guide" | "explain" | "debug" | "solution" | "optimize";
