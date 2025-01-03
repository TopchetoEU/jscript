import { func, number, object } from "./primordials.ts";
import { unwrapThis, valueKey } from "./utils.ts";

export const Number = (() => {
	class Number {
		[valueKey]!: number;

		public toString() {
			return "" + unwrapThis(this, "number", Number, "Number.prototype.toString");
		}
		public valueOf() {
			return unwrapThis(this, "number", Number, "Number.prototype.toString");
		}

		public constructor (value?: unknown) {
			if (func.invokeType(arguments, this) === "call") {
				if (arguments.length === 0) return 0 as any;
				else return +(value as any) as any;
			}
			this[valueKey] = (Number as any)(value);
		}

		public static isFinite(value: number) {
			value = unwrapThis(value, "number", Number, "Number.isFinite", "value");
			if (value === undefined || value !== value) return false;
			if (value === number.Infinity || value === -number.Infinity) return false;
			return true;
		}
		public static isInteger(value: number) {
			value = unwrapThis(value, "number", Number, "Number.isInteger", "value");
			if (value === undefined) return false;
			return number.parseInt(value) === value;
		}
		public static isNaN(value: number) {
			return number.isNaN(value);
		}
		public static isSafeInteger(value: number) {
			value = unwrapThis(value, "number", Number, "Number.isSafeInteger", "value");
			if (value === undefined || number.parseInt(value) !== value) return false;
			return value >= -9007199254740991 && value <= 9007199254740991;
		}
		public static parseFloat(value: unknown) {
			if (typeof value === "number") return value;
			else return number.parseFloat(value + "");
		}
		public static parseInt(value: unknown, radix = 10) {
			radix = +radix;
			if (number.isNaN(radix)) radix = 10;

			if (typeof value === "number") return number.parseInt(value, radix);
			else return number.parseInt(value + "", radix);
		}

		declare public static readonly EPSILON: number;
		declare public static readonly MIN_SAFE_INTEGER: number;
		declare public static readonly MAX_SAFE_INTEGER: number;
		declare public static readonly POSITIVE_INFINITY: number;
		declare public static readonly NEGATIVE_INFINITY: number;
		declare public static readonly NaN: number;
		declare public static readonly MAX_VALUE: number;
		declare public static readonly MIN_VALUE: number;
	}

	object.defineField(Number, "EPSILON", { c: false, e: false, w: false, v: 2.220446049250313e-16 });
	object.defineField(Number, "MIN_SAFE_INTEGER", { c: false, e: false, w: false, v: -9007199254740991 });
	object.defineField(Number, "MAX_SAFE_INTEGER", { c: false, e: false, w: false, v: 9007199254740991 });
	object.defineField(Number, "POSITIVE_INFINITY", { c: false, e: false, w: false, v: +number.Infinity });
	object.defineField(Number, "NEGATIVE_INFINITY", { c: false, e: false, w: false, v: -number.Infinity });
	object.defineField(Number, "NaN", { c: false, e: false, w: false, v: number.NaN });
	object.defineField(Number, "MAX_VALUE", { c: false, e: false, w: false, v: 1.7976931348623157e+308 });
	object.defineField(Number, "MIN_VALUE", { c: false, e: false, w: false, v: 5e-324 });
	func.setCallable(Number, true);
	func.setConstructable(Number, true);

	return Number as any as typeof Number & ((value?: unknown) => number);
})();
export type Number = InstanceType<typeof Number>;
