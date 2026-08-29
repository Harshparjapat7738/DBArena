import type { Dataset, FixtureProblem, ProblemMeta, SkillMastery } from "../types";

/**
 * Fixture problems, shaped exactly like the real `ProblemSummary` the
 * backend catalog returns (`@DBArena/api-client`) - interchangeable with
 * real ones anywhere a problem card is rendered. The backend catalog has
 * exactly one published problem today (`two-sum`, the M02 sample dataset);
 * these fill out every topic/engine/difficulty combination the product
 * needs to demo Practice/Dashboard/Learning/Daily Challenge with, and get
 * superseded one-for-one as real problems are authored.
 */
export const MOCK_PROBLEMS: FixtureProblem[] = [
  {
    slug: "top-spenders",
    title: "Top Spenders This Month",
    difficulty: "EASY",
    tags: ["select", "order-by", "aggregation"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "customers-without-orders",
    title: "Customers Without Orders",
    difficulty: "EASY",
    tags: ["join", "outer-join"],
    allowedEngines: ["POSTGRES", "MYSQL", "MONGODB"],
  },
  {
    slug: "second-highest-salary",
    title: "Second Highest Salary",
    difficulty: "EASY",
    tags: ["select", "order-by", "limit-offset"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "department-headcount",
    title: "Department Headcount",
    difficulty: "EASY",
    tags: ["group-by", "aggregation"],
    allowedEngines: ["POSTGRES", "MYSQL", "MONGODB"],
  },
  {
    slug: "running-order-total",
    title: "Running Total of Daily Orders",
    difficulty: "MEDIUM",
    tags: ["window-functions"],
    allowedEngines: ["POSTGRES"],
  },
  {
    slug: "orders-and-items",
    title: "Orders With Their Line Items",
    difficulty: "MEDIUM",
    tags: ["join", "group-by"],
    allowedEngines: ["POSTGRES", "MYSQL", "MONGODB"],
  },
  {
    slug: "employees-above-avg",
    title: "Employees Paid Above Department Average",
    difficulty: "MEDIUM",
    tags: ["window-functions", "cte"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "monthly-active-users",
    title: "Monthly Active Users",
    difficulty: "MEDIUM",
    tags: ["mongo-aggregation", "group-by"],
    allowedEngines: ["MONGODB"],
  },
  {
    slug: "fraud-flagged-transactions",
    title: "Flag Suspicious Transactions",
    difficulty: "MEDIUM",
    tags: ["cte", "join", "aggregation"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "recursive-org-chart",
    title: "Flatten the Org Chart",
    difficulty: "HARD",
    tags: ["cte", "recursive"],
    allowedEngines: ["POSTGRES"],
  },
  {
    slug: "session-gaps-and-islands",
    title: "User Session Gaps and Islands",
    difficulty: "HARD",
    tags: ["window-functions", "cte"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "product-recommendation-pipeline",
    title: "Build a Product Recommendation Pipeline",
    difficulty: "HARD",
    tags: ["mongo-aggregation", "lookup"],
    allowedEngines: ["MONGODB"],
  },
  {
    slug: "slow-query-rewrite",
    title: "Rewrite the Slow Report Query",
    difficulty: "HARD",
    tags: ["query-optimization", "join"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
  {
    slug: "star-schema-design",
    title: "Design a Star Schema for Sales",
    difficulty: "MEDIUM",
    tags: ["database-design"],
    allowedEngines: ["POSTGRES", "MYSQL"],
  },
];

/**
 * Per-slug metadata the real `ProblemSummary`/`ProblemDetail` DTOs don't
 * carry yet (estimated time, completion rate, the dataset it's authored
 * against, interview-style company tags). Includes an entry for the one
 * real backend problem (`two-sum`) so it renders exactly as richly as a
 * fixture one; every mock problem above has a matching entry too.
 */
export const PROBLEM_META: Record<string, ProblemMeta> = {
  "two-sum": {
    slug: "two-sum",
    datasetSlug: "two-sum",
    estimatedMinutes: 10,
    completionRatePct: 78,
    companyTags: ["Amazon", "Google"],
    topics: ["select", "join"],
    attempts: 41302,
  },
  "top-spenders": {
    slug: "top-spenders",
    datasetSlug: "ecommerce",
    estimatedMinutes: 8,
    completionRatePct: 81,
    companyTags: ["Amazon", "Shopify"],
    topics: ["select"],
    attempts: 12840,
  },
  "customers-without-orders": {
    slug: "customers-without-orders",
    datasetSlug: "ecommerce",
    estimatedMinutes: 10,
    completionRatePct: 69,
    companyTags: ["Meta"],
    topics: ["join"],
    attempts: 9531,
  },
  "second-highest-salary": {
    slug: "second-highest-salary",
    datasetSlug: "employees",
    estimatedMinutes: 8,
    completionRatePct: 74,
    companyTags: ["Amazon", "Microsoft", "Google"],
    topics: ["select"],
    attempts: 58200,
  },
  "department-headcount": {
    slug: "department-headcount",
    datasetSlug: "employees",
    estimatedMinutes: 6,
    completionRatePct: 88,
    companyTags: [],
    topics: ["group-by"],
    attempts: 15320,
  },
  "running-order-total": {
    slug: "running-order-total",
    datasetSlug: "ecommerce",
    estimatedMinutes: 15,
    completionRatePct: 52,
    companyTags: ["Stripe"],
    topics: ["window-functions"],
    attempts: 6210,
  },
  "orders-and-items": {
    slug: "orders-and-items",
    datasetSlug: "ecommerce",
    estimatedMinutes: 12,
    completionRatePct: 65,
    companyTags: ["Amazon"],
    topics: ["join", "group-by"],
    attempts: 8890,
  },
  "employees-above-avg": {
    slug: "employees-above-avg",
    datasetSlug: "employees",
    estimatedMinutes: 15,
    completionRatePct: 48,
    companyTags: ["Microsoft"],
    topics: ["window-functions", "cte"],
    attempts: 5120,
  },
  "monthly-active-users": {
    slug: "monthly-active-users",
    datasetSlug: "social-media",
    estimatedMinutes: 14,
    completionRatePct: 55,
    companyTags: ["Meta", "TikTok"],
    topics: ["mongo-aggregation"],
    attempts: 4310,
  },
  "fraud-flagged-transactions": {
    slug: "fraud-flagged-transactions",
    datasetSlug: "banking",
    estimatedMinutes: 18,
    completionRatePct: 41,
    companyTags: ["Stripe", "Visa"],
    topics: ["cte", "join"],
    attempts: 3980,
  },
  "recursive-org-chart": {
    slug: "recursive-org-chart",
    datasetSlug: "employees",
    estimatedMinutes: 22,
    completionRatePct: 33,
    companyTags: ["Oracle"],
    topics: ["cte"],
    attempts: 2870,
  },
  "session-gaps-and-islands": {
    slug: "session-gaps-and-islands",
    datasetSlug: "social-media",
    estimatedMinutes: 25,
    completionRatePct: 29,
    companyTags: ["Netflix"],
    topics: ["window-functions"],
    attempts: 2140,
  },
  "product-recommendation-pipeline": {
    slug: "product-recommendation-pipeline",
    datasetSlug: "ecommerce",
    estimatedMinutes: 28,
    completionRatePct: 24,
    companyTags: ["Amazon"],
    topics: ["mongo-aggregation"],
    attempts: 1560,
  },
  "slow-query-rewrite": {
    slug: "slow-query-rewrite",
    datasetSlug: "banking",
    estimatedMinutes: 20,
    completionRatePct: 37,
    companyTags: ["Datadog"],
    topics: ["query-optimization"],
    attempts: 1980,
  },
  "star-schema-design": {
    slug: "star-schema-design",
    datasetSlug: "ecommerce",
    estimatedMinutes: 30,
    completionRatePct: 44,
    companyTags: [],
    topics: ["database-design"],
    attempts: 1720,
  },
};

export const SKILL_MASTERY: SkillMastery[] = [
  { topic: "SELECT", masteryPct: 90, problemsSolved: 18, problemsTotal: 20 },
  { topic: "JOIN", masteryPct: 72, problemsSolved: 13, problemsTotal: 18 },
  { topic: "GROUP BY", masteryPct: 81, problemsSolved: 13, problemsTotal: 16 },
  { topic: "Window Functions", masteryPct: 45, problemsSolved: 5, problemsTotal: 11 },
  { topic: "CTE", masteryPct: 54, problemsSolved: 6, problemsTotal: 11 },
  { topic: "Mongo Aggregation", masteryPct: 30, problemsSolved: 3, problemsTotal: 10 },
];

export const DATASETS: Dataset[] = [
  {
    slug: "two-sum",
    name: "Two Sum (Numbers)",
    description: "A tiny interview-style dataset: a pool of numbers and target-sum queries against it.",
    category: "Interview Prep",
    engines: ["POSTGRES", "MYSQL", "MONGODB"],
    rowCountLabel: "7 rows",
    problemCount: 1,
    entities: [
      {
        name: "numbers",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "value", type: "INTEGER", nullable: false },
        ],
        sampleRows: [
          { id: 1, value: 2 },
          { id: 2, value: 7 },
          { id: 3, value: 11 },
          { id: 4, value: 15 },
        ],
      },
      {
        name: "queries",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "target", type: "INTEGER", nullable: false },
        ],
        sampleRows: [
          { id: 1, target: 9 },
          { id: 2, target: 26 },
          { id: 3, target: 100 },
        ],
      },
    ],
  },
  {
    slug: "ecommerce",
    name: "E-commerce Storefront",
    description: "Customers, orders, line items and products for a mid-size online store.",
    category: "E-commerce",
    engines: ["POSTGRES", "MYSQL", "MONGODB"],
    rowCountLabel: "48K rows",
    problemCount: 6,
    entities: [
      {
        name: "customers",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "full_name", type: "TEXT", nullable: false },
          { name: "email", type: "TEXT", nullable: false },
          { name: "signup_date", type: "TIMESTAMP", nullable: false },
          { name: "country", type: "TEXT", nullable: true },
        ],
        sampleRows: [
          { id: 101, full_name: "Ava Chen", email: "ava@example.com", signup_date: "2025-01-14", country: "US" },
          { id: 102, full_name: "Marcus Lee", email: "marcus@example.com", signup_date: "2025-02-02", country: "CA" },
          { id: 103, full_name: "Priya Nair", email: "priya@example.com", signup_date: "2025-03-19", country: "IN" },
        ],
      },
      {
        name: "orders",
        kind: "table",
        relationships: [{ toEntity: "customers", type: "many-to-one" }],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "customer_id", type: "INTEGER", nullable: false, foreignKey: "customers.id" },
          { name: "order_date", type: "TIMESTAMP", nullable: false },
          { name: "status", type: "TEXT", nullable: false },
          { name: "total_amount", type: "DECIMAL", nullable: false },
        ],
        sampleRows: [
          { id: 5001, customer_id: 101, order_date: "2026-05-01", status: "DELIVERED", total_amount: 84.5 },
          { id: 5002, customer_id: 101, order_date: "2026-06-11", status: "SHIPPED", total_amount: 23.0 },
          { id: 5003, customer_id: 103, order_date: "2026-06-14", status: "PENDING", total_amount: 156.2 },
        ],
      },
      {
        name: "order_items",
        kind: "table",
        relationships: [{ toEntity: "orders", type: "many-to-one" }],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "order_id", type: "INTEGER", nullable: false, foreignKey: "orders.id" },
          { name: "product_name", type: "TEXT", nullable: false },
          { name: "quantity", type: "INTEGER", nullable: false },
          { name: "unit_price", type: "DECIMAL", nullable: false },
        ],
        sampleRows: [
          { id: 1, order_id: 5001, product_name: "Wireless Mouse", quantity: 1, unit_price: 24.5 },
          { id: 2, order_id: 5001, product_name: "USB-C Cable", quantity: 2, unit_price: 30.0 },
        ],
      },
    ],
  },
  {
    slug: "employees",
    name: "Employees & Payroll",
    description: "A classic HR dataset: employees, departments, salaries and reporting lines.",
    category: "Employees",
    engines: ["POSTGRES", "MYSQL", "MONGODB"],
    rowCountLabel: "12K rows",
    problemCount: 4,
    entities: [
      {
        name: "departments",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "name", type: "TEXT", nullable: false },
        ],
        sampleRows: [
          { id: 1, name: "Engineering" },
          { id: 2, name: "Sales" },
          { id: 3, name: "Finance" },
        ],
      },
      {
        name: "employees",
        kind: "table",
        relationships: [
          { toEntity: "departments", type: "many-to-one" },
          { toEntity: "employees", type: "self-reference (manager_id)" },
        ],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "full_name", type: "TEXT", nullable: false },
          { name: "department_id", type: "INTEGER", nullable: false, foreignKey: "departments.id" },
          { name: "manager_id", type: "INTEGER", nullable: true, foreignKey: "employees.id" },
          { name: "salary", type: "DECIMAL", nullable: false },
          { name: "hired_at", type: "TIMESTAMP", nullable: false },
        ],
        sampleRows: [
          { id: 1, full_name: "Dana Whitfield", department_id: 1, manager_id: null, salary: 185000, hired_at: "2019-03-01" },
          { id: 2, full_name: "Omar Reyes", department_id: 1, manager_id: 1, salary: 142000, hired_at: "2021-07-12" },
          { id: 3, full_name: "Lin Zhao", department_id: 2, manager_id: null, salary: 128000, hired_at: "2020-01-20" },
        ],
      },
    ],
  },
  {
    slug: "banking",
    name: "Retail Banking",
    description: "Accounts, transactions and card activity, useful for fraud/risk-style problems.",
    category: "Banking",
    engines: ["POSTGRES", "MYSQL", "MONGODB"],
    rowCountLabel: "210K rows",
    problemCount: 2,
    entities: [
      {
        name: "accounts",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "holder_name", type: "TEXT", nullable: false },
          { name: "account_type", type: "TEXT", nullable: false },
          { name: "opened_at", type: "TIMESTAMP", nullable: false },
        ],
        sampleRows: [
          { id: 8001, holder_name: "J. Alvarez", account_type: "CHECKING", opened_at: "2018-04-11" },
          { id: 8002, holder_name: "S. Patel", account_type: "SAVINGS", opened_at: "2020-09-02" },
        ],
      },
      {
        name: "transactions",
        kind: "table",
        relationships: [{ toEntity: "accounts", type: "many-to-one" }],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "account_id", type: "INTEGER", nullable: false, foreignKey: "accounts.id" },
          { name: "amount", type: "DECIMAL", nullable: false },
          { name: "merchant", type: "TEXT", nullable: true },
          { name: "occurred_at", type: "TIMESTAMP", nullable: false },
          { name: "flagged", type: "BOOLEAN", nullable: false },
        ],
        sampleRows: [
          { id: 90001, account_id: 8001, amount: -42.1, merchant: "Corner Cafe", occurred_at: "2026-06-01", flagged: false },
          { id: 90002, account_id: 8001, amount: -1250.0, merchant: "Unknown POS", occurred_at: "2026-06-02", flagged: true },
        ],
      },
    ],
  },
  {
    slug: "social-media",
    name: "Social Media Activity",
    description: "Users, posts, likes and sessions - a natural fit for engagement/retention problems.",
    category: "Social Media",
    engines: ["POSTGRES", "MYSQL", "MONGODB"],
    rowCountLabel: "1.2M rows",
    problemCount: 2,
    entities: [
      {
        name: "users",
        kind: "table",
        relationships: [],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "handle", type: "TEXT", nullable: false },
          { name: "joined_at", type: "TIMESTAMP", nullable: false },
        ],
        sampleRows: [
          { id: 1, handle: "@rowan", joined_at: "2023-01-05" },
          { id: 2, handle: "@juno", joined_at: "2023-02-19" },
        ],
      },
      {
        name: "posts",
        kind: "table",
        relationships: [{ toEntity: "users", type: "many-to-one" }],
        columns: [
          { name: "id", type: "INTEGER", nullable: false, primaryKey: true },
          { name: "user_id", type: "INTEGER", nullable: false, foreignKey: "users.id" },
          { name: "posted_at", type: "TIMESTAMP", nullable: false },
          { name: "like_count", type: "INTEGER", nullable: false },
        ],
        sampleRows: [
          { id: 1, user_id: 1, posted_at: "2026-06-01", like_count: 34 },
          { id: 2, user_id: 2, posted_at: "2026-06-02", like_count: 128 },
        ],
      },
    ],
  },
];

