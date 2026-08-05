type Decimal = { coefficient: bigint; scale: number };

const powersOfTen = new Map<number, bigint>([[0, 1n]]);

function powerOfTen(exponent: number): bigint {
  const cached = powersOfTen.get(exponent);
  if (cached) return cached;
  const value = 10n ** BigInt(exponent);
  powersOfTen.set(exponent, value);
  return value;
}

function parseDecimal(value: string): Decimal {
  const match = value.match(/^(-?)(\d+)(?:\.(\d+))?$/);
  if (!match) throw new Error(`Invalid decimal: ${value}`);
  const fraction = match[3] ?? "";
  const coefficient = BigInt(`${match[1]}${match[2]}${fraction}`);
  return { coefficient, scale: fraction.length };
}

function roundHalfEven(numerator: bigint, denominator: bigint): bigint {
  const quotient = numerator / denominator;
  const remainder = numerator % denominator;
  const comparison = remainder * 2n - denominator;
  return comparison > 0n || comparison === 0n && quotient % 2n !== 0n ? quotient + 1n : quotient;
}

function rescale(decimal: Decimal, targetScale: number): bigint {
  if (decimal.scale <= targetScale) return decimal.coefficient * powerOfTen(targetScale - decimal.scale);
  const divisor = powerOfTen(decimal.scale - targetScale);
  const sign = decimal.coefficient < 0n ? -1n : 1n;
  return sign * roundHalfEven(decimal.coefficient * sign, divisor);
}

function currencyScale(currency: string): number {
  if (currency === "TWD") return 0;
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).resolvedOptions().maximumFractionDigits ?? 2;
}

export function divideMoney(amount: string, rate: string, currency: string): string {
  const dividend = parseDecimal(amount);
  const divisor = parseDecimal(rate);
  if (divisor.coefficient === 0n) throw new Error("Rate must not be zero");
  const scale = currencyScale(currency);
  const sign = (dividend.coefficient < 0n) !== (divisor.coefficient < 0n) ? -1n : 1n;
  const numerator = (dividend.coefficient < 0n ? -dividend.coefficient : dividend.coefficient) * powerOfTen(divisor.scale + scale);
  const denominator = (divisor.coefficient < 0n ? -divisor.coefficient : divisor.coefficient) * powerOfTen(dividend.scale);
  return scaledDecimal(sign * roundHalfEven(numerator, denominator), scale);
}

export function multiplyMoney(amount: string, rate: string, currency: string): string {
  const left = parseDecimal(amount);
  const right = parseDecimal(rate);
  const product = { coefficient: left.coefficient * right.coefficient, scale: left.scale + right.scale };
  const scale = currencyScale(currency);
  return scaledDecimal(rescale(product, scale), scale);
}

export function formatMoney(amount: string, currency: string): string {
  const scale = currencyScale(currency);
  const coefficient = rescale(parseDecimal(amount), scale);
  const negative = coefficient < 0n;
  const absolute = negative ? -coefficient : coefficient;
  const divisor = powerOfTen(scale);
  const whole = absolute / divisor;
  const fraction = scale ? `.${(absolute % divisor).toString().padStart(scale, "0")}` : "";
  const formattedWhole = new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(whole);
  return `${negative ? "-" : ""}${formattedWhole}${fraction}`;
}

function scaledDecimal(coefficient: bigint, scale: number): string {
  const negative = coefficient < 0n;
  const absolute = negative ? -coefficient : coefficient;
  if (!scale) return `${negative ? "-" : ""}${absolute}`;
  const digits = absolute.toString().padStart(scale + 1, "0");
  return `${negative ? "-" : ""}${digits.slice(0, -scale)}.${digits.slice(-scale)}`;
}
