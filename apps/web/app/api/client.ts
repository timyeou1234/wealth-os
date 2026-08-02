import { client } from "./generated/client.gen";

client.setConfig({ baseUrl: "" });

export { get as getSnapshot, get2 as getFinancialHealth, list as listSnapshots } from "./generated";
export type { AssetFactResponse, FinancialHealthResponse, LiabilityFactResponse, MoneyResponse, SnapshotResponse } from "./generated";
