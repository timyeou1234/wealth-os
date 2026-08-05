import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import { CSRF_COOKIE_NAME } from "./app/api/csrf";
import { auth0 } from "./app/server/auth0";

export async function proxy(request: NextRequest): Promise<NextResponse> {
  if (request.nextUrl.pathname.startsWith("/auth/")) {
    const response = await auth0.middleware(request);
    if (request.nextUrl.pathname === "/auth/logout") response.cookies.delete(CSRF_COOKIE_NAME);
    if (request.nextUrl.pathname === "/auth/callback") setCsrfCookie(request, response);
    return response;
  }

  try {
    const session = await auth0.getSession(request);
    if (!session) {
      if (request.nextUrl.pathname.startsWith("/api/")) {
        return NextResponse.json({ error: "unauthorized" }, { status: 401 });
      }
      const login = new URL("/auth/login", request.url);
      login.searchParams.set("returnTo", `${request.nextUrl.pathname}${request.nextUrl.search}`);
      return NextResponse.redirect(login);
    }
    const response = await auth0.middleware(request);
    if (!request.cookies.has(CSRF_COOKIE_NAME)) setCsrfCookie(request, response);
    return response;
  } catch {
    return NextResponse.json({ error: "authentication_unavailable" }, { status: 503 });
  }
}

function setCsrfCookie(request: NextRequest, response: NextResponse): void {
  response.cookies.set(CSRF_COOKIE_NAME, crypto.randomUUID(), {
    httpOnly: false,
    path: "/",
    sameSite: "strict",
    secure: request.nextUrl.protocol === "https:",
  });
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|sitemap.xml|robots.txt).*)"],
};
