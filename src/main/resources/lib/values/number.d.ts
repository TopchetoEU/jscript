declare interface Number {
	valueOf(): number;
	toString(): string;
}
declare interface NumberConstructor {
	(val?: unknown): number;
	new (val?: unknown): Number;

	isFinite(value: number): boolean;
	isInteger(value: number): boolean;
	isNaN(value: number): boolean;
	isSafeInteger(value: number): boolean;
	parseFloat(value: unknown): number;
	parseInt(value: unknown, radix?: number): number;

	readonly EPSILON: number;
	readonly MIN_SAFE_INTEGER: number;
	readonly MAX_SAFE_INTEGER: number;
	readonly POSITIVE_INFINITY: number;
	readonly NEGATIVE_INFINITY: number;
	readonly NaN: number;
	readonly MAX_VALUE: number;
	readonly MIN_VALUE: number;
}
