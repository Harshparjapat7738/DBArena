import type { DailyChallenge, LearningPath } from "../types";

export const LEARNING_PATHS: LearningPath[] = [
  {
    slug: "sql-fundamentals",
    title: "SQL Fundamentals",
    description: "SELECT, filtering, sorting, and the basics of relational thinking.",
    level: "Beginner",
    engine: "SQL",
    estimatedHours: 3,
    lessons: [
      { slug: "select-where", title: "SELECT and WHERE", summary: "Filtering rows with predicates.", durationMinutes: 12, completed: true },
      { slug: "sorting-limits", title: "ORDER BY and LIMIT", summary: "Sorting and paging results.", durationMinutes: 10, completed: true, practiceProblemSlug: "top-spenders" },
      { slug: "joins-101", title: "Introduction to JOIN", summary: "Combining rows across tables.", durationMinutes: 18, completed: false, practiceProblemSlug: "customers-without-orders" },
      { slug: "aggregation-101", title: "GROUP BY and Aggregates", summary: "COUNT, SUM, AVG and grouping.", durationMinutes: 15, completed: false, practiceProblemSlug: "department-headcount" },
    ],
  },
  {
    slug: "advanced-sql",
    title: "Advanced SQL",
    description: "Window functions, CTEs, and recursive queries.",
    level: "Intermediate",
    engine: "SQL",
    estimatedHours: 5,
    lessons: [
      { slug: "window-functions", title: "Window Functions", summary: "RANK, ROW_NUMBER, running totals.", durationMinutes: 22, completed: false, practiceProblemSlug: "running-order-total" },
      { slug: "ctes", title: "Common Table Expressions", summary: "Readable, composable subqueries.", durationMinutes: 18, completed: false, practiceProblemSlug: "employees-above-avg" },
      { slug: "recursive-ctes", title: "Recursive CTEs", summary: "Traversing hierarchies in SQL.", durationMinutes: 25, completed: false, practiceProblemSlug: "recursive-org-chart" },
    ],
  },
  {
    slug: "postgresql-deep-dive",
    title: "PostgreSQL",
    description: "Engine-specific features: JSONB, arrays, and Postgres-flavored window functions.",
    level: "Intermediate",
    engine: "POSTGRES",
    estimatedHours: 4,
    lessons: [
      { slug: "postgres-types", title: "Postgres-specific Types", summary: "JSONB, arrays, ranges.", durationMinutes: 16, completed: false },
      { slug: "postgres-explain", title: "Reading EXPLAIN ANALYZE", summary: "Understanding the query planner.", durationMinutes: 20, completed: false },
    ],
  },
  {
    slug: "mongodb-essentials",
    title: "MongoDB",
    description: "Documents, the aggregation pipeline, and modeling without joins.",
    level: "Intermediate",
    engine: "MONGODB",
    estimatedHours: 4,
    lessons: [
      { slug: "documents-and-queries", title: "Documents and find()", summary: "Querying a document collection.", durationMinutes: 14, completed: false },
      { slug: "aggregation-pipeline", title: "The Aggregation Pipeline", summary: "$match, $group, $project.", durationMinutes: 22, completed: false, practiceProblemSlug: "monthly-active-users" },
      { slug: "lookup-and-modeling", title: "$lookup and Data Modeling", summary: "Embedding vs. referencing.", durationMinutes: 20, completed: false, practiceProblemSlug: "product-recommendation-pipeline" },
    ],
  },
  {
    slug: "query-optimization",
    title: "Query Optimization",
    description: "Indexes, execution plans, and rewriting slow queries.",
    level: "Advanced",
    engine: "SQL",
    estimatedHours: 3,
    lessons: [
      { slug: "indexes-101", title: "How Indexes Work", summary: "B-trees, selectivity, covering indexes.", durationMinutes: 18, completed: false },
      { slug: "rewriting-slow-queries", title: "Rewriting Slow Queries", summary: "Spotting and fixing common anti-patterns.", durationMinutes: 24, completed: false, practiceProblemSlug: "slow-query-rewrite" },
    ],
  },
  {
    slug: "database-design",
    title: "Database Design",
    description: "Normalization, star schemas, and modeling for the right engine.",
    level: "Advanced",
    engine: "SQL",
    estimatedHours: 3,
    lessons: [
      { slug: "normalization", title: "Normalization", summary: "1NF through 3NF, and when to denormalize.", durationMinutes: 20, completed: false },
      { slug: "star-schemas", title: "Star Schemas", summary: "Fact and dimension tables for analytics.", durationMinutes: 18, completed: false, practiceProblemSlug: "star-schema-design" },
    ],
  },
  {
    slug: "interview-preparation",
    title: "Interview Preparation",
    description: "The patterns that show up again and again in database interviews.",
    level: "Intermediate",
    engine: "SQL",
    estimatedHours: 4,
    lessons: [
      { slug: "interview-patterns", title: "Common Interview Patterns", summary: "Top-N per group, running totals, gaps and islands.", durationMinutes: 20, completed: false, practiceProblemSlug: "session-gaps-and-islands" },
      { slug: "explaining-your-query", title: "Explaining Your Approach", summary: "How to talk through a query out loud.", durationMinutes: 12, completed: false },
    ],
  },
];

const today = new Date();
function isoDaysAgo(daysAgo: number): string {
  const d = new Date(today);
  d.setDate(d.getDate() - daysAgo);
  return d.toISOString().slice(0, 10);
}

export const DAILY_CHALLENGE_TODAY: DailyChallenge = {
  date: isoDaysAgo(0),
  problemSlug: "employees-above-avg",
  title: "Employees Paid Above Department Average",
  difficulty: "MEDIUM",
  engine: "POSTGRES",
  topic: "Window Functions",
  estimatedMinutes: 15,
  xpReward: 40,
  completed: false,
};

export const DAILY_CHALLENGE_HISTORY: DailyChallenge[] = [
  { date: isoDaysAgo(1), problemSlug: "running-order-total", title: "Running Total of Daily Orders", difficulty: "MEDIUM", engine: "POSTGRES", topic: "Window Functions", estimatedMinutes: 15, xpReward: 40, completed: true },
  { date: isoDaysAgo(2), problemSlug: "department-headcount", title: "Department Headcount", difficulty: "EASY", engine: "MYSQL", topic: "GROUP BY", estimatedMinutes: 6, xpReward: 20, completed: true },
  { date: isoDaysAgo(3), problemSlug: "customers-without-orders", title: "Customers Without Orders", difficulty: "EASY", engine: "POSTGRES", topic: "JOIN", estimatedMinutes: 10, xpReward: 20, completed: true },
  { date: isoDaysAgo(4), problemSlug: "monthly-active-users", title: "Monthly Active Users", difficulty: "MEDIUM", engine: "MONGODB", topic: "Mongo Aggregation", estimatedMinutes: 14, xpReward: 40, completed: false },
  { date: isoDaysAgo(5), problemSlug: "second-highest-salary", title: "Second Highest Salary", difficulty: "EASY", engine: "POSTGRES", topic: "SELECT", estimatedMinutes: 8, xpReward: 20, completed: true },
  { date: isoDaysAgo(6), problemSlug: "top-spenders", title: "Top Spenders This Month", difficulty: "EASY", engine: "MYSQL", topic: "SELECT", estimatedMinutes: 8, xpReward: 20, completed: true },
  { date: isoDaysAgo(7), problemSlug: "fraud-flagged-transactions", title: "Flag Suspicious Transactions", difficulty: "MEDIUM", engine: "POSTGRES", topic: "CTE", estimatedMinutes: 18, xpReward: 40, completed: false },
];
