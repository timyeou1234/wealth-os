import { client } from "./generated/client.gen";

client.setConfig({ baseUrl: "" });

export { getFinancialHealth, getSnapshot, listSnapshots } from "./generated";
export type { AssetFactResponse, FinancialHealthResponse, LiabilityFactResponse, MoneyResponse, SnapshotResponse } from "./generated";
