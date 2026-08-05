import { client } from "./generated/client.gen";
import { CSRF_HEADER_NAME, csrfTokenFromCookie } from "./csrf";
import { handleSessionResponse } from "./session-timeout";

client.setConfig({ baseUrl: "" });

client.interceptors.request.use((request) => {
  const csrfToken = csrfTokenFromCookie(document.cookie);
  if (csrfToken) request.headers.set(CSRF_HEADER_NAME, csrfToken);
  return request;
});

client.interceptors.response.use((response) => handleSessionResponse(
  response,
  `${window.location.pathname}${window.location.search}`,
  (url) => window.location.assign(url),
));

export { archiveAsset, archiveLiability, captureSnapshot, getFinancialHealth, getFxRates, getSnapshot, listAssets, listLiabilities, listSnapshots } from "./generated";
export type { AssetFactResponse, AssetResponse, CaptureAssetRequest, CaptureLiabilityRequest, CaptureSnapshotRequest, FinancialHealthResponse, FxRateItemResponse, LiabilityFactResponse, LiabilityResponse, MoneyResponse, SnapshotResponse, ValidationProblemResponse } from "./generated";
