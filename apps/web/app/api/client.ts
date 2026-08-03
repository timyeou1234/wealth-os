import { client } from "./generated/client.gen";

client.setConfig({ baseUrl: "" });

export { archiveAsset, archiveLiability, captureSnapshot, getFinancialHealth, getFxRates, getSnapshot, listAssets, listLiabilities, listSnapshots } from "./generated";
export type { AssetFactResponse, AssetResponse, CaptureAssetRequest, CaptureLiabilityRequest, CaptureSnapshotRequest, FinancialHealthResponse, FxRateItemResponse, LiabilityFactResponse, LiabilityResponse, MoneyResponse, SnapshotResponse, ValidationProblemResponse } from "./generated";
