import { createAiEndpoints, createApiClient, createAuthEndpoints, createCatalogEndpoints } from "@DBArena/api-client";
import { useAuthStore } from "../auth/authStore";

/**
 * Client-side: everything goes through the same-origin BFF proxy
 * (`/api/proxy/**`, see `app/api/proxy/[...path]/route.ts`) so the
 * browser never needs api-gateway's CORS configured (M14 left this
 * carried-forward and it's still true) and the HttpOnly refresh cookie is
 * sent automatically since it's a same-origin request.
 */
const browserClient = createApiClient({
  baseUrl: "/api/proxy",
  getAccessToken: () => useAuthStore.getState().accessToken,
});

export const authApi = createAuthEndpoints(browserClient);
export const catalogApi = createCatalogEndpoints(browserClient);
export const aiApi = createAiEndpoints(browserClient);

/**
 * Server-side (Server Components only): talks to api-gateway directly -
 * no CORS concern for a server-to-server fetch, no extra hop through this
 * app's own proxy. Only ever used for PUBLIC reads: a Server Component has
 * no access to the in-memory access token (it lives in browser JS only),
 * so anything requiring auth must happen client-side through `catalogApi`/
 * `aiApi`/`authApi` above instead.
 */
export function createServerCatalogApi() {
  const gatewayUrl = process.env.DBArena_GATEWAY_URL ?? "http://localhost:8080";
  return createCatalogEndpoints(createApiClient({ baseUrl: gatewayUrl }));
}
