import { Boolean } from "./boolean.ts";
import { TypeError } from "./errors.ts";
import { Number } from "./number.ts";
import { func, object } from "./primordials.ts";
import { String } from "./string.ts";
import { Symbol } from "./symbol.ts";
import { valueKey } from "./utils.ts";

export const Object = (() => {
	class Object {
		public toString(this: unknown) {
			if (this === undefined) return "[object Undefined]";
			else if (this === null) return "[object Null]";
			else if (typeof this === "object") {
				if (Symbol.toStringTag in this) return "[object " + this[Symbol.toStringTag] + "]";
				else return "[object Object]";
			}
			else if (typeof this === "number" || this instanceof Object) return "[object Object]";
			else if (typeof this === "symbol" || this instanceof Symbol) return "[object Symbol]";
			else if (typeof this === "string" || this instanceof String) return "[object String]";
			else if (typeof this === "boolean" || this instanceof Boolean) return "[object Boolean]";
			else if (typeof this === "function") return "[object Function]";
		}
		public valueOf() {
			return this;
		}

		public constructor (value?: unknown) {
			if (typeof value === 'object' && value !== null) return value as any;
			if (typeof value === 'string') return new String(value) as any;
			if (typeof value === 'number') return new Number(value) as any;
			if (typeof value === 'boolean') return new Boolean(value) as any;
			if (typeof value === 'symbol') {
				var res: Symbol = {} as any;
				object.setPrototype(res, Symbol.prototype);
				res[valueKey] = value;
				return res as any;
			}
		
			return {} as any;
		}

		public static defineProperty(obj: object, key: string | symbol, desc: PropertyDescriptor) {
			if (obj === null || typeof obj !== "function" && typeof obj !== "object") throw new TypeError("Object.defineProperty called on non-object");
			if (desc === null || typeof desc !== "function" && typeof desc !== "object") throw new TypeError("Property description must be an object: " + desc);
			if ("get" in desc || "set" in desc) {
				var get = desc.get, set = desc.set;

				if (get !== undefined && typeof get !== "function") throw new TypeError("Getter must be a function: " + get);
				if (set !== undefined && typeof set !== "function") throw new TypeError("Setter must be a function: " + set);
				if ("value" in desc || "writable" in desc) {
					throw new TypeError("Invalid property descriptor. Cannot both specify accessors and a value or writable attribute");
				}
				if (!object.defineProperty(obj, key, !!desc.enumerable, !!desc.configurable, get, set)) {
					throw new TypeError("Cannot redefine property: " + String(key));
				}
			}
			else if (!object.defineField(obj, key, !!desc.writable, !!desc.enumerable, !!desc.configurable, desc.value)) {
				throw new TypeError("Cannot redefine property: " + String(key));
			}

			return obj;
		}
		public static defineProperties(obj: object, desc: PropertyDescriptorMap) {
			const keys = object.getOwnMembers(obj, false);
			const symbols = object.getOwnSymbolMembers(obj, false);

			for (let i = 0; i < keys.length; i++) {
				Object.defineProperty(obj, keys[i], desc[keys[i]]);
			}
			for (let i = 0; i < symbols.length; i++) {
				Object.defineProperty(obj, symbols[i], desc[symbols[i]]);
			}

			return obj;
		}
		public static create(proto: object, desc?: PropertyDescriptorMap) {
			let res = object.setPrototype({}, proto);
			if (desc != null) this.defineProperties(res, desc);

			return res;
		}
	}

	func.setCallable(Object, true);
	func.setConstructable(Object, true);

	return Object as any as typeof Object & ((value?: unknown) => object);
})();
export type Object = InstanceType<typeof Object>;
