import { ApiError, createApiClient } from "@DBArena/api-client";
import { afterEach, describe, expect, it, vi } from "vitest";

function mockFetchOnce(response: Partial<Response> & { json?: () => Promise<unknown> }) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: response.ok ?? true,
    status: response.status ?? 200,
    headers: response.headers ?? new Headers({ "content-type": "application/json" }),
    json: response.json ?? (async () => ({})),
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("api-client", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("attaches the bearer token when one is available", async () => {
    const fetchMock = mockFetchOnce({ json: async () => ({ ok: true }) });
    const client = createApiClient({ baseUrl: "/api/proxy", getAccessToken: () => "token-123" });

    await client.request("/api/v1/catalog/tags");

    const headers = fetchMock.mock.calls[0]?.[1].headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer token-123");
  });

  it("omits the Authorization header when there is no token", async () => {
    const fetchMock = mockFetchOnce({ json: async () => ({}) });
    const client = createApiClient({ baseUrl: "/api/proxy", getAccessToken: () => null });

    await client.request("/api/v1/catalog/tags");

    const headers = fetchMock.mock.calls[0]?.[1].headers as Headers;
    expect(headers.has("Authorization")).toBe(false);
  });

  it("throws an ApiError with the RFC 7807 body on a non-2xx response", async () => {
    mockFetchOnce({
      ok: false,
      status: 404,
      json: async () => ({ title: "Not Found", status: 404, code: "catalog.problem_not_found" }),
    });
    const client = createApiClient({ baseUrl: "/api/proxy" });

    await expect(client.request("/api/v1/catalog/problems/missing")).rejects.toMatchObject({
      status: 404,
      code: "catalog.problem_not_found",
    });
  });

  it("returns undefined for a 204 No Content response", async () => {
    mockFetchOnce({ status: 204, headers: new Headers() });
    const client = createApiClient({ baseUrl: "/api/proxy" });

    await expect(client.request("/auth/logout", { method: "POST" })).resolves.toBeUndefined();
  });

  it("ApiError falls back to a generic message when the body has no title/detail", () => {
    const err = new ApiError(500);
    expect(err.message).toBe("Request failed with status 500");
  });
});
