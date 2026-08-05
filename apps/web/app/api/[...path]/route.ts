import { auth0 } from "../../server/auth0";
import { timingSafeEqual } from "node:crypto";
import { CSRF_HEADER_NAME, csrfTokenFromCookie } from "../csrf";

type RouteContext = { params: Promise<{ path: string[] }> };

const READ_ROUTES = [
  /^v1\/assets(?:\/[0-9a-f-]+)?$/,
  /^v1\/liabilities(?:\/[0-9a-f-]+)?$/,
  /^v1\/snapshots(?:\/[0-9a-f-]+)?$/,
  /^v1\/fx-rates$/,
  /^v1\/financial-health\/[0-9a-f-]+$/,
];
// This is a security policy, not a mirror of every OpenAPI operation. Product-user
// routes are explicit so future operational endpoints are private until reviewed.
const POST_ROUTES = [
  /^v1\/assets$/,
  /^v1\/assets\/[0-9a-f-]+\/archive$/,
  /^v1\/liabilities$/,
  /^v1\/liabilities\/[0-9a-f-]+\/archive$/,
  /^v1\/snapshot-captures$/,
  /^v1\/snapshots$/,
];
const PUT_ROUTES = [
  /^v1\/assets\/[0-9a-f-]+$/,
  /^v1\/liabilities\/[0-9a-f-]+$/,
];

function isAllowed(method: string, path: string): boolean {
  const routes = method === "GET" ? READ_ROUTES : method === "POST" ? POST_ROUTES : PUT_ROUTES;
  return routes.some((route) => route.test(path));
}

function hasCsrfProtection(request: Request): boolean {
  const origin = request.headers.get("origin");
  const allowedOrigins = (process.env.APP_BASE_URL ?? "http://localhost:3000")
    .split(",")
    .map((value) => new URL(value.trim()).origin);
  const headerToken = request.headers.get(CSRF_HEADER_NAME);
  const cookieToken = csrfTokenFromCookie(request.headers.get("cookie") ?? "");
  if (!headerToken || !cookieToken) return false;
  const headerBytes = Buffer.from(headerToken);
  const cookieBytes = Buffer.from(cookieToken);
  if (headerBytes.length !== cookieBytes.length) return false;
  const validToken = timingSafeEqual(headerBytes, cookieBytes);
  return validToken && origin !== null && allowedOrigins.includes(origin);
}

async function forward(request: Request, context: RouteContext): Promise<Response> {
  let session;
  try {
    session = await auth0.getSession();
  } catch {
    return Response.json({ error: "authentication_unavailable" }, { status: 503 });
  }
  if (!session) {
    return Response.json({ error: "unauthorized" }, { status: 401 });
  }

  const path = (await context.params).path.join("/");
  if (!isAllowed(request.method, path)) {
    return Response.json({ error: "not_found" }, { status: 404 });
  }
  if (request.method !== "GET" && !hasCsrfProtection(request)) {
    return Response.json({ error: "forbidden" }, { status: 403 });
  }

  let token: string;
  try {
    token = (await auth0.getAccessToken()).token;
  } catch {
    return Response.json({ error: "unauthorized" }, { status: 401 });
  }

  const requestUrl = new URL(request.url);
  const apiBaseUrl = process.env.WEALTHOS_API_BASE_URL ?? "http://localhost:8080";
  const body = request.method === "GET" ? undefined : await request.text();
  const headers: Record<string, string> = {
    accept: request.headers.get("accept") ?? "application/json",
    authorization: `Bearer ${token}`,
  };
  const requestContentType = request.headers.get("content-type");
  if (requestContentType) headers["content-type"] = requestContentType;
  const upstream = await fetch(`${apiBaseUrl}/api/${path}${requestUrl.search}`, {
    method: request.method,
    headers,
    body,
    cache: "no-store",
  });

  const responseHeaders = new Headers();
  const contentType = upstream.headers.get("content-type");
  if (contentType) responseHeaders.set("content-type", contentType);
  responseHeaders.set("cache-control", "no-store");
  return new Response(upstream.body, { status: upstream.status, headers: responseHeaders });
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
