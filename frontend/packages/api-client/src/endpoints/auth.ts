import type { ApiClient } from "../client";
import type { AuthResponse, AuthUser, LoginRequest, RegisterRequest } from "../types";

export function createAuthEndpoints(client: ApiClient) {
  return {
    register: (body: RegisterRequest) =>
      client.request<AuthResponse>("/api/v1/auth/register", { method: "POST", body: JSON.stringify(body) }),

    login: (body: LoginRequest) =>
      client.request<AuthResponse>("/api/v1/auth/login", { method: "POST", body: JSON.stringify(body) }),

    /** Relies on the HttpOnly refresh cookie being sent automatically (`credentials: "include"`) - no body needed. */
    refresh: () => client.request<AuthResponse>("/api/v1/auth/refresh", { method: "POST" }),

    logout: () => client.request<void>("/api/v1/auth/logout", { method: "POST" }),

    me: () => client.request<AuthUser>("/api/v1/auth/me"),
  };
}
