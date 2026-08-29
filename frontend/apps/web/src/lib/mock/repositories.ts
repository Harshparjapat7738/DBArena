// Mock repositories: one `async` function group per domain, each returning
// the shapes in `./types.ts`. This is the seam a future session swaps for
// real HTTP calls (matching `@DBArena/api-client`'s own `createXEndpoints`
// pattern) one repository at a time, without any page/component needing to
// change - every caller already awaits a Promise and never reaches into
// fixtures or localStorage directly.
//
// State that should feel "lived in" across a session (XP, bookmarks,
// submission history, lesson/daily-challenge completion) is seeded from
// the fixtures below on first use and then persisted to `localStorage` via
// `./localStore` - so solving a mock problem, bookmarking one, or running
// a playground query actually sticks around on reload, the way a real
// backend would, without one existing.

import type { Difficulty, EngineKind, ProblemSummary } from "@DBArena/api-client";
import { readJson, writeJson } from "./localStore";
import { DATASETS, MOCK_PROBLEMS, MOCK_STATEMENTS, PROBLEM_META, SKILL_MASTERY } from "./fixtures/catalog.fixtures";
import { DAILY_CHALLENGE_HISTORY, DAILY_CHALLENGE_TODAY, LEARNING_PATHS } from "./fixtures/learning.fixtures";
import { BADGES, buildLeaderboard } from "./fixtures/social.fixtures";
import type {
  ActivityItem,
  AiAction,
  BadgeDef,
  Bookmark,
  Dataset,
  DailyChallenge,
  LeaderboardEntry,
  LeaderboardScope,
  LearningPath,
  MockQueryResult,
  ProblemMeta,
  ProblemStatus,
  QueryHistoryEntry,
  SkillMastery,
  StreakInfo,
  Submission,
  SubmissionVerdict,
  XpProgress,
} from "./types";

function delay<T>(value: T, ms = 120): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10);
}

