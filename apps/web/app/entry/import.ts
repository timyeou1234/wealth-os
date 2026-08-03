export type AgentImportAsset = {
  id?: string;
  name: string;
  type: "CASH" | "INVESTMENT" | "REAL_ESTATE" | "VEHICLE" | "BUSINESS" | "OTHER";
  liquidity: "LIQUID" | "SEMI_LIQUID" | "ILLIQUID";
  amount: string;
  effectiveDate: string;
  source: string;
  manualConversion?: AgentImportManualConversion;
};

export type AgentImportLiability = {
  id?: string;
  name: string;
  amount: string;
  effectiveDate: string;
  source: string;
  manualConversion?: AgentImportManualConversion;
};

export type AgentImportManualConversion = {
  originalAmount: string;
  originalCurrency: string;
  exchangeRateBasis: string;
  effectiveDate: string;
};

export type AgentImport = {
  schemaVersion: 1;
  baseCurrency: string;
  snapshotDate?: string;
  assets: AgentImportAsset[];
  liabilities: AgentImportLiability[];
};

const MAX_BYTES = 256 * 1024;
const MAX_POSITIONS = 500;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const DECIMAL = /^(?:0|[1-9]\d*)(?:\.\d+)?$/;
const DATE = /^\d{4}-\d{2}-\d{2}$/;
const ASSET_TYPES = new Set(["CASH", "INVESTMENT", "REAL_ESTATE", "VEHICLE", "BUSINESS", "OTHER"]);
const LIQUIDITIES = new Set(["LIQUID", "SEMI_LIQUID", "ILLIQUID"]);
const SUPPORTED_CURRENCIES = new Set(Intl.supportedValuesOf("currency"));
const PREFERRED_CURRENCIES = ["TWD", "USD", "EUR", "JPY", "CNY", "HKD", "GBP", "AUD", "CAD", "SGD"];

export const supportedCurrencies = [
  ...PREFERRED_CURRENCIES.filter((currency) => SUPPORTED_CURRENCIES.has(currency)),
  ...[...SUPPORTED_CURRENCIES].filter((currency) => !PREFERRED_CURRENCIES.includes(currency)).sort(),
];

export function isSupportedCurrency(value: string): boolean {
  return /^[A-Z]{3}$/.test(value) && SUPPORTED_CURRENCIES.has(value);
}

export function parseAgentImport(input: string, knownAssetIds: Set<string>, knownLiabilityIds: Set<string>): AgentImport {
  if (new TextEncoder().encode(input).byteLength > MAX_BYTES) throw new Error("Import is larger than 256 KB and is not allowed.");
  const text = unwrap(input.trim());
  let value: unknown;
  try {
    value = JSON.parse(text);
  } catch {
    throw new Error("Agent output must be valid JSON or one JSON code block.");
  }
  const root = object(value, "Import");
  allow(root, ["schemaVersion", "baseCurrency", "snapshotDate", "assets", "liabilities"], "Import");
  if (root.schemaVersion !== 1) throw new Error("schemaVersion must be 1.");
  const baseCurrency = requiredString(root.baseCurrency, "baseCurrency", 3);
  if (!/^[A-Z]{3}$/.test(baseCurrency)) throw new Error("baseCurrency must be a three-letter uppercase currency code.");
  if (!isSupportedCurrency(baseCurrency)) throw new Error("baseCurrency must be a supported ISO 4217 currency.");
  const snapshotDate = optionalDate(root.snapshotDate, "snapshotDate");
  const rawAssets = array(root.assets, "assets");
  const rawLiabilities = array(root.liabilities, "liabilities");
  if (rawAssets.length + rawLiabilities.length > MAX_POSITIONS) throw new Error("Import cannot contain more than 500 positions.");

  const assets = rawAssets.map((item, index) => parseAsset(item, index, knownAssetIds, baseCurrency));
  const liabilities = rawLiabilities.map((item, index) => parseLiability(item, index, knownLiabilityIds, baseCurrency));
  duplicateIds(assets, "asset");
  duplicateIds(liabilities, "liability");
  return { schemaVersion: 1, baseCurrency, ...(snapshotDate ? { snapshotDate } : {}), assets, liabilities };
}

function parseAsset(value: unknown, index: number, knownIds: Set<string>, baseCurrency: string): AgentImportAsset {
  const label = `assets[${index}]`;
  const item = object(value, label);
  allow(item, ["id", "name", "type", "liquidity", "amount", "effectiveDate", "source", "manualConversion"], label);
  const id = optionalId(item.id, label, knownIds);
  const type = requiredString(item.type, `${label}.type`, 32);
  const liquidity = requiredString(item.liquidity, `${label}.liquidity`, 32);
  if (!ASSET_TYPES.has(type)) throw new Error(`${label}.type is unknown.`);
  if (!LIQUIDITIES.has(liquidity)) throw new Error(`${label}.liquidity is unknown.`);
  return {
    ...(id ? { id } : {}),
    name: requiredString(item.name, `${label}.name`, 200),
    type: type as AgentImportAsset["type"],
    liquidity: liquidity as AgentImportAsset["liquidity"],
    amount: amount(item.amount, `${label}.amount`),
    effectiveDate: requiredDate(item.effectiveDate, `${label}.effectiveDate`),
    source: requiredString(item.source, `${label}.source`, 100),
    ...parseManualConversion(item.manualConversion, `${label}.manualConversion`, baseCurrency),
  };
}

