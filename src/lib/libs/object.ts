import { Boolean } from "./boolean.ts";
import { TypeError } from "./errors.ts";
import { Number } from "./number.ts";
import { func, object } from "./primordials.ts";
import { String } from "./string.ts";
import { symbols, valueKey } from "./utils.ts";
import { Symbol } from "./symbol.ts";

export const Object = (() => {
	class Object {
		public toString(this: unknown) {
			if (this === undefined) return "[object Undefined]";
			else if (this === null) return "[object Null]";
			else if (typeof this === "object") {
				if (symbols.toStringTag in this) return "[object " + this[symbols.toStringTag] + "]";
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
		public hasOwnProperty(key: string) {
			return object.getOwnMember(this, key) != null;
		}

		public constructor (value?: unknown) {
			if (typeof value === 'object' && value !== null) return value as any;
			if (typeof value === 'string') return new String(value) as any;
			if (typeof value === 'number') return new Number(value) as any;
			if (typeof value === 'boolean') return new Boolean(value) as any;
			if (typeof value === 'symbol') {
				const res: Symbol = {} as any;
				object.setPrototype(res, Symbol.prototype);
				res[valueKey] = value;
				return res as any;
			}
		
			return {} as any;
		}

		public static getOwnPropertyDescriptor(obj: object, key: any) {
			return object.getOwnMember(obj, key);
		}

		public static defineProperty(obj: object, key: string | symbol, desc: PropertyDescriptor) {
			if (obj === null || typeof obj !== "function" && typeof obj !== "object") {
				throw new TypeError("Object.defineProperty called on non-object");
			}
			if (desc === null || typeof desc !== "function" && typeof desc !== "object") {
				throw new TypeError("Property description must be an object: " + desc);
			}
			const res: any = {};

			if ("get" in desc || "set" in desc) {
				if ("get" in desc) {
					const get = desc.get;
					if (get !== undefined && typeof get !== "function") throw new TypeError("Getter must be a function: " + get);
					res.g = get;
				}
				if ("set" in desc) {
					const set = desc.set;
					if (set !== undefined && typeof set !== "function") throw new TypeError("Setter must be a function: " + set);
					res.s = set;
				}
				if ("enumerable" in desc) res.e = !!desc.enumerable;
				if ("configurable" in desc) res.e = !!desc.configurable;

				if (!object.defineProperty(obj, key, res)) throw new TypeError("Cannot redefine property: " + String(key));
			}
			else {
				if ("enumerable" in desc) res.e = !!desc.enumerable;
				if ("configurable" in desc) res.e = !!desc.configurable;
				if ("writable" in desc) res.w = !!desc.writable;
				if ("value" in desc) res.v = desc.value;

				if (!object.defineField(obj, key, res)) throw new TypeError("Cannot redefine property: " + String(key));
			}

			return obj;
		}
		public static defineProperties(obj: object, desc: PropertyDescriptorMap) {
			const keys = object.getOwnMembers(desc, false);
			const symbols = object.getOwnSymbolMembers(desc, false);

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
		public static assign(target: any) {
			for (let i = 1; i < arguments.length; i++) {
				const obj = arguments[i];
				const keys = object.getOwnMembers(obj, false);
				const symbols = object.getOwnSymbolMembers(obj, false);
	
				for (let j = 0; j < keys.length; j++) {
					target[keys[j]] = obj[keys[j]];
				}
				for (let j = 0; j < symbols.length; j++) {
					target[symbols[j]] = obj[symbols[j]];
				}
			}

			return target;
		}


		public static setPrototypeOf(obj: object, proto: object | null) {
			object.setPrototype(obj, proto!);
		}
		public static getPrototypeOf(obj: object) {
			return object.getPrototype(obj) || null;
		}

		public static keys(obj: any) {
			const res: any[] = [];
			const keys = object.getOwnMembers(obj, false);
			const symbols = object.getOwnSymbolMembers(obj, false);

			for (let i = 0; i < keys.length; i++) {
				res[res.length] = keys[i];
			}
			for (let i = 0; i < symbols.length; i++) {
				res[res.length] = symbols[i];
			}

			return res;
		}
		public static values(obj: any) {
			const res: any[] = [];
			const keys = object.getOwnMembers(obj, false);
			const symbols = object.getOwnSymbolMembers(obj, false);

			for (let i = 0; i < keys.length; i++) {
				res[res.length] = obj[keys[i]];
			}
			for (let i = 0; i < symbols.length; i++) {
				res[res.length] = obj[symbols[i]];
			}

			return res;
		}
		public static entries(obj: any) {
			const res: [any, any][] = [];
			const keys = object.getOwnMembers(obj, false);
			const symbols = object.getOwnSymbolMembers(obj, false);

			for (let i = 0; i < keys.length; i++) {
				res[res.length] = [keys[i], obj[keys[i]]];
			}
			for (let i = 0; i < symbols.length; i++) {
				res[res.length] = [symbols[i], obj[symbols[i]]];
			}

			return res;
		}

		public static preventExtensions(obj: object) {
			object.preventExt(obj);
		}
		public static seal(obj: object) {
			object.seal(obj);
		}
		public static freeze(obj: object) {
			object.freeze(obj);
		}
	}

	object.setPrototype(Object.prototype, undefined);

	func.setCallable(Object, true);
	func.setConstructable(Object, true);

	return Object as any as typeof Object & ((value?: unknown) => object);
})();
export type Object = InstanceType<typeof Object>;
