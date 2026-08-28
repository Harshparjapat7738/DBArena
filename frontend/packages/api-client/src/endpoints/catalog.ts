import type { ApiClient } from "../client";
import type { CursorPage, ProblemDetail, ProblemListQuery, ProblemSummary, TagCount } from "../types";

function buildQuery(query: ProblemListQuery): string {
  const params = new URLSearchParams();
  if (query.cursor) params.set("cursor", query.cursor);
  if (query.limit) params.set("limit", String(query.limit));
  if (query.tag) params.set("tag", query.tag);
  if (query.difficulty) params.set("difficulty", query.difficulty);
  if (query.engine) params.set("engine", query.engine);
  if (query.q) params.set("q", query.q);
  const qs = params.toString();
  return qs ? `?${qs}` : "";
}

/** Every call here hits a public endpoint (catalog-service browsing, no auth required) - safe from a Server Component too. */
export function createCatalogEndpoints(client: ApiClient) {
  return {
    listProblems: (query: ProblemListQuery = {}) =>
      client.request<CursorPage<ProblemSummary>>(`/api/v1/catalog/problems${buildQuery(query)}`),

    getProblem: (slug: string) => client.request<ProblemDetail>(`/api/v1/catalog/problems/${encodeURIComponent(slug)}`),

    listTags: () => client.request<TagCount[]>("/api/v1/catalog/tags"),
  };
}
