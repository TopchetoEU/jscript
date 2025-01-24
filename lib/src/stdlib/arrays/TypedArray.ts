import { buffer, func, type InternalBuffer, object, string, symbol } from "../primordials.ts";
import { symbols, wrapI } from "../utils.ts";
import { Error, TypeError } from "../values/errors.ts";
import { ArrayBuffer, unwrapAB, wrapAB } from "./ArrayBuffer.ts";

export const token = symbol.getSymbol("TypedArray.abstractIgnore");

export function typedArrayFuncs(perEl: number, constructor: (buff: InternalBuffer, start: number, end: number) => number[]) {
	return {
		map(self: number[], cb: (val: number, i: number, self: number[]) => number, fnSelf: any) {
			const res = constructor(buffer.buff(self.length * perEl), 0, self.length);

			for (let i = 0; i < self.length; i++) {
				res[i] = func.invoke(cb, fnSelf, [self[i], i, self]);
			}

			return res;
		},
		filter(self: number[], cb: (val: number, i: number, self: number[]) => boolean, fnSelf: any) {
			const bigger = constructor(buffer.buff(self.length * perEl), 0, self.length);
			let j = 0;
	
			for (let i = 0; i < self.length; i++) {
				if (func.invoke(cb, self, [self[i], i, fnSelf])) bigger[j++] = self[i];
			}
	
			const res = constructor(buffer.buff(j * perEl), 0, j);
			for (let i = 0; i < j; i++) res[i] = bigger[i];
			return res;
		},
		slice(self: number[], start = 0, end = self.length) {
			start = wrapI(start, self.length);
			end = wrapI(end, self.length);
	
			if (end <= start) return constructor(buffer.buff(0), 0, 0);
	
			const res = constructor(buffer.buff((end - start) * perEl), 0, end - start);
	
			for (let i = 0; i < end - start; i++) {
				res[i] = self[start + i];
			}
	
			return res;
		},
		subarray(self: number[], start = 0, end = self.length) {
			start = wrapI(start, self.length);
			end = wrapI(end, self.length);
	
			if (end <= start) return constructor(buffer.buff(0), 0, 0);
	
			const offset = buffer.start(self);
			return constructor(buffer.backer(self), offset + start, offset + end);
		},
		construct(self: ArrayBuffer | number[] | number | Iterable<number>, start?: number, end?: number) {
			if (typeof self === "number") {
				return constructor(buffer.buff(self * perEl), 0, self);
			}
			if (self instanceof ArrayBuffer) {
				const internal = unwrapAB(self);
				if (start === undefined) start = 0;
				if (end === undefined) end = (internal.length / perEl) | 0;
				return constructor(internal, start, end);
			}
			if (symbols.iterator in self && typeof self[symbols.iterator] === "function") {
				const arr: number[] = [];
				let i = 0;
				const gen: Iterator<number> = self[symbols.iterator]();
	
				for (let it = gen.next(); !it.done; it = gen.next()) {
					arr[i++] = Number(it.value);
				}
	
				const res = constructor(buffer.buff(i * perEl), 0, i);
				for (let j = 0; j < i; j++) res[j] = arr[j];
				return res;
			}
	
			const res = constructor(buffer.buff((self as number[]).length * perEl), 0, (self as number[]).length);
			for (let i = 0; i < (self as number[]).length; i++) res[i] = (self as number[])[i];
			return res;
		},
		byteOffset(self: number[]) {
			return buffer.start(self) * perEl;
		},
		byteLength(self: number[]) {
			return (buffer.end(self) - buffer.start(self)) * perEl;
		},
	};
}

export class TypedArray {
	public get buffer() {
		return wrapAB(buffer.backer(this as any));
	}
	public get byteOffset(): number {
		throw new Error("abstract");
	}
	public get byteLength(): number {
		throw new Error("abstract");
	}

	public forEach(this: number[], cb: (val: number, i: number, self: this) => void, self?: any) {
		for (let i = 0; i < this.length; i++) {
			if (i in this) func.invoke(cb, self, [this[i], i, this]);
		}
	}
	public join(this: number[], delim = ",") {
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

	public subarray(this: number[], start = 0, end = this.length) {
		throw new Error("'slice' is an abstract method");
	}

	public slice(this: number[], start = 0, end = this.length) {
		throw new Error("'slice' is an abstract method");
	}

	public map(this: number[], cb: (val: number, i: number, self: this) => number, self?: any) {
		throw new Error("'map' is an abstract method");
	}
	public filter(this: number[], cb: (val: number, i: number, self: this) => boolean, self?: any) {
		throw new Error("'filter' is an abstract method");
	}
	public reduce(this: number[], cb: (a: number, b: number, i: number, self: number[]) => number, initial?: number) {
		let i = 0;
		if (arguments.length <= 1) initial = this[i++];

		for (; i < this.length; i++) {
			initial = cb(initial!, this[i], i, this);
		}

		return initial;
	}
	public some(this: number[], cb: (val: number, i: number, self: number[]) => boolean, self?: any) {
		for (let i = 0; i < this.length; i++) {
			if (func.invoke(cb, self, [this[i], i, this])) return true;
		}

		return false;
	}
	public every(this: number[], cb: (val: number, i: number, self: number[]) => boolean, self?: any) {
		for (let i = 0; i < this.length; i++) {
			if (!func.invoke(cb, self, [this[i], i, this])) return false;
		}

		return true;
	}
	public find(this: number[], cb: (val: number, i: number, self: number[]) => boolean, self?: any) {
		for (let i = 0; i < this.length; i++) {
			if (func.invoke(cb, self, [this[i], i, this])) return this[i];
		}

		return undefined;
	}
	public indexOf(this: number[], val: number, start = 0) {
		start |= 0;
		if (start < 0) start = 0;
		for (let i = start; i < this.length; i++) {
			if (this[i] === val) return i;
		}

		return -1;
	}
	public lastIndexOf(this: number[], val: number, start = 0) {
		start |= 0;
		if (start < 0) start = 0;

		for (let i = this.length - 1; i >= start; i--) {
			if (this[i] === val) return i;
		}

		return -1;
	}
	public includes(this: number[], val: number) {
		for (let i = 0; i < this.length; i++) {
			if (this[i] === val) return i;
		}

		return false;
	}

	public sort(this: number[], cb?:  (a: number, b: number) => number) {
		cb ||= (a, b) => a - b;

		return object.sort(this, cb);
	}
	public reverse(this: number[]) {
		const mid = this.length >> 1;
		const end = this.length - 1;

		for (let i = 0; i < mid; i++) {
			const tmp = this[i];
			this[i] = this[end - i];
			this[end - i] = tmp;
		}

		return this;
	}

	public constructor(_token?: typeof token) {
		if (_token !== token) {
			throw new TypeError("TypedArray constructor can't be called");
		}
	}
}