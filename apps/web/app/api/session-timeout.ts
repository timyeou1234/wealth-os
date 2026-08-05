export function handleSessionResponse(
  response: Response,
  returnTo: string,
  navigate: (url: string) => void,
): Response {
  if (response.status === 401) {
    navigate(`/auth/login?returnTo=${encodeURIComponent(returnTo)}`);
  }
  return response;
}
