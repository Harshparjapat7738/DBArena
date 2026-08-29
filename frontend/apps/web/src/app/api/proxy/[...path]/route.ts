import { NextRequest, NextResponse } from "next/server";

/**
 * Generic same-origin reverse proxy to api-gateway - the BFF pattern
 * frontend/CLAUDE.md's "no localStorage for auth" rule implies: the
 * browser only ever talks to this app's own origin, so (a) the HttpOnly
 * refresh cookie set by identity-service is scoped to a domain the browser
 * will actually send it back to, and (b) api-gateway never needs CORS
 * configured for a browser origin at all (M14 left that carried-forward;
 * this sidesteps it rather than waiting on it). Every request this app's
 * client-side API layer makes goes through here, auth and data alike -
 * one code path, not a bespoke route handler per backend endpoint.
 *
 * Forwards: method, body, the `Authorization` header (the in-memory access
 * token), and the `Cookie` header (the refresh cookie, for `/auth/refresh`
 * and `/auth/logout`). Forwards back: status, body, and every `Set-Cookie`
 * the upstream sent - Next's fetch (undici) exposes multiple Set-Cookie
 * values via `headers.getSetCookie()`.
 */
const GATEWAY_URL = process.env.DBArena_GATEWAY_URL ?? "http://localhost:8080";

const HOP_BY_HOP_REQUEST_HEADERS = new Set(["host", "connection", "content-length"]);

async function proxy(request: NextRequest, path: string[]): Promise<NextResponse> {
  const targetUrl = `${GATEWAY_URL}/${path.join("/")}${request.nextUrl.search}`;

  const headers = new Headers();
  request.headers.forEach((value, key) => {
    if (!HOP_BY_HOP_REQUEST_HEADERS.has(key.toLowerCase())) {
      headers.set(key, value);
    }
  });

  const hasBody = !["GET", "HEAD"].includes(request.method);
  const upstream = await fetch(targetUrl, {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    redirect: "manual",
  });

  const responseHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    if (key.toLowerCase() !== "set-cookie") {
      responseHeaders.set(key, value);
    }
  });

  const response = new NextResponse(upstream.body, {
    status: upstream.status,
    headers: responseHeaders,
  });

  for (const cookie of upstream.headers.getSetCookie()) {
    response.headers.append("set-cookie", cookie);
  }

  return response;
}

type RouteContext = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function POST(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function PUT(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return proxy(request, path);
}

export async function DELETE(request: NextRequest, context: RouteContext) {
  const { path } = await context.params;
  return proxy(request, path);
}
