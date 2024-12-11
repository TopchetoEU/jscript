import { func, string } from "./primordials.ts";
import { Symbol } from "./symbol.ts";
import { unwrapThis, valueKey } from "./utils.ts";

export const String = (() => {
	class String {
		[valueKey]!: string;

		public at(index: number) {
			throw "Not implemented :/";
			return unwrapThis(this, "string", String, "String.prototype.at")[index];
		};
		public toString() {
			return unwrapThis(this, "string", String, "String.prototype.toString");
		}
		public valueOf() {
			return unwrapThis(this, "string", String, "String.prototype.valueOf");
		}

		// public split(val: string) {
		// 	const res: string[] = [];

		// 	while (true) {
		// 		val.indexOf();
		// 	}
		// }

		public [Symbol.iterator]() {
			var i = 0;
			var arr: string | undefined = unwrapThis(this, "string", String, "String.prototype[Symbol.iterator]");

			return {
				next () {
					if (arr == null) return { done: true, value: undefined };
					if (i > arr.length) {
						arr = undefined;
						return { done: true, value: undefined };
					}
					else {
						var val = arr[i++];
						if (i >= arr.length) arr = undefined;
						return { done: false, value: val };
					}
				},
				[Symbol.iterator]() { return this; }
			};
		}

		public constructor (value?: unknown) {
			if (func.invokeType(arguments, this) === "call") {
				if (arguments.length === 0) return "" as any;
				else if (typeof value === "symbol") return value.toString() as any;
				else return (value as any) + "" as any;
			}
			this[valueKey] = (String as any)(value);
		}

		public static fromCharCode(...args: number[]) {
			const res: string[] = [];
			res[arguments.length] = "";

			for (let i = 0; i < arguments.length; i++) {
				res[i] = string.fromCharCode(+arguments[i]);
			}

			return string.stringBuild(res);
		}
		public static fromCodePoint(...args: number[]) {
			const res: string[] = [];
			res[arguments.length] = "";

			for (var i = 0; i < arguments.length; i++) {
				res[i] = string.fromCodePoint(+arguments[i]);
			}
			return string.stringBuild(res);
		}
	}

	func.setCallable(String, true);
	func.setConstructable(String, true);

	return String as any as typeof String & ((value?: unknown) => string);
})();
export type String = InstanceType<typeof String>;
