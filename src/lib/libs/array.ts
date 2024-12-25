import { func, object, string } from "./primordials.ts";
import { String } from "./string.ts";
import { limitI, symbols, wrapI } from "./utils.ts";

export const Array = (() => {
	class Array {
		public forEach(this: any[], cb: (val: any, i: number, self: this[]) => void, self?: any) {
			for (let i = 0; i < this.length; i++) {
				if (i in this) func.invoke(cb, self, [this[i], i, this]);
			}
		}
		public join(this: any[], delim = ",") {
			delim = String(delim);
			const parts = [];
			if (delim) {
				for (let i = 0; i < this.length; i++) {
					if (i) parts[parts.length] = delim;
					parts[parts.length] = (i in this) ? String(this[i]) : "";
				}
			}
			else {
				for (let i = 0; i < this.length; i++) {
					parts[i] = (i in this) ? String(this[i]) : "";
				}
			}

			return string.stringBuild(parts);
		}

		public push(this: any[]) {
			const start = this.length;
			for (let i = arguments.length - 1; i >= 0; i--) {
				this[start + i] = arguments[i];
			}
			return arguments.length;
		}
		public pop(this: any[]) {
			if (this.length === 0) return undefined;
			else {
				const res = this[this.length - 1];
				this.length--;
				return res;
			}
		}

		public unshift(this: any[]) {
			for (let i = this.length + arguments.length - 1; i >= arguments.length; i--) {
				this[i] = this[i - arguments.length];
			}
			for (let i = 0; i < arguments.length; i++) {
				this[i] = arguments[i];
			}
			return arguments.length;
		}
		public shift(this: any[]) {
			if (this.length === 0) return undefined;

			const tmp = this[0];

			for (let i = 1; i < this.length; i++) {
				this[i - 1] = this[i];
			}

			this.length--;

			return tmp;
		}

		public concat(this: any[]) {
			const res: any[] = [];

			function add(arr: any) {
				if (Array.isArray(arr) || symbols.isConcatSpreadable in arr) {
					const start = res.length;
					res.length += arr.length;

					for (let i = 0; i < res.length; i++) {
						if (i in arr) res[start + i] = arr[i];
					}
				}
				else res[res.length] = arr;
			}

			add(this);

			for (let i = 0; i < arguments.length; i++) {
				add(arguments[i]);
			}

			return res;
		}
		public slice(this: any[], start = 0, end = this.length) {
			start = wrapI(start, this.length);
			end = wrapI(end, this.length);

			if (end <= start) return [];

			const res: any[] = [];
			res.length = end - start;

			for (let i = start; i < end; i++) {
				res[i] = this[start + i];
			}

			return res;
		}
		public splice(this: any[], start = 0, count = this.length - start) {
			const vals: any[] = []
			for (let i = 0; i < arguments.length - 2; i++) vals[i] = arguments[i + 2];

			start = limitI(wrapI(start, this.length), this.length);
			count = limitI(wrapI(count, this.length), this.length - start);

			const res: any[] = [];
			const change = vals.length - count;

			for (let i = start; i < start + count; i++) {
				res[i - start] = this[i]; 
			}

			if (change < 0) {
				for (let i = start - change; i < this.length; i++) {
					this[i + change] = this[i];
				}
				this.length = this.length + change;
			}
			else {
				for (let i = this.length - 1; i >= start - change; i--) {
					this[i + change] = this[i];
				}
			}

			for (let i = 0; i < vals.length; i++) {
				this[i + start] = vals[i];
			}

			return res;
		}

		public map(this: any[], cb: Function, self?: any) {
			const res = [];
			res.length = this.length;

			for (let i = 0; i < this.length; i++) {
				if (i in this) res[i] = func.invoke(cb, self, [this[i], i, this]);
			}

			return res;
		}
		public filter(this: any[], cb: Function, self?: any) {
			const res = [];

			for (let i = 0; i < this.length; i++) {
				if (i in this && func.invoke(cb, self, [this[i], i, this])) res[res.length] = this[i];
			}

			return res;
		}
		public some(this: any[], cb: Function, self?: any) {
			for (let i = 0; i < this.length; i++) {
				if (i in this && func.invoke(cb, self, [this[i], i, this])) return true;
			}

			return false;
		}
		public find(this: any[], cb: Function, self?: any) {
			for (let i = 0; i < this.length; i++) {
				if (i in this && func.invoke(cb, self, [this[i], i, this])) return this[i];
			}

			return undefined;
		}

		public sort(this: any[], cb?: Function) {
			cb ||= (a: any, b: any) => {
				if (String(a) < String(b)) return -1;
				if (String(a) === String(b)) return 0;
				return 1;
			};

			return object.sort(this, cb);
		}
	
		public [symbols.iterator](this: any[]) {
			let i = 0;
			let arr: any[] | undefined = func.invoke(Array.prototype.slice, this, []);

			return {
				next() {
					if (arr == null) return { done: true, value: undefined };

					if (i >= arr.length) {
						arr = undefined;
						return { done: true, value: undefined };
					}

					while (true) {
						const res = arr![i];

						if (i in arr!) {
							i++;
							return { done: false, value: res };
						}
						else i++;
					}
				},
				[symbols.iterator]() { return this; }
			};
		}

		public constructor (len: unknown) {
			if (arguments.length === 1 && typeof len === "number") {
				const res: any[] = [];
				res.length = len;
				return res as any;
			}
			else {
				const res: any[] = [];
				res.length = arguments.length;
				for (let i = 0; i < arguments.length; i++) {
					res[i] = arguments[i];
				}
				return res as any;
			}
		}

		public static isArray(val: any): val is any[] {
			return object.isArray(val);
		}
		public static from(val: any, cb?: Function, self?: any): any[] {
			if (symbols.iterator in val) {
				const res = [];
				const it = val[symbols.iterator]();

				if (cb) {
					for (let val = it.next(); !val.done; val = it.next()) {
						res[res.length] = func.invoke(cb, self, [val.value]);
					}
				}
				else {
					for (let val = it.next(); !val.done; val = it.next()) {
						res[res.length] = val.value;
					}
				}

				return res;
			}
			else if ("length" in val) {
				const res = [];

				if (cb) {
					for (let i = 0; i < val.length; i++) {
						if (i in val) res[i] = func.invoke(cb, self, [val[i]]);
					}
				}
				else {
					for (let i = 0; i < val.length; i++) {
						if (i in val) res[i] = val[i];
					}
				}

				return res;
			}
			else if (val == null) throw new Error("Illegal argument");
			else return [];
		}
	}

	func.setCallable(Array, true);
	func.setConstructable(Array, true);

	return Array as any as typeof Array & ((value?: unknown) => object);
})();
export type Array = InstanceType<typeof Array>;
