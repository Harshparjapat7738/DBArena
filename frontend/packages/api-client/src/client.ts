import { ApiError, type ProblemDetails } from "./types";

export interface ApiClientConfig {
  /** No trailing slash. Client-side this is `/api/proxy` (same-origin BFF, see apps/web's proxy route handler); server-side it's the gateway's own URL - no CORS concern for a server-to-server fetch. */
  baseUrl: string;
  /** Only ever supplied client-side - the access token lives in memory only (frontend/CLAUDE.md: no localStorage for auth), so a Server Component has no way to obtain one and must stick to public endpoints. */
  getAccessToken?: () => string | null | undefined;
}

export interface ApiClient {
  request<T>(path: string, init?: RequestInit): Promise<T>;
}

/** Thin fetch wrapper: attaches the bearer token (if any), assumes/parses JSON, maps a non-2xx response to {@link ApiError} using its RFC 7807 body. */
export function createApiClient(config: ApiClientConfig): ApiClient {
  async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const token = config.getAccessToken?.();
    const headers = new Headers(init.headers);
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    if (init.body && !headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${config.baseUrl}${path}`, {
      ...init,
      headers,
      credentials: "include",
    });

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get("content-type") ?? "";
    const isJson = contentType.includes("json");
    const body = isJson ? await response.json() : undefined;

    if (!response.ok) {
      throw new ApiError(response.status, isJson ? (body as ProblemDetails) : undefined);
    }

    return body as T;
  }

  return { request };
}
