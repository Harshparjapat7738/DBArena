import type { ApiClient } from "../client";
import type { HintRequest, HintResponse } from "../types";

export function createAiEndpoints(client: ApiClient) {
  return {
    getHint: (problemSlug: string, body: HintRequest) =>
      client.request<HintResponse>(`/api/v1/ai/problems/${encodeURIComponent(problemSlug)}/hint`, {
        method: "POST",
        body: JSON.stringify(body),
      }),
  };
}