function uid(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

// ---------------------------------------------------------------------------
// Progress: XP, streak, mastery, badges, activity feed.
// ---------------------------------------------------------------------------

const STREAK_SEED: StreakInfo = { current: 6, longest: 14, lastActiveDate: todayIso(), freezesAvailable: 1 };

function levelForXp(xp: number): XpProgress {
  const xpForNextLevel = 500;
  const level = Math.floor(xp / xpForNextLevel) + 1;
  const xpIntoLevel = xp % xpForNextLevel;
  const rankTiers = ["Bronze", "Silver", "Gold", "Platinum", "Diamond"];
  const romanNumerals = ["I", "II", "III", "IV", "V"];
  const tier = rankTiers[Math.min(rankTiers.length - 1, Math.floor(level / 5))];
  const sub = romanNumerals[(level - 1) % 5];
  return { xp, level, xpIntoLevel, xpForNextLevel, rank: `${tier} ${sub}` };
}

const XP_SEED: XpProgress = levelForXp(4120);

const ACTIVITY_SEED: ActivityItem[] = [
  { id: "a1", type: "solved", title: "Solved “Two Sum (Numbers)”", detail: "Accepted · 3ms", timestamp: hoursAgoIso(3), engine: "POSTGRES", difficulty: "EASY", href: "/practice/two-sum" },
  { id: "a2", type: "streak", title: "6-day streak", detail: "Keep it going today", timestamp: hoursAgoIso(3) },
  { id: "a3", type: "attempted", title: "Attempted “Running Total of Daily Orders”", detail: "Wrong answer · row order mismatch", timestamp: hoursAgoIso(26), engine: "POSTGRES", difficulty: "MEDIUM", href: "/practice/running-order-total" },
  { id: "a4", type: "lesson", title: "Completed “ORDER BY and LIMIT”", detail: "SQL Fundamentals", timestamp: hoursAgoIso(30), href: "/learning/sql-fundamentals" },
  { id: "a5", type: "solved", title: "Solved “Department Headcount”", detail: "Accepted · 5ms", timestamp: hoursAgoIso(50), engine: "MYSQL", difficulty: "EASY", href: "/practice/department-headcount" },
  { id: "a6", type: "badge", title: "Earned badge “Join Master”", detail: "15 JOIN problems solved", timestamp: hoursAgoIso(96) },
];

function hoursAgoIso(hours: number): string {
  return new Date(Date.now() - hours * 3600 * 1000).toISOString();
}

export const progressRepository = {
  async getXp(): Promise<XpProgress> {
    return delay(readJson("xp", XP_SEED));
  },
  async getStreak(): Promise<StreakInfo> {
    return delay(readJson("streak", STREAK_SEED));
  },
  async getMastery(): Promise<SkillMastery[]> {
    return delay(SKILL_MASTERY);
  },
  async getBadges(): Promise<BadgeDef[]> {
    return delay(readJson("badges", BADGES));
  },
  async getActivity(limit = 20): Promise<ActivityItem[]> {
    const items = readJson("activity", ACTIVITY_SEED);
    return delay(items.slice(0, limit));
  },
  /** Awards XP and appends an activity entry - the one mutation every "complete a thing" flow (solve a problem, finish a lesson, complete the daily challenge) funnels through. */
  async awardXp(amount: number, activity: Omit<ActivityItem, "id" | "timestamp">): Promise<XpProgress> {
    const current = readJson("xp", XP_SEED);
    const next = levelForXp(current.xp + amount);
    writeJson("xp", next);

    const activityLog = readJson("activity", ACTIVITY_SEED);
    activityLog.unshift({ ...activity, id: uid("act"), timestamp: new Date().toISOString() });
    writeJson("activity", activityLog.slice(0, 50));

    const streak = readJson("streak", STREAK_SEED);
    const today = todayIso();
    if (streak.lastActiveDate !== today) {
      const next2: StreakInfo = { ...streak, current: streak.current + 1, longest: Math.max(streak.longest, streak.current + 1), lastActiveDate: today };
      writeJson("streak", next2);
    }
    return delay(next);
  },
};

// ---------------------------------------------------------------------------
// Problems: fixture problems + per-slug meta + local solved/attempted status.
// ---------------------------------------------------------------------------

const STATUS_SEED: Record<string, ProblemStatus> = {
  "two-sum": "solved",
  "top-spenders": "solved",
  "second-highest-salary": "solved",
  "department-headcount": "solved",
  "customers-without-orders": "attempted",
  "running-order-total": "attempted",
  "orders-and-items": "solved",
  "monthly-active-users": "attempted",
};

export interface MockProblemFilter {
  q?: string;
  difficulty?: Difficulty;
  engine?: EngineKind;
  tag?: string;
  status?: ProblemStatus;
}

export const problemsRepository = {
  /** Fixture problems only (real ones come from `catalogApi` server-side, per the existing Practice page) - filtered client-side the same way the real query params are. */
  async listMockProblems(filter: MockProblemFilter = {}): Promise<ProblemSummary[]> {
    const statuses = readJson("problemStatus", STATUS_SEED);
    let items = MOCK_PROBLEMS;
    if (filter.q) {
      const q = filter.q.toLowerCase();
      items = items.filter((p) => p.title.toLowerCase().includes(q) || p.tags.some((t) => t.includes(q)));
    }
    if (filter.difficulty) items = items.filter((p) => p.difficulty === filter.difficulty);
    if (filter.engine) items = items.filter((p) => p.allowedEngines.includes(filter.engine!));
    if (filter.tag) items = items.filter((p) => p.tags.includes(filter.tag!));
    if (filter.status) items = items.filter((p) => (statuses[p.slug] ?? "not-started") === filter.status);
    return delay(items);
  },
  async getMeta(slug: string): Promise<ProblemMeta | null> {
    return delay(PROBLEM_META[slug] ?? null);
  },
  async getAllMeta(): Promise<Record<string, ProblemMeta>> {
    return delay(PROBLEM_META);
  },
  async getMockProblem(slug: string): Promise<ProblemSummary | null> {
    return delay(MOCK_PROBLEMS.find((p) => p.slug === slug) ?? null);
  },
  async getStatus(slug: string): Promise<ProblemStatus> {
    const statuses = readJson("problemStatus", STATUS_SEED);
    return delay(statuses[slug] ?? "not-started");
  },
  async getAllStatuses(): Promise<Record<string, ProblemStatus>> {
    return delay(readJson("problemStatus", STATUS_SEED));
  },
  async setStatus(slug: string, status: ProblemStatus): Promise<void> {
    const statuses = readJson("problemStatus", STATUS_SEED);
    // Never downgrade solved -> attempted from a later, worse attempt.
    if (statuses[slug] === "solved" && status === "attempted") return;
    statuses[slug] = status;
    writeJson("problemStatus", statuses);
  },
  /** All known problem+meta topic tags, for filter dropdowns. */
  async listTopics(): Promise<string[]> {
    const set = new Set<string>();
    for (const m of Object.values(PROBLEM_META)) for (const t of m.topics) set.add(t);
    return delay(Array.from(set).sort());
  },
  /** Not-yet-solved problems, weighted toward the learner's weakest mastered topics - the Dashboard's "Recommended for you" rail. */
  async getRecommended(limit = 4): Promise<ProblemSummary[]> {
    const statuses = readJson("problemStatus", STATUS_SEED);
    const weakTopics = [...SKILL_MASTERY]
      .sort((a, b) => a.masteryPct - b.masteryPct)
      .map((m) => m.topic.toLowerCase().replace(/\s+/g, "-"));

    const unsolved = MOCK_PROBLEMS.filter((p) => (statuses[p.slug] ?? "not-started") !== "solved");
    const scored = unsolved.map((p) => {
      const topics = PROBLEM_META[p.slug]?.topics ?? [];
      const weakestIndex = topics.reduce((best, t) => {
        const i = weakTopics.indexOf(t);
        return i === -1 ? best : Math.min(best, i);
      }, weakTopics.length);
      return { problem: p, weakestIndex };
    });
    scored.sort((a, b) => a.weakestIndex - b.weakestIndex);
    return delay(scored.slice(0, limit).map((s) => s.problem));
  },
  async getMockStatement(slug: string): Promise<string | null> {
    return delay(MOCK_STATEMENTS[slug] ?? null);
  },
  /** Other problems sharing a topic or dataset with `slug` - used for the problem detail page's "Related problems" rail. */
  async getRelated(slug: string, limit = 4): Promise<ProblemSummary[]> {
    const meta = PROBLEM_META[slug];
    if (!meta) return delay([]);
    const scored = MOCK_PROBLEMS.filter((p) => p.slug !== slug).map((p) => {
      const pMeta = PROBLEM_META[p.slug];
      let score = 0;
      if (pMeta?.datasetSlug === meta.datasetSlug) score += 2;
      if (pMeta) score += pMeta.topics.filter((t) => meta.topics.includes(t)).length;
      return { problem: p, score };
    });
    scored.sort((a, b) => b.score - a.score);
    return delay(scored.filter((s) => s.score > 0).slice(0, limit).map((s) => s.problem));
  },
  /** Problems authored against a given dataset - the dataset detail page's "Practice with this dataset" rail. */
  async listByDataset(datasetSlug: string): Promise<ProblemSummary[]> {
    const slugs = new Set(Object.values(PROBLEM_META).filter((m) => m.datasetSlug === datasetSlug).map((m) => m.slug));
    return delay(MOCK_PROBLEMS.filter((p) => slugs.has(p.slug)));
  },
};

// ---------------------------------------------------------------------------
// Datasets
// ---------------------------------------------------------------------------

export const datasetsRepository = {
  async listDatasets(): Promise<Dataset[]> {
    return delay(DATASETS);
  },
  async getDataset(slug: string): Promise<Dataset | null> {
    return delay(DATASETS.find((d) => d.slug === slug) ?? null);
  },
};

// ---------------------------------------------------------------------------
// Learning
// ---------------------------------------------------------------------------

export const learningRepository = {
  async listPaths(): Promise<LearningPath[]> {
    const overrides = readJson<Record<string, string[]>>("lessonProgress", {});
    return delay(
      LEARNING_PATHS.map((path) => ({
        ...path,
        lessons: path.lessons.map((lesson) => ({
          ...lesson,
          completed: lesson.completed || (overrides[path.slug]?.includes(lesson.slug) ?? false),
        })),
      })),
    );
  },
  async getPath(slug: string): Promise<LearningPath | null> {
    const paths = await learningRepository.listPaths();
    return paths.find((p) => p.slug === slug) ?? null;
  },
  async completeLesson(pathSlug: string, lessonSlug: string): Promise<void> {
    const overrides = readJson<Record<string, string[]>>("lessonProgress", {});
    const set = new Set(overrides[pathSlug] ?? []);
    set.add(lessonSlug);
    overrides[pathSlug] = Array.from(set);
    writeJson("lessonProgress", overrides);
    await progressRepository.awardXp(15, { type: "lesson", title: `Completed a lesson in ${pathSlug}` });
  },
};

// ---------------------------------------------------------------------------
// Daily challenge
// ---------------------------------------------------------------------------

export const dailyChallengeRepository = {
  async getToday(): Promise<DailyChallenge> {
    const completedToday = readJson<boolean>("dailyCompleted:" + DAILY_CHALLENGE_TODAY.date, false);
    return delay({ ...DAILY_CHALLENGE_TODAY, completed: completedToday });
  },
  async getHistory(): Promise<DailyChallenge[]> {
    return delay(DAILY_CHALLENGE_HISTORY);
  },
  async completeToday(): Promise<void> {
    writeJson("dailyCompleted:" + DAILY_CHALLENGE_TODAY.date, true);
    await progressRepository.awardXp(DAILY_CHALLENGE_TODAY.xpReward, {
      type: "solved",
      title: `Completed today's daily challenge`,
      detail: DAILY_CHALLENGE_TODAY.title,
      engine: DAILY_CHALLENGE_TODAY.engine,
      difficulty: DAILY_CHALLENGE_TODAY.difficulty,
      href: `/practice/${DAILY_CHALLENGE_TODAY.problemSlug}`,
    });
  },
};

// ---------------------------------------------------------------------------
// Leaderboard
// ---------------------------------------------------------------------------

export const leaderboardRepository = {
  async getLeaderboard(scope: LeaderboardScope, currentUserName: string): Promise<LeaderboardEntry[]> {
    const base = buildLeaderboard(currentUserName);
    // Scope changes the ordering flavor slightly so each tab doesn't look identical.
    const scoped =
      scope === "weekly" ? [...base].sort((a, b) => b.streak - a.streak)
      : scope === "monthly" ? [...base].sort((a, b) => b.solved - a.solved)
      : base;
    return delay(scoped.map((e, i) => ({ ...e, rank: i + 1 })));
  },
};

// ---------------------------------------------------------------------------
// Bookmarks
// ---------------------------------------------------------------------------

const BOOKMARK_SEED: Bookmark[] = [
  { problemSlug: "recursive-org-chart", title: "Flatten the Org Chart", difficulty: "HARD", bookmarkedAt: hoursAgoIso(70) },
  { problemSlug: "star-schema-design", title: "Design a Star Schema for Sales", difficulty: "MEDIUM", bookmarkedAt: hoursAgoIso(140) },
];

export const bookmarksRepository = {
  async list(): Promise<Bookmark[]> {
    return delay(readJson("bookmarks", BOOKMARK_SEED));
  },
  async isBookmarked(slug: string): Promise<boolean> {
    const list = readJson("bookmarks", BOOKMARK_SEED);
    return delay(list.some((b) => b.problemSlug === slug));
  },
  async toggle(problem: { slug: string; title: string; difficulty: Difficulty }): Promise<boolean> {
    const list = readJson("bookmarks", BOOKMARK_SEED);
    const exists = list.some((b) => b.problemSlug === problem.slug);
    const next = exists
      ? list.filter((b) => b.problemSlug !== problem.slug)
      : [{ problemSlug: problem.slug, title: problem.title, difficulty: problem.difficulty, bookmarkedAt: new Date().toISOString() }, ...list];
    writeJson("bookmarks", next);
    return delay(!exists);
  },
};

// ---------------------------------------------------------------------------
// Submissions
// ---------------------------------------------------------------------------

const SUBMISSION_SEED: Submission[] = [
  { id: "s1", problemSlug: "two-sum", problemTitle: "Two Sum (Numbers)", engine: "POSTGRES", status: "ACCEPTED", query: "SELECT a.id, b.id\nFROM numbers a\nJOIN numbers b ON a.value + b.value = 10\nWHERE a.id < b.id;", executionMs: 3, rowsReturned: 2, testsPassed: 4, testsTotal: 4, score: 100, submittedAt: hoursAgoIso(3), planCost: 12.4, rowsExamined: 16 },
  { id: "s2", problemSlug: "running-order-total", problemTitle: "Running Total of Daily Orders", engine: "POSTGRES", status: "WRONG_ANSWER", query: "SELECT order_date, SUM(total_amount)\nFROM orders\nGROUP BY order_date;", executionMs: 9, rowsReturned: 30, testsPassed: 1, testsTotal: 4, score: 25, submittedAt: hoursAgoIso(26), planCost: 44.1, rowsExamined: 5002, message: "Expected a running total per day, got a per-day total only - missing the window function." },
  { id: "s3", problemSlug: "department-headcount", problemTitle: "Department Headcount", engine: "MYSQL", status: "ACCEPTED", query: "SELECT department_id, COUNT(*) AS headcount\nFROM employees\nGROUP BY department_id;", executionMs: 5, rowsReturned: 3, testsPassed: 3, testsTotal: 3, score: 100, submittedAt: hoursAgoIso(50), planCost: 8.2, rowsExamined: 240 },
  { id: "s4", problemSlug: "top-spenders", problemTitle: "Top Spenders This Month", engine: "MYSQL", status: "RUNTIME_ERROR", query: "SELECT customer_id, SUM(total_amount)\nFROM order\nGROUP BY customer_id;", executionMs: 0, rowsReturned: 0, testsPassed: 0, testsTotal: 3, score: 0, submittedAt: hoursAgoIso(96), planCost: 0, rowsExamined: 0, message: "Table \"order\" does not exist - did you mean \"orders\"?" },
];

export const submissionsRepository = {
  async list(): Promise<Submission[]> {
    return delay(readJson("submissions", SUBMISSION_SEED));
  },
  async get(id: string): Promise<Submission | null> {
    const list = readJson("submissions", SUBMISSION_SEED);
    return delay(list.find((s) => s.id === id) ?? null);
  },
  async listForProblem(slug: string): Promise<Submission[]> {
    const list = readJson("submissions", SUBMISSION_SEED);
    return delay(list.filter((s) => s.problemSlug === slug));
  },
  /** Fakes grading: deterministic-ish verdict from the query's shape, purely for demo purposes - never claims to run anything for real. */
  async submit(input: { problemSlug: string; problemTitle: string; engine: EngineKind; query: string }): Promise<Submission> {
    const q = input.query.trim().toLowerCase();
    let status: SubmissionVerdict = "ACCEPTED";
    let message: string | undefined;
    let testsPassed = 4;
    if (!q.includes("select") && !q.includes("aggregate") && !q.includes("find")) {
      status = "RUNTIME_ERROR";
      message = "Couldn't parse a query - make sure it's a valid statement for the selected engine.";
      testsPassed = 0;
    } else if (q.length < 20) {
      status = "WRONG_ANSWER";
      message = "Output didn't match the expected result set on 2 of 4 hidden tests.";
      testsPassed = 2;
    }

    const submission: Submission = {
      id: uid("sub"),
      problemSlug: input.problemSlug,
      problemTitle: input.problemTitle,
      engine: input.engine,
      status,
      query: input.query,
      executionMs: Math.round(2 + Math.random() * 30),
      rowsReturned: status === "ACCEPTED" ? Math.round(2 + Math.random() * 20) : 0,
      testsPassed,
      testsTotal: 4,
      score: Math.round((testsPassed / 4) * 100),
      submittedAt: new Date().toISOString(),
      planCost: Math.round((10 + Math.random() * 80) * 10) / 10,
      rowsExamined: Math.round(100 + Math.random() * 5000),
      message,
    };

    const list = readJson("submissions", SUBMISSION_SEED);
    list.unshift(submission);
    writeJson("submissions", list.slice(0, 100));

    if (status === "ACCEPTED") {
      await problemsRepository.setStatus(input.problemSlug, "solved");
      await progressRepository.awardXp(30, {
        type: "solved",
        title: `Solved "${input.problemTitle}"`,
        detail: `Accepted · ${submission.executionMs}ms`,
        engine: input.engine,
        href: `/practice/${input.problemSlug}`,
      });
    } else {
      await problemsRepository.setStatus(input.problemSlug, "attempted");
      await progressRepository.awardXp(5, {
        type: "attempted",
        title: `Attempted "${input.problemTitle}"`,
        detail: status === "WRONG_ANSWER" ? "Wrong answer" : "Runtime error",
        engine: input.engine,
        href: `/practice/${input.problemSlug}`,
      });
    }

    return delay(submission, 500);
  },
};

// ---------------------------------------------------------------------------
// Playground: fake query execution + history.
// ---------------------------------------------------------------------------

function fakeResultFor(datasetSlug: string, engine: EngineKind): MockQueryResult {
  // DATASETS is a static, non-empty fixture array - both indexes are safe.
  const dataset = DATASETS.find((d) => d.slug === datasetSlug) ?? DATASETS[0]!;
  const entity = dataset.entities[0]!;
  const columns = entity.columns.map((c) => c.name);
  const rows = entity.sampleRows.map((row) => columns.map((c) => row[c] ?? null));
  return {
    columns,
    rows,
    rowCount: rows.length,
    executionMs: Math.round(2 + Math.random() * 40),
    warnings: engine === "MONGODB" ? [] : [],
    error: null,
    explainPlan:
      engine === "MONGODB"
        ? `{ stage: "COLLSCAN", nReturned: ${rows.length}, executionTimeMillisEstimate: ${Math.round(1 + Math.random() * 5)} }`
        : `Seq Scan on ${entity.name}  (cost=0.00..${(10 + Math.random() * 40).toFixed(2)} rows=${rows.length} width=64)`,
  };
}

export const playgroundRepository = {
  /** Never executes anything - returns a plausible canned result shaped by the selected dataset's first table/collection, with a small artificial delay so Run/Submit feel real. */
  async runQuery(input: { engine: EngineKind; datasetSlug: string; query: string }): Promise<MockQueryResult> {
    const trimmed = input.query.trim();
    if (!trimmed) {
      return delay({ columns: [], rows: [], rowCount: 0, executionMs: 0, warnings: [], error: "Query is empty.", explainPlan: "" }, 150);
    }
    const entry: QueryHistoryEntry = {
      id: uid("qh"),
      query: input.query,
      engine: input.engine,
      datasetSlug: input.datasetSlug,
      ranAt: new Date().toISOString(),
      status: "success",
    };
    const history = readJson<QueryHistoryEntry[]>("queryHistory", []);
    history.unshift(entry);
    writeJson("queryHistory", history.slice(0, 50));
    return delay(fakeResultFor(input.datasetSlug, input.engine), 350 + Math.random() * 350);
  },
  async getHistory(): Promise<QueryHistoryEntry[]> {
    return delay(readJson<QueryHistoryEntry[]>("queryHistory", []));
  },
  async clearHistory(): Promise<void> {
    writeJson("queryHistory", []);
  },
};

// ---------------------------------------------------------------------------
// AI assistant: mock responses for every action except real hints.
// ---------------------------------------------------------------------------

export interface AiContext {
  problemTitle: string;
  schemaSummary?: string;
  userQuery?: string;
  errorOrResult?: string;
}

const AI_ACTION_LABEL: Record<AiAction, string> = {
  hint: "Hint",
  guide: "Guide me",
  explain: "Explain my query",
  debug: "Debug my query",
  solution: "Show solution",
  optimize: "Optimize query",
};

/**
 * Canned, action-shaped responses standing in for a real AI call - never
 * claims to have executed anything, and every response opens by naming
 * exactly what context it "received" (question, schema, sample rows, the
 * learner's own query/error), mirroring the real ai-assistant-service's
 * actual hard-coded context cap (root CLAUDE.md hard rule #5) rather than
 * pretending this mock has unlimited context.
 */
export const aiRepository = {
  actionLabel(action: AiAction): string {
    return AI_ACTION_LABEL[action];
  },
  async respond(action: AiAction, context: AiContext): Promise<string> {
    const contextLine = `*(Context received: the problem statement, a schema summary${
      context.userQuery ? ", your query" : ""
    }${context.errorOrResult ? ", and your last result/error" : ""} - never the reference solution or hidden test data.)*`;

    const responses: Record<AiAction, string> = {
      hint: `Think about which columns you need to compare, and whether a single table is enough or you need to bring in a second one. Re-read "${context.problemTitle}"'s requirements once more before writing anything.`,
      guide: `Start by identifying the entities involved in "${context.problemTitle}" and how they relate. Write the simplest query that returns *something* first, then narrow it down with WHERE/HAVING until the shape matches what's asked.`,
      explain: context.userQuery
        ? `Your query reads roughly as: filter rows, then ${context.userQuery.toLowerCase().includes("group by") ? "group and aggregate them" : "project the selected columns"}. Walk through it clause by clause - which part runs first?`
        : "Paste your query first so I can walk through what it's actually doing, clause by clause.",
      debug: context.errorOrResult
        ? `Given the error/result you shared, check for a typo in a table or column name, or a type mismatch in a comparison - that's the most common cause of this shape of failure.`
        : "Paste your query and the error or unexpected output you got, and I'll help narrow down what's going wrong.",
      solution: `A full solution is intentionally not shown here - on a real submission this would stay hidden until you solve it or explicitly ask to reveal it, to keep the problem meaningful. Try the "Guide me" or "Hint" actions instead.`,
      optimize: `Once your query returns the right rows, look at which columns you're filtering or joining on - those are usually the first candidates for an index. Avoid a function call on the filtered column (e.g. \`LOWER(col) = ...\`) since it can prevent an index from being used.`,
    };

    return delay(`${responses[action]}\n\n${contextLine}`, 500 + Math.random() * 500);
  },
};
