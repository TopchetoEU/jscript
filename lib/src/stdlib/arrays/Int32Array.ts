import { buffer } from "../primordials.ts";
import { abstractIgnore, TypedArray, typedArrayFuncs } from "./TypedArray.ts";

const factory = buffer.int32;
const funcs = typedArrayFuncs(4, factory);

export class Int32Array extends TypedArray {
	public subarray(this: number[], start?: number, end?: number) {
		return funcs.subarray(this, start, end);
	}
	public slice(this: any[], start?: number, end?: number) {
		return funcs.slice(this, start, end);
	}
	public map(this: any[], cb: (val: number, i: number, self: any) => number, self?: any) {
		return funcs.map(this, cb, self);
	}
	public filter(this: any[], cb: (val: number, i: number, self: any) => boolean, self?: any) {
		return funcs.filter(this, cb, self);
	}

	public constructor(obj: any, start?: number, end?: number) {
		super(abstractIgnore);
		return funcs.construct(obj, start, end) as any;
	}
}