/** Problem statements for the fixture catalog above - written out per-problem rather than templated, so Practice doesn't feel like a wall of placeholder text. Keyed by slug, same as `PROBLEM_META`. */
export const MOCK_STATEMENTS: Record<string, string> = {
  "top-spenders": `Write a query against the **ecommerce** dataset that returns each customer's total spend across all of their orders this month, ordered from highest to lowest.

**Return:** \`customer_id\`, \`total_spent\` - one row per customer who placed at least one order this month.`,
  "customers-without-orders": `Using the **ecommerce** dataset, find every customer who has never placed an order.

**Return:** \`customer_id\`, \`full_name\` - customers with zero rows in \`orders\`.`,
  "second-highest-salary": `From the **employees** dataset, find the second-highest salary company-wide. If there's a tie for the highest salary, the second-highest is still the next distinct value down.

**Return:** a single column \`second_highest_salary\` (one row).`,
  "department-headcount": `Using the **employees** dataset, count how many employees belong to each department.

**Return:** \`department_id\`, \`headcount\`, ordered by \`headcount\` descending.`,
  "running-order-total": `Using the **ecommerce** dataset, compute a running (cumulative) total of order amounts per day, ordered by date.

**Return:** \`order_date\`, \`daily_total\`, \`running_total\` - one row per day that had at least one order.`,
  "orders-and-items": `Using the **ecommerce** dataset, return each order together with how many distinct line items it contains and its total value (quantity × unit price, summed).

**Return:** \`order_id\`, \`item_count\`, \`order_value\`.`,
  "employees-above-avg": `Using the **employees** dataset, find every employee whose salary is above their own department's average salary.

**Return:** \`employee_id\`, \`full_name\`, \`department_id\`, \`salary\` - employees strictly above their department's average.`,
  "monthly-active-users": `Using the **social-media** dataset's \`posts\` collection, count how many distinct users posted at least once in each calendar month.

**Return:** \`month\`, \`active_users\` - one document per month present in the data.`,
  "fraud-flagged-transactions": `Using the **banking** dataset, find accounts with three or more flagged transactions in the last 30 days, along with their total flagged amount.

**Return:** \`account_id\`, \`flagged_count\`, \`flagged_total\` - only accounts meeting the threshold.`,
  "recursive-org-chart": `Using the **employees** dataset's \`manager_id\` self-reference, produce a flattened org chart showing each employee alongside their reporting depth from the top (a top-level employee with no manager has depth 0).

**Return:** \`employee_id\`, \`full_name\`, \`depth\`.`,
  "session-gaps-and-islands": `Using the **social-media** dataset, identify "sessions" of consecutive daily activity per user - a session ends when a user has a gap of 2 or more days with no posts.

**Return:** \`user_id\`, \`session_start\`, \`session_end\`, one row per session.`,
  "product-recommendation-pipeline": `Using the **ecommerce** dataset materialized into MongoDB, build an aggregation pipeline that recommends the top 3 products most frequently bought alongside each product, based on shared orders.

**Return:** one document per product with a \`recommended\` array of up to 3 product names.`,
  "slow-query-rewrite": `The following report query against the **banking** dataset is correct but slow on the full dataset - it applies a function to an indexed column in its \`WHERE\` clause, which defeats the index. Rewrite it to be equivalent but faster.

\`\`\`sql
SELECT * FROM transactions
WHERE LOWER(merchant) = 'unknown pos';
\`\`\`

**Return:** a rewritten query returning the same rows, without wrapping the filtered column in a function.`,
  "star-schema-design": `Design a star schema for analyzing **ecommerce** sales: propose a fact table and the dimension tables it should reference, and state the grain of the fact table in one sentence.

**Return:** a short written design (table names + key columns), not a query - this is a modeling problem, not an execution one.`,
};