function parseLiability(value: unknown, index: number, knownIds: Set<string>, baseCurrency: string): AgentImportLiability {
  const label = `liabilities[${index}]`;
  const item = object(value, label);
  allow(item, ["id", "name", "amount", "effectiveDate", "source", "manualConversion"], label);
  const id = optionalId(item.id, label, knownIds);
  return {
    ...(id ? { id } : {}),
    name: requiredString(item.name, `${label}.name`, 200),
    amount: amount(item.amount, `${label}.amount`),
    effectiveDate: requiredDate(item.effectiveDate, `${label}.effectiveDate`),
    source: requiredString(item.source, `${label}.source`, 100),
    ...parseManualConversion(item.manualConversion, `${label}.manualConversion`, baseCurrency),
  };
}

function parseManualConversion(value: unknown, label: string, baseCurrency: string): { manualConversion?: AgentImportManualConversion } {
  if (value === undefined) return {};
  const item = object(value, label);
  allow(item, ["originalAmount", "originalCurrency", "exchangeRateBasis", "effectiveDate"], label);
  const originalCurrency = requiredString(item.originalCurrency, `${label}.originalCurrency`, 3);
  if (!/^[A-Z]{3}$/.test(originalCurrency)) throw new Error(`${label}.originalCurrency must be a three-letter uppercase currency code.`);
  if (!isSupportedCurrency(originalCurrency)) throw new Error(`${label}.originalCurrency must be a supported ISO 4217 currency.`);
  if (originalCurrency === baseCurrency) throw new Error(`${label}.originalCurrency must differ from baseCurrency.`);
  return { manualConversion: {
    originalAmount: amount(item.originalAmount, `${label}.originalAmount`),
    originalCurrency,
    exchangeRateBasis: requiredString(item.exchangeRateBasis, `${label}.exchangeRateBasis`, 200),
    effectiveDate: requiredDate(item.effectiveDate, `${label}.effectiveDate`),
  } };
}

function unwrap(input: string): string {
  if (!input.startsWith("```")) return input;
  const match = input.match(/^```(?:json)?[ \t]*\n([\s\S]*?)\n```$/i);
  if (!match) throw new Error("Agent output must contain exactly one JSON code block without extra text.");
  return match[1];
}

function object(value: unknown, label: string): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value) || Object.getPrototypeOf(value) !== Object.prototype) throw new Error(`${label} must be a JSON object.`);
  return value as Record<string, unknown>;
}

function allow(value: Record<string, unknown>, allowed: string[], label: string) {
  const allowedKeys = new Set(allowed);
  for (const key of Object.keys(value)) {
    if (!allowedKeys.has(key) || key === "__proto__" || key === "constructor" || key === "prototype") throw new Error(`${label}.${key} is not allowed.`);
  }
}

function array(value: unknown, label: string): unknown[] {
  if (value === undefined) return [];
  if (!Array.isArray(value)) throw new Error(`${label} must be an array.`);
  return value;
}

function requiredString(value: unknown, label: string, maxLength: number): string {
  if (typeof value !== "string" || value.trim().length === 0) throw new Error(`${label} is required.`);
  if (value.length > maxLength) throw new Error(`${label} is too long.`);
  return value;
}

function amount(value: unknown, label: string): string {
  const result = requiredString(value, label, 100);
  if (!DECIMAL.test(result)) throw new Error(`${label} must be a non-negative decimal string.`);
  return result;
}

function requiredDate(value: unknown, label: string): string {
  const result = requiredString(value, label, 10);
  if (!DATE.test(result) || Number.isNaN(Date.parse(`${result}T00:00:00Z`)) || new Date(`${result}T00:00:00Z`).toISOString().slice(0, 10) !== result) throw new Error(`${label} must be a valid YYYY-MM-DD date.`);
  return result;
}

function optionalDate(value: unknown, label: string): string | undefined {
  if (value === undefined) return undefined;
  return requiredDate(value, label);
}

function optionalId(value: unknown, label: string, knownIds: Set<string>): string | undefined {
  if (value === undefined) return undefined;
  if (typeof value !== "string" || !UUID.test(value)) throw new Error(`${label}.id must be a UUID.`);
  if (!knownIds.has(value)) throw new Error(`${label}.id is unknown.`);
  return value;
}

function duplicateIds(items: Array<{ id?: string }>, label: string) {
  const ids = items.flatMap((item) => item.id ? [item.id] : []);
  if (new Set(ids).size !== ids.length) throw new Error(`Import contains a duplicate ${label} ID.`);
}
