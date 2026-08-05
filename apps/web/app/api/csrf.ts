export const CSRF_COOKIE_NAME = "wealthos_csrf";
export const CSRF_HEADER_NAME = "x-wealthos-csrf";

export function csrfTokenFromCookie(cookieHeader: string): string | null {
  const prefix = `${CSRF_COOKIE_NAME}=`;
  const encoded = cookieHeader
    .split(";")
    .map((cookie) => cookie.trim())
    .find((cookie) => cookie.startsWith(prefix))
    ?.slice(prefix.length);
  if (!encoded) return null;
  try {
    return decodeURIComponent(encoded);
  } catch {
    return null;
  }
}
