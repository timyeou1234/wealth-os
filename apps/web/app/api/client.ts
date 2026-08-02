import { client } from "./generated/client.gen";

client.setConfig({ baseUrl: "" });

export { archiveAsset, archiveLiability, captureSnapshot, getFinancialHealth, getSnapshot, listAssets, listLiabilities, listSnapshots } from "./generated";
export type { AssetFactResponse, AssetResponse, CaptureAssetRequest, CaptureLiabilityRequest, CaptureSnapshotRequest, FinancialHealthResponse, LiabilityFactResponse, LiabilityResponse, MoneyResponse, SnapshotResponse } from "./generated";
