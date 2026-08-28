// Mirrors backend DTOs (catalog-service's ProblemDetailResponse/ProblemSummaryResponse,
// identity-service's auth responses, ai-assistant-service's HintResponseBody) field-for-field.
// Hand-written, not generated - frontend/CLAUDE.md's "typed client generated from the
// gateway OpenAPI" describes the eventual state; no service in this reactor emits a
// consolidated OpenAPI spec file yet to generate from, so this is the documented interim.

export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type EngineKind = "POSTGRES" | "MYSQL" | "MONGODB";

export interface ProblemSummary {
  slug: string;
  title: string;
  difficulty: Difficulty;
  tags: string[];
  allowedEngines: EngineKind[];
}

export interface ProblemDetail {
  slug: string;
  title: string;
  statementMarkdown: string;
  difficulty: Difficulty;
  tags: string[];
  allowedEngines: EngineKind[];
  datasetSlug: string;
  published: boolean;
  createdAtEpochMillis: number;
  updatedAtEpochMillis: number;
}

export interface TagCount {
  tag: string;
  count: number;
}

/**
 * Mirrors common-core's `CursorPage<T>` record exactly as Jackson emits
 * it: `items` plus `nextCursor` (an `Optional<String>` that serializes as
 * the string or `null`, via Spring Boot's auto-registered jdk8 module) -
 * `hasMore()` is a derived instance method on the Java record, not a
 * component, so it is never present in the JSON. Compute it here instead.
 */
export interface CursorPage<T> {
  items: T[];
  nextCursor: string | null;
}

export function hasMorePages<T>(page: CursorPage<T>): boolean {
  return page.nextCursor !== null;
}

export interface ProblemListQuery {
  cursor?: string;
  limit?: number;
  tag?: string;
  difficulty?: Difficulty;
  engine?: EngineKind;
  q?: string;
}

export type HintLevel = "CONCEPT" | "APPROACH" | "NEAR_MISS";

export interface HintRequest {
  learnerQuery: string;
  errorOrResultText?: string;
  level: HintLevel;
}

export interface HintResponse {
  problemSlug: string;
  level: HintLevel;
  hint: string;
  provider: string;
  truncated: boolean;
}

export interface AuthUser {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
}

/** Backend enforces a 12-200 char password (identity-service's `RegisterRequest`) - mirrored in the frontend Zod schema, not just here. */
export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface AuthResponse {
  accessToken: string;
  user: AuthUser;
}

/** RFC 7807 `application/problem+json`, per root CLAUDE.md's REST conventions. */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  [extension: string]: unknown;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly problem?: ProblemDetails;

  constructor(status: number, problem?: ProblemDetails) {
    super(problem?.detail ?? problem?.title ?? `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.code = problem?.code;
    this.problem = problem;
  }
}